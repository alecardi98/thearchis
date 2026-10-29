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
			while (running && (obj = in.readObject()) != null) {
				Message msg = (Message) obj;
				switch (msg.getType()) {
				case REGISTER_RESPONSE -> {
					DBStatus status = (DBStatus) msg.getData();
					handleRegisterResponse(status);
				}
				default -> logger.error("Tipo di messaggio sconosciuto: {}", msg.getType());
				}

			}
		} catch (IOException | ClassNotFoundException e) {
			if (!running) { // disconnessione avviata volontariamente dal Client
				logger.info("ServerListener arrestato correttamente.");
			} else {
				logger.error("Comunicazione con il Server persa in modo inatteso: {}", e.getMessage());
			}
		} finally {
			if (running) { // disconnessione causata dal server
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