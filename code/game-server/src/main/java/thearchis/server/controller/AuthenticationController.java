package thearchis.server.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBResult;
import thearchis.common.db.DBStatus;
import thearchis.common.game.ClientState;
import thearchis.common.game.Player;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
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
			DBResult<Void> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof AuthenticationData)) {
			logger.warn("Client #{}: richiesta registrazione rifiutata (payload non di tipo AuthenticationData).",
					clientId);
			DBResult<Void> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));
			return;
		}

		AuthenticationData userData = (AuthenticationData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		if (nickname == null || nickname.isBlank() || password == null || password.isBlank()) {
			logger.warn("Client #{}: richiesta registrazione rifiutata (credenziali nulle o vuote).", clientId);
			DBResult<Void> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));
			return;
		}

		logger.info("Client #{} : ricevuta richiesta di registrazione (nickname '{}')", clientId, nickname);

		DBResult<Void> result = Server.getInstance().getDbManager().registerUser(nickname, password);
		clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));

		DBStatus status = result.getStatus();
		logger.info("Client #{} : esito registrazione inviato (status '{}')", clientId, status);
	}

	public void handleLogin(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di login.", clientId);

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta login rifiutata (messaggio o payload nullo).", clientId);
			DBResult<Player> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof AuthenticationData)) {
			logger.warn("Client #{}: richiesta login rifiutata (payload non di tipo AuthenticationData).", clientId);
			DBResult<Player> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		AuthenticationData userData = (AuthenticationData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		if (nickname == null || nickname.isBlank() || password == null || password.isBlank()) {
			logger.warn("Client #{}: richiesta login rifiutata (credenziali nulle o vuote).", clientId);
			DBResult<Player> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		Server server = Server.getInstance();
		if (server.isUserAuthenticated(nickname)) {
			logger.info("Client #{} : nickname '{}' già autenticato altrove.", clientId, nickname);
			DBResult<Player> result = new DBResult<>(DBStatus.ALREADY_LOGGED_IN, null);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
			return;
		}

		DBResult<Player> result = server.getDbManager().loginUser(nickname, password);
		DBStatus status = result.getStatus();

		if (status == DBStatus.LOGIN_OK) {
			Player player = result.getData();
			clientHandler.setPlayer(player);
			clientHandler.setClientState(ClientState.MAIN_MENU);
			server.registerLoggedUser(nickname, clientId);
		}

		clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, result));
		logger.info("Client #{} : esito login inviato (status '{}')", clientId, status);
	}
}
