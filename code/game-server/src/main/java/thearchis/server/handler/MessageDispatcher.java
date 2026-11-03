package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBStatus;
import thearchis.common.game.ClientState;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.server.controller.AuthenticationController;
import thearchis.server.controller.CollectionController;
import thearchis.server.controller.GameController;
import thearchis.server.controller.ShopController;

public class MessageDispatcher {

	private static final Logger logger = LogManager.getLogger(MessageDispatcher.class);
	private static MessageDispatcher instance;

	private final AuthenticationController authenticationController;
	private final ShopController shopController;
	private final CollectionController collectionController;
	private final GameController gameController;

	private MessageDispatcher() {
		this.authenticationController = new AuthenticationController();
		this.shopController = new ShopController();
		this.collectionController = new CollectionController();
		this.gameController = new GameController();
	}

	public static synchronized MessageDispatcher getInstance() {
		if (instance == null) {
			instance = new MessageDispatcher();
		}
		return instance;
	}

	public void dispatch(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		ClientState currentState = clientHandler.getClientState();
		MessageType type = msg.getType();

		if (!isAllowedInState(type, currentState)) {
			logger.warn("Client #{} : tentativo non autorizzato di inviare '{}' nello stato {}.", clientId, type,
					currentState);

			sendBadRequestResponse(clientHandler, type);
			return;
		}

		switch (type) {
		case REGISTER_REQUEST -> authenticationController.handleRegister(clientHandler, msg);
		case LOGIN_REQUEST -> authenticationController.handleLogin(clientHandler, msg);
		default -> logger.error("Client #{} : tipo di messaggio sconosciuto ({}).", clientId, type);
		}
	}

	/**
	 * Mappa di controllo per verificare se un tipo di messaggio è permesso nello
	 * stato attuale
	 */
	private boolean isAllowedInState(MessageType type, ClientState state) {
		return switch (state) {
		case NOT_AUTHENTICATED -> type == MessageType.REGISTER_REQUEST || type == MessageType.LOGIN_REQUEST;
		default -> false;
		};
	}

	private void sendBadRequestResponse(ClientHandler clientHandler, MessageType type) {
		MessageType responseType = switch (type) {
		case REGISTER_REQUEST -> MessageType.REGISTER_RESPONSE;
		case LOGIN_REQUEST -> MessageType.LOGIN_RESPONSE;
		default -> MessageType.ERROR_RESPONSE;
		};

		clientHandler.sendMessage(new Message(responseType, DBStatus.BAD_REQUEST));
	}

}