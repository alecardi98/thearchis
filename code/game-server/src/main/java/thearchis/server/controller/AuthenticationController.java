package thearchis.server.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.game.ClientState;
import thearchis.common.game.Player;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.common.message.AuthenticationData;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

public class AuthenticationController {

	private static final Logger logger = LogManager.getLogger(AuthenticationController.class);

	public AuthenticationController() {
	}

	public void handleRegister(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di registrazione.", clientId);

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta registrazione rifiutata (messaggio o payload nullo).", clientId);
			OperationResult<Void> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof AuthenticationData)) {
			logger.warn("Client #{}: richiesta registrazione rifiutata (payload non di tipo AuthenticationData).",
					clientId);
			OperationResult<Void> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));
			return;
		}

		AuthenticationData userData = (AuthenticationData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		if (nickname == null || nickname.isBlank() || password == null || password.isBlank()) {
			logger.warn("Client #{}: richiesta registrazione rifiutata (credenziali nulle o vuote).", clientId);
			OperationResult<Void> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));
			return;
		}

		logger.info("Client #{} : ricevuta richiesta di registrazione (nickname '{}')", clientId, nickname);

		OperationResult<Void> result = Server.getInstance().getDbManager().registerUser(nickname, password);
		clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));

		OperationStatus status = result.getStatus();
		logger.info("Client #{} : esito registrazione inviato (status '{}')", clientId, status);
	}

	public void handleLogin(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di login.", clientId);

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta login rifiutata (messaggio o payload nullo).", clientId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof AuthenticationData)) {
			logger.warn("Client #{}: richiesta login rifiutata (payload non di tipo AuthenticationData).", clientId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		AuthenticationData userData = (AuthenticationData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		if (nickname == null || nickname.isBlank() || password == null || password.isBlank()) {
			logger.warn("Client #{}: richiesta login rifiutata (credenziali nulle o vuote).", clientId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		Server server = Server.getInstance();
		if (server.isUserAuthenticated(nickname)) {
			logger.info("Client #{} : nickname '{}' già autenticato altrove.", clientId, nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.ALREADY_LOGGED_IN, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		OperationResult<Player> result = server.getDbManager().loginUser(nickname, password);
		OperationStatus status = result.getStatus();

		if (status == OperationStatus.LOGIN_OK) {
			Player player = result.getData();
			clientHandler.setPlayer(player);
			clientHandler.setClientState(ClientState.MAIN_MENU);
			server.registerLoggedUser(nickname, clientId);
		}

		clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
		logger.info("Client #{} : esito login inviato (status '{}')", clientId, status);
	}
}
