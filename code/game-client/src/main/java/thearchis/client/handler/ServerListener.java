package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.collection.Collection;
import thearchis.common.db.DBResult;
import thearchis.common.db.DBStatus;
import thearchis.common.game.Player;
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

	@SuppressWarnings("unchecked")
	private void processMessage(Message msg) {
		switch (msg.getType()) {
		case REGISTER_RESPONSE -> {
			DBResult<Void> result = (DBResult<Void>) msg.getData();
			DBStatus status = result.getStatus();
			handleRegisterResponse(status);
		}
		case LOGIN_RESPONSE -> {
			DBResult<Player> result = (DBResult<Player>) msg.getData();
			handleLoginResponse(result);
		}
		case COLLECTION_RESPONSE -> {
			DBResult<Collection> result = (DBResult<Collection>) msg.getData();
			handleCollectionResponse(result);
		}
		case CRAFT_RESPONSE -> {
			DBResult<Player> result = (DBResult<Player>) msg.getData();
			handleCraftResponse(result);

		}
		case DISENCHANT_RESPONSE -> {
			DBResult<Player> result = (DBResult<Player>) msg.getData();
			handleDisenchantResponse(result);

		}
		default -> logger.error("Tipo di messaggio non gestito: {}", msg.getType());
		}
	}

	private void handleRegisterResponse(DBStatus status) {
		logger.info("Esito registrazione: {}", status);

		switch (status) {
		case REGISTER_OK -> System.out.println("[GUI] Registrazione: registrazione riuscita.");
		case NICKNAME_ALREADY_EXISTS -> System.out.println("[GUI] Registrazione: nickname già esistente.");
		case BAD_REQUEST -> System.out.println("[GUI] Registrazione: azione non consentita nello stato attuale.");
		case DB_ERROR -> System.out.println("[GUI] Registrazione: errore del server/database.");
		default -> System.out.println("[GUI] Registrazione: risposta sconosciuta.");
		}
	}

	private void handleLoginResponse(DBResult<Player> result) {
		DBStatus status = result.getStatus();
		Player player = result.getData();
		logger.info("Esito login: {}.", status);

		switch (status) {
		case LOGIN_OK -> {
			System.out.println("[GUI] Login: login riuscito.");
			client.setPlayer(player);
		}
		case INVALID_CREDENTIALS -> System.out.println("[GUI] Login: credenziali errate.");
		case ALREADY_LOGGED_IN -> System.out.println("[GUI] Login: utente già loggato.");
		case BAD_REQUEST -> System.out.println("[GUI] Login: azione non consentita nello stato attuale.");
		case DB_ERROR -> System.out.println("[GUI] Login: errore del server/database.");
		default -> System.out.println("[GUI] Login: risposta sconosciuta.");

		}
	}

	private void handleCollectionResponse(DBResult<Collection> result) {
		DBStatus status = result.getStatus();
		Collection collection = result.getData();
		logger.info("Esito collezione: {}.", status);

		switch (status) {
		case COLLECTION_OK -> {
			System.out.println("[GUI] Collezione: collezione caricata con successo.");
			if (client.getPlayer() != null) {
				client.getPlayer().setCollection(collection);
			}
		}
		case BAD_REQUEST -> System.out.println("[GUI] Collezione: azione non consentita nello stato attuale.");
		case DB_ERROR -> System.out.println("[GUI] Collezione: errore del server/database.");
		default -> System.out.println("[GUI] Collezione: risposta sconosciuta.");
		}
	}

	private void handleCraftResponse(DBResult<Player> result) {
		DBStatus status = result.getStatus();
		Player player = result.getData();
		logger.info("Esito craft: {}.", status);

		switch (status) {
		case CRAFT_OK -> {
			System.out.println("[GUI] Craft: carta creata con successo.");
			client.setPlayer(player);
		}
		case NOT_ENOUGH_DUST -> System.out.println("[GUI] Craft: polvere insufficiente.");
		case BAD_REQUEST -> System.out.println("[GUI] Craft: azione non consentita o carta vincolata.");
		case DB_ERROR -> System.out.println("[GUI] Craft: errore del server/database.");
		default -> System.out.println("[GUI] Craft: risposta sconosciuta.");
		}
	}
	
	private void handleDisenchantResponse(DBResult<Player> result) {
		DBStatus status = result.getStatus();
		Player player = result.getData();
		logger.info("Esito disenchant: {}.", status);

		switch (status) {
		case DISENCHANT_OK -> {
			System.out.println("[GUI] Disenchant: carta disincantata con successo.");
			client.setPlayer(player);
		}
		case CARD_NOT_OWNED -> System.out.println("[GUI] Disenchant: carta non posseduta.");
		case BAD_REQUEST -> System.out.println("[GUI] Disenchant: azione non consentita o carta vincolata.");
		case DB_ERROR -> System.out.println("[GUI] Disenchant: errore del server/database.");
		default -> System.out.println("[GUI] Disenchant: risposta sconosciuta.");
		}
	}

}