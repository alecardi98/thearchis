package thearchis.server.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.collection.Collection;
import thearchis.common.db.DBResult;
import thearchis.common.db.DBStatus;
import thearchis.common.game.ClientState;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

public class CollectionController {

	private static final Logger logger = LogManager.getLogger(CollectionController.class);

	public CollectionController() {
	}

	public void handleCollectionRequest(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{}: ricevuta richiesta di collezione per l'utente '{}'.", clientId);

		if (clientHandler.getPlayer() == null) {
			logger.warn("Client #{}: richiesta collezione rifiutata (utente non autenticato).", clientId);

			DBResult<Collection> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}

		String nickname = clientHandler.getPlayer().getNickname();
		Server server = Server.getInstance();
		if (!server.isUserAuthenticated(nickname)) {
			logger.warn("Client #{}: richiesta collezione rifiutata (utente '{}' non autenticato).", clientId,
					nickname);

			DBResult<Collection> result = new DBResult<>(DBStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}

		DBResult<Collection> result = server.getDbManager().getUserCollection(nickname);
		DBStatus status = result.getStatus();

		if(status == DBStatus.COLLECTION_OK) {
			clientHandler.setClientState(ClientState.IN_COLLECTION);
		}
		
		clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
		logger.info("Client #{}: esito collezione inviato per '{}' (status '{}')", clientId, nickname, status);
	}

}
