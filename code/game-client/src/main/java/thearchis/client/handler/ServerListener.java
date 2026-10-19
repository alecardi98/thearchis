package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBStatus;
import thearchis.common.message.Message;

public class ServerListener implements Runnable {

	private static final Logger logger = LogManager.getLogger(ServerListener.class);
	private ObjectInputStream in;
	private Client client;

	public ServerListener(ObjectInputStream in, Client client) {
		this.in = in;
		this.client = client;
	}

	@Override
	public void run() {
		try {
			Object obj;
			while ((obj = in.readObject()) != null) {
				Message msg = (Message) obj;

				switch (msg.getType()) {
				case REGISTER_RESPONSE -> handleRegisterResponse((DBStatus) msg.getData());
				default -> logger.error("ERRORE! Messaggio sconosciuto.");

				}
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.error("ERRORE! Comunicazione con il Server persa.");
			cleanUp();
		}
	}

	private void handleRegisterResponse(DBStatus status) {
		logger.info("Esito registrazione: {}", status);

		switch (status) {
		case REGISTER_OK -> {
			System.out.println("[GUI] Registrazione riuscita!");
		}
		case NICKNAME_ALREADY_EXISTS -> {
			System.out.println("[GUI] Nickname già esistente!");
		}
		case DB_ERROR -> {
			System.out.println("[GUI] Problema temporaneo al server.");
		}
		default -> System.out.println("[GUI] Risposta sconosciuta.");
		}
	}

	/*
	 * Metodo che chiude correttamente il ServerListener
	 */
	private void cleanUp() {
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		client.serverDisconnection();
	}

}
