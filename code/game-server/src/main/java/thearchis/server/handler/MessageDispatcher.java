package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.game.ClientState;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.controller.AuthenticationController;
import thearchis.server.controller.CollectionController;
import thearchis.server.controller.GameController;
import thearchis.server.controller.BackNavigationController;
import thearchis.server.controller.ShopController;

public class MessageDispatcher {

	private static final Logger logger = LogManager.getLogger(MessageDispatcher.class);
	private static MessageDispatcher instance;

	private final AuthenticationController authenticationController;
	private final ShopController shopController;
	private final CollectionController collectionController;
	private final GameController gameController;
	private final BackNavigationController backNavigationController;

	private MessageDispatcher() {
		this.authenticationController = new AuthenticationController();
		this.shopController = new ShopController();
		this.collectionController = new CollectionController();
		this.gameController = new GameController();
		this.backNavigationController = new BackNavigationController();
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
		case COLLECTION_REQUEST -> collectionController.handleCollectionRequest(clientHandler, msg);
		case CRAFT_REQUEST -> collectionController.handleCraftRequest(clientHandler, msg);
		case DISENCHANT_REQUEST -> collectionController.handleDisenchantRequest(clientHandler, msg);
		case BACKNAVIGATION_REQUEST -> backNavigationController.handleBackNavigationRequest(clientHandler, msg);

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
		case MAIN_MENU -> type == MessageType.COLLECTION_REQUEST || type == MessageType.BACKNAVIGATION_REQUEST;
		case IN_COLLECTION -> type == MessageType.CRAFT_REQUEST || type == MessageType.DISENCHANT_REQUEST
				|| type == MessageType.BACKNAVIGATION_REQUEST;
		case IN_SHOP -> type == MessageType.BACKNAVIGATION_REQUEST;

		// TODO aggiungere BACKNAVIGATION_REQUEST ai menu che lo consentono, aggiungere
		// IN_DECK_BUILDER, IN_QUEUE, IN_GAME, IN_SHOP

		default -> false;
		};
	}

	private void sendBadRequestResponse(ClientHandler clientHandler, MessageType type) {
		MessageType responseType = switch (type) {
		case REGISTER_REQUEST -> MessageType.REGISTER_RESPONSE;
		case LOGIN_REQUEST -> MessageType.LOGIN_RESPONSE;
		case COLLECTION_REQUEST -> MessageType.COLLECTION_RESPONSE;
		case CRAFT_REQUEST -> MessageType.CRAFT_RESPONSE;
		case DISENCHANT_REQUEST -> MessageType.DISENCHANT_RESPONSE;

		case BACKNAVIGATION_REQUEST -> MessageType.BACKNAVIGATION_RESPONSE;

		default -> MessageType.ERROR_RESPONSE;
		};

		OperationResult<?> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
		clientHandler.sendMessage(new Message(responseType, result));
	}

}