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
		AuthenticationData userData = (AuthenticationData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di registrazione (nickname '{}')", clientId, nickname);

		DBResult<Void> result = Server.getInstance().getDbManager().registerUser(nickname, password);

		clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, result));

		DBStatus status = result.getStatus();
		logger.info("Client #{} : esito registrazione inviato (status '{}')", clientId, status);
	}

	public void handleLogin(ClientHandler clientHandler, Message msg) {
		AuthenticationData userData = (AuthenticationData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di login (nickname '{}')", clientId, nickname);

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
