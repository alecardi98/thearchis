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

				if (obj instanceof Message msg) {
					processMessage(msg);
				} else {
					logger.warn("Oggetto ricevuto non riconosciuto: {}", obj.getClass().getName());
				}
			}
		} catch (IOException | ClassNotFoundException e) {

			if (!running) { // Disconnessione avviata volontariamente dal Client
				logger.info("ServerListener arrestato correttamente.");
			} else {
				logger.error("Comunicazione con il Server persa in modo inatteso ({}).", e.getMessage());
			}
		} finally {
			if (running) {
				client.serverDisconnection();
			}
		}
	}

	public void stopListener() {
		this.running = false;
	}

	private void processMessage(Message msg) {
		switch (msg.getType()) {
		case REGISTER_RESPONSE -> {
			DBStatus status = (DBStatus) msg.getData();
			handleRegisterResponse(status);
		}
		case LOGIN_RESPONSE -> {
			DBStatus status = (DBStatus) msg.getData();
			handleLoginResponse(status);
		}
		default -> logger.error("Tipo di messaggio non gestito: {}", msg.getType());
		}
	}

	private void handleRegisterResponse(DBStatus status) {
		logger.info("Esito registrazione: {}", status);

		switch (status) {
		case REGISTER_OK -> System.out.println("[GUI] Registrazione: registrazione riuscita.");
		case NICKNAME_ALREADY_EXISTS -> System.out.println("[GUI] Registrazione: nickname già esistente.");
		case DB_ERROR -> System.out.println("[GUI] Registrazione: errore del server/database.");
		default -> System.out.println("[GUI] Registrazione: risposta sconosciuta.");
		}
	}

	private void handleLoginResponse(DBStatus status) {
		logger.info("Esito login: {}", status);

		switch (status) {
		case LOGIN_OK -> {
			System.out.println("[GUI] Login: login riuscito.");
			client.confirmLogin();
		}
		case INVALID_CREDENTIALS -> {
			System.out.println("[GUI] Login: credenziali errate.");
			client.clearPendingLogin();
		}
		case ALREADY_LOGGED_IN -> {
			System.out.println("[GUI] Login: utente già loggato.");
			client.clearPendingLogin();
		}
		case DB_ERROR -> {
			System.out.println("[GUI] Login: errore del server/database.");
			client.clearPendingLogin();
		}
		default -> {
			System.out.println("[GUI] Login: risposta sconosciuta.");
			client.clearPendingLogin();
		}
		}
	}
}