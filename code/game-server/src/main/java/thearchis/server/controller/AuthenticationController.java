package thearchis.server.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBStatus;
import thearchis.common.game.ClientState;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.UserData;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

public class AuthenticationController {

	private static final Logger logger = LogManager.getLogger(AuthenticationController.class);

	public void handleRegister(ClientHandler clientHandler, Message msg) {
		UserData userData = (UserData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di registrazione (nickname '{}')", clientId, nickname);

		DBStatus status = Server.getInstance().getDbManager().registerUser(nickname, password);
		clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, status));

		logger.info("Client #{} : esito registrazione inviato (status '{}')", clientId, status);
	}

	public void handleLogin(ClientHandler clientHandler, Message msg) {
		UserData userData = (UserData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		int clientId = clientHandler.getClientId();
		logger.info("Client #{} : ricevuta richiesta di login (nickname '{}')", clientId, nickname);

		Server server = Server.getInstance();

		if (server.isUserAuthenticated(nickname)) {
			logger.info("Client #{} : nickname '{}' già autenticato altrove.", clientId, nickname);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, DBStatus.ALREADY_LOGGED_IN));
			return;
		}

		DBStatus status = server.getDbManager().loginUser(nickname, password);

		if (status == DBStatus.LOGIN_OK) {
			clientHandler.setNickname(nickname);
			clientHandler.setClientState(ClientState.MAIN_MENU);
			server.registerLoggedUser(nickname, clientId);
		}

		clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, status));
		logger.info("Client #{} : esito login inviato (status '{}')", clientId, status);
	}
}
