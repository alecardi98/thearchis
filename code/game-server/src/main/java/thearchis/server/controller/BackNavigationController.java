package thearchis.server.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.game.ClientState;
import thearchis.common.message.BackNavigationData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

public class BackNavigationController {

	private static final Logger logger = LogManager.getLogger(BackNavigationController.class);

	public BackNavigationController() {
	}

	public void handleBackNavigationRequest(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();

		logger.info("Client #{}: ricevuta richiesta di navigazione", clientId);

		if (clientHandler.getPlayer() == null) {
			logger.warn("Client #{}: richiesta navigazione rifiutata (player null).", clientId);
			OperationResult<ClientState> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.BACKNAVIGATION_RESPONSE, result));
			return;
		}

		String nickname = clientHandler.getPlayer().getNickname();
		Server server = Server.getInstance();
		if (!server.isUserAuthenticated(nickname)) {
			logger.warn("Client #{}: richiesta navigazione rifiutata (utente {} non autenticato).", clientId, nickname);
			OperationResult<ClientState> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.BACKNAVIGATION_RESPONSE, result));
			return;
		}

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta navigazione rifiutata (messaggio o payload nullo).", clientId);
			OperationResult<ClientState> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.BACKNAVIGATION_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof BackNavigationData)) {
			logger.warn("Client #{}: richiesta navigazione rifiutata (payload non di tipo BackNavigationData).",
					clientId);
			OperationResult<ClientState> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.BACKNAVIGATION_RESPONSE, result));
			return;
		}

		BackNavigationData backNavigationData = (BackNavigationData) msg.getData();
		ClientState destination = backNavigationData.getDestination();
		ClientState start = clientHandler.getClientState();

		if (!isValidTransition(start, destination)) {
			logger.warn("Client #{}: tentativo di navigazione indietro non valida ({} -> {}).", clientId, start,
					destination);
			OperationResult<ClientState> result = new OperationResult<>(OperationStatus.BAD_REQUEST, start);
			clientHandler.sendMessage(new Message(MessageType.BACKNAVIGATION_RESPONSE, result));
			return;
		}

		clientHandler.setClientState(destination);
		logger.info("Client #{}: transizione completata con successo ({} -> {}).", clientId, start, destination);

		OperationResult<ClientState> result = new OperationResult<>(OperationStatus.BACKNAVIGATION_OK, destination);
		clientHandler.sendMessage(new Message(MessageType.BACKNAVIGATION_RESPONSE, result));
	}

	private boolean isValidTransition(ClientState start, ClientState destination) {
		return switch (start) {
		case IN_COLLECTION, IN_DECKBUILDER, IN_SHOP, IN_QUEUE -> destination == ClientState.MAIN_MENU;
		case MAIN_MENU -> destination == ClientState.LOGOUT;
		case IN_GAME, NOT_AUTHENTICATED, LOGOUT -> false;
		default -> false;
		};
	}

}
