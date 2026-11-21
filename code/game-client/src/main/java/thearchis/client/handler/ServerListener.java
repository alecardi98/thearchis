package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.game.ClientState;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.message.Message;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

public class ServerListener implements Runnable {

	private static final Logger logger = LogManager.getLogger("[ServerListener]");
	private final ObjectInputStream in;
	private final Client client;
	private volatile boolean running = true;

	public ServerListener(ObjectInputStream in, Client client) {
		if (in == null) {
			throw new IllegalArgumentException("ServerListener : in non può essere null.");
		}
		this.in = in;

		if (client == null) {
			throw new IllegalArgumentException("ServerListener : client non può essere null.");
		}
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
					logger.warn("run : Oggetto ricevuto non riconosciuto ({}).", obj.getClass().getName());
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			if (!running) { // Disconnessione avviata volontariamente dal Client
				logger.info("run : ServerListener arrestato correttamente.");
			} else {
				logger.error("run : Comunicazione con il Server persa in modo inatteso ({}).", e.getMessage());
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
		if (msg == null) {
			logger.warn("processMessage : Message null ricevuto dal server, non processato.");
			return;
		}

		if (msg.getType() == null) {
			logger.warn("processMessage : Message con MessageType null ricevuto dal server, non processato.");
			return;
		}

		switch (msg.getType()) {
		case REGISTER_RESPONSE -> handleRegisterResponse(msg);
		case LOGIN_RESPONSE -> handleLoginResponse(msg);
		case COLLECTION_RESPONSE -> handleCollectionResponse(msg);
		case CRAFT_RESPONSE -> handleCraftResponse(msg);
		case DISENCHANT_RESPONSE -> handleDisenchantResponse(msg);

		case BACKNAVIGATION_RESPONSE -> handleBackNavigationResponse(msg);

		default -> logger.warn("processMessage : MessageType {} non gestito, non processato.", msg.getType());
		}

	}

	private void handleRegisterResponse(Message msg) {
		OperationResult<Void> result = extractAndValidateResult(msg);
		if (result == null) {
			return;
		}

		OperationStatus status = result.getStatus();
		logger.info("handleRegisterResponse : Esito REGISTER_REQUEST -> {}.", status);

		switch (status) {
		case REGISTER_OK -> System.out.println("[GUI] Registrazione : registrazione riuscita.");
		case NICKNAME_ALREADY_EXISTS -> System.out.println("[GUI] Registrazione : nickname già esistente.");
		case BAD_REQUEST -> System.out.println("[GUI] Registrazione : richiesta non valida.");
		case DB_ERROR -> System.out.println("[GUI] Registrazione : errore del server/database.");
		default -> System.out.println("[GUI] Registrazione : risposta sconosciuta.");
		}
	}

	private void handleLoginResponse(Message msg) {
		OperationResult<Player> result = extractAndValidateResult(msg);
		if (result == null) {
			return;
		}
		OperationStatus status = result.getStatus();

		logger.info("handleLoginResponse : Esito LOGIN_REQUEST -> {}.", status);

		switch (status) {
		case LOGIN_OK -> {
			if (result.getData() instanceof Player player) {
				System.out.println("[GUI] Login : login riuscito.");
				client.setPlayer(player);
			} else {
				logger.fatal(
						"[GUI] ERRORE FATALE : Login : Login dichiarato OK ma il payload ricevuto non contiene un oggetto Player valido. Disconnesione.");
				client.serverDisconnection();
				return;
			}
		}
		case INVALID_CREDENTIALS -> System.out.println("[GUI] Login : credenziali errate.");
		case ALREADY_LOGGED_IN -> System.out.println("[GUI] Login : utente già loggato.");
		case BAD_REQUEST -> System.out.println("[GUI] Login : richiesta non valida.");
		case DB_ERROR -> System.out.println("[GUI] Login : errore del server/database.");
		default -> System.out.println("[GUI] Login : risposta sconosciuta.");
		}
	}

	private void handleCollectionResponse(Message msg) {
		OperationResult<Collection> result = extractAndValidateResult(msg);
		if (result == null) {
			return;
		}
		OperationStatus status = result.getStatus();

		logger.info("handleCollectionResponse : Esito COLLECTION_REQUEST -> {}.", status);

		switch (status) {
		case COLLECTION_OK -> {
			if (!(result.getData() instanceof Collection collection)) {
				logger.fatal(
						"[GUI] ERRORE FATALE : Collezione : Collezione OK ma il payload ricevuto non contiene una Collection valida. Disconnessione.");
				client.serverDisconnection();
				return;
			}

			if (client.getPlayer() == null) {
				logger.fatal(
						"[GUI] ERRORE FATALE : Collezione : Collection valida ma l'oggetto Player locale è null. Disconnessione.");
				client.serverDisconnection();
				return;
			}

			client.getPlayer().setCollection(collection);
			System.out.println("[GUI] Collezione : collezione caricata con successo.");
		}
		case BAD_REQUEST -> System.out.println("[GUI] Collezione : richiesta non valida.");
		case DB_ERROR -> System.out.println("[GUI] Collezione : errore del server/database.");
		default -> System.out.println("[GUI] Collezione : risposta sconosciuta.");
		}
	}

	private void handleCraftResponse(Message msg) {
		OperationResult<Player> result = extractAndValidateResult(msg);
		if (result == null) {
			return;
		}
		OperationStatus status = result.getStatus();

		logger.info("handleCraftResponse : Esito CRAFT_REQUEST -> {}.", status);

		switch (status) {
		case CRAFT_OK -> {
			if (result.getData() instanceof Player player) {
				System.out.println("[GUI] Craft : carta creata con successo.");
				client.setPlayer(player);
			} else {
				logger.fatal(
						"[GUI] ERRORE FATALE : Craft : Craft OK ma il payload ricevuto non contiene un oggetto Player valido. Disconnessione.");
				client.serverDisconnection();
				return;
			}
		}
		case NOT_ENOUGH_DUST -> System.out.println("[GUI] Craft : polvere insufficiente.");
		case BAD_REQUEST -> System.out.println("[GUI] Craft : richiesta non valida.");
		case DB_ERROR -> System.out.println("[GUI] Craft : errore del server/database.");
		default -> System.out.println("[GUI] Craft : risposta sconosciuta.");
		}
	}

	private void handleDisenchantResponse(Message msg) {
		OperationResult<Player> result = extractAndValidateResult(msg);
		if (result == null) {
			return;
		}
		OperationStatus status = result.getStatus();

		logger.info("handleDisenchantResponse : Esito DISENCHANT_REQUEST -> {}.", status);

		switch (status) {
		case DISENCHANT_OK -> {
			if (result.getData() instanceof Player player) {
				System.out.println("[GUI] Disenchant : carta disincantata con successo.");
				client.setPlayer(player);
			} else {
				logger.fatal(
						"[GUI] ERRORE FATALE : Disenchant : Disenchant OK ma il payload ricevuto non contiene un oggetto Player valido. Disconnessione.");
				client.serverDisconnection();
				return;
			}
		}
		case CARD_NOT_OWNED -> System.out.println("[GUI] Disenchant : carta non posseduta.");
		case BAD_REQUEST -> System.out.println("[GUI] Disenchant : richiesta non valida.");
		case DB_ERROR -> System.out.println("[GUI] Disenchant : errore del server/database.");
		default -> System.out.println("[GUI] Disenchant : risposta sconosciuta.");
		}
	}

	private void handleBackNavigationResponse(Message msg) {
		OperationResult<ClientState> result = extractAndValidateResult(msg);
		if (result == null) {
			return;
		}
		OperationStatus status = result.getStatus();

		logger.info("handleBackNavigationResponse : Esito BACKNAVIGATION_REQUEST -> {}.", status);

		switch (status) {
		case BACKNAVIGATION_OK -> {
			if (result.getData() instanceof ClientState destination) {
				System.out.println("[GUI] BackNavigation : arrivato a " + destination + " con successo.");
			} else {
				logger.fatal(
						"[GUI] ERRORE FATALE : BackNavigation : Navigazione dichiarata OK ma il payload ricevuto non contiene un oggetto ClientState valido. Disconnesione.");
				client.serverDisconnection();
				return;
			}
		}
		case BAD_REQUEST -> System.out.println("[GUI] BackNavigation : richiesta non valida.");
		default -> System.out.println("[GUI] BackNavigation : risposta sconosciuta.");
		}
	}

	/*
	 * Helper per il controllo della correttezza dei messaggi del Server
	 */
	@SuppressWarnings("unchecked")
	private <T> OperationResult<T> extractAndValidateResult(Message msg) {
		if (msg.getData() == null) {
			logger.warn("extractAndValidateResult : Message con payload null ricevuto dal server, non processato.");
			return null;
		}

		if (!(msg.getData() instanceof OperationResult<?> result)) {
			logger.warn(
					"extractAndValidateResult : Message con payload non di tipo OperationResult<> ricevuto dal server, non processato.");
			return null;
		}

		if (result.getStatus() == null) {
			logger.warn(
					"extractAndValidateResult : OperationResult<> con status null ricevuto dal server, non processato.");
			return null;
		}

		return (OperationResult<T>) result;
	}

}