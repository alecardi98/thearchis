package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBStatus;
import thearchis.common.message.Message;

public class ServerListener implements Runnable {

	private static final Logger logger = LogManager.getLogger(ServerListener.class);
	private final ObjectInputStream in;
	private final Client client;
	private volatile boolean running = true;

	public ServerListener(ObjectInputStream in, Client client) {
		this.in = in;
		this.client = client;
	}

	@Override
	public void run() {
		try {
			Object obj;
			// Il ciclo gira finché il flag è true e lo stream restituisce oggetti validi
			while (running && (obj = in.readObject()) != null) {
				if (obj instanceof Message msg) {
					switch (msg.getType()) {
					case REGISTER_RESPONSE -> {
						if (msg.getData() instanceof DBStatus status) {
							handleRegisterResponse(status);
						} else {
							logger.warn("Payload non valido per REGISTER_RESPONSE.");
						}
					}
					default -> logger.error("ERRORE! Tipo di messaggio sconosciuto: {}", msg.getType());
					}
				} else {
					logger.warn("Ricevuto oggetto non riconosciuto dal Server: {}", obj.getClass().getName());
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			// Se running è false, la disconnessione è stata avviata volontariamente dal Client
			if (!running) {
				logger.info("ServerListener arrestato correttamente.");
			} else {
				logger.error("ERRORE! Comunicazione con il Server persa in modo inatteso: {}", e.getMessage());
			}
		} finally {
			// Se la disconnessione è stata causata dal Server (running è ancora true),
			// notifichiamo al Client di liberare le risorse
			if (running) {
				client.serverDisconnection();
			}
		}
	}

	public void stopListener() {
		this.running = false;
	}

	private void handleRegisterResponse(DBStatus status) {
		logger.info("Esito registrazione: {}", status);

		switch (status) {
		case REGISTER_OK -> System.out.println("[GUI] Registrazione riuscita!");
		case NICKNAME_ALREADY_EXISTS -> System.out.println("[GUI] Nickname già esistente!");
		case DB_ERROR -> System.out.println("[GUI] Problema temporaneo al server.");
		default -> System.out.println("[GUI] Risposta sconosciuta.");
		}
	}
}