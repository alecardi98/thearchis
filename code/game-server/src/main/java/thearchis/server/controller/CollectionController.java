package thearchis.server.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.card.Card;
import thearchis.common.card.MasterSet;
import thearchis.common.game.ClientState;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

public class CollectionController {

	private static final Logger logger = LogManager.getLogger(CollectionController.class);

	public CollectionController() {
	}

	/*
	 * Gestisce COLLECTION_REQUEST per l'apertura dell'interfaccia 'collezione':
	 * invia al client la Collection
	 */
	public void handleCollectionRequest(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{}: ricevuta richiesta di collezione.", clientId);

		if (clientHandler.getPlayer() == null) {
			logger.warn("Client #{}: richiesta collezione rifiutata (player null).", clientId);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}
		String nickname = clientHandler.getPlayer().getNickname();

		Server server = Server.getInstance();
		if (!server.isUserAuthenticated(nickname)) {
			logger.warn("Client #{}: richiesta collezione rifiutata (utente '{}' non autenticato).", clientId,
					nickname);

			OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta collezione rifiutata (messaggio o payload nullo).", clientId, nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof CollectionData)) {
			logger.warn("Client #{}: richiesta collezione rifiutata (payload non di tipo CollectionData).", clientId,
					nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}

		CollectionData collectionData = (CollectionData) msg.getData();
		int collectionRequest = collectionData.getCardId();

		if (collectionRequest != 0) {
			logger.warn("Client #{}: richiesta collezione rifiutata (payload non valido).", clientId, nickname);

			OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
			return;
		}

		OperationResult<Collection> result = server.getDBManager().getUserCollection(nickname);
		OperationStatus status = result.getStatus();

		if (status == OperationStatus.COLLECTION_OK) {
			clientHandler.setClientState(ClientState.IN_COLLECTION);
		}

		clientHandler.sendMessage(new Message(MessageType.COLLECTION_RESPONSE, result));
		logger.info("Client #{}: esito collezione inviato per '{}' (status '{}')", clientId, nickname, status);
	}

	/*
	 * Gestisce CRAFT_REQUEST per il craft delle carte: invia al client il Player
	 * aggiornato
	 */
	public void handleCraftRequest(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{}: ricevuta richiesta di craft.", clientId);

		if (clientHandler.getPlayer() == null) {
			logger.warn("Client #{}: richiesta craft rifiutata (player null).", clientId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));
			return;
		}

		String nickname = clientHandler.getPlayer().getNickname();
		Server server = Server.getInstance();
		if (!server.isUserAuthenticated(nickname)) {
			logger.warn("Client #{}: richiesta craft rifiutata (utente {} non autenticato).", clientId, nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));
			return;
		}

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta craft rifiutata (messaggio o payload nullo).", clientId, nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof CollectionData)) {
			logger.warn("Client #{}: richiesta craft rifiutata (payload non di tipo CollectionData).", clientId,
					nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));
			return;
		}

		CollectionData cardToCraft = (CollectionData) msg.getData();
		int cardId = cardToCraft.getCardId();

		Card card = MasterSet.getInstance().getCardById(cardId);
		if (card == null) {
			logger.warn("Client #{}: richiesta craft fallita (carta #{} non trovata nel MasterSet).", clientId, cardId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));
			return;
		}

		if (card.isConstrained()) {
			logger.warn("Client #{}: richiesta craft rifiutata (carta #{} vincolata).", clientId, cardId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));
			return;
		}

		int craftCost = card.getCraftCost();

		OperationResult<Player> result = server.getDBManager().craftCard(nickname, cardId, craftCost);
		clientHandler.sendMessage(new Message(MessageType.CRAFT_RESPONSE, result));

		OperationStatus status = result.getStatus();
		logger.info("Client #{}: esito craft per carta #{} inviato (status {}).", clientId, cardId, status);
	}

	/*
	 * Gestisce DISENCHANT_REQUEST per il disenchant delle carte: invia al client il
	 * Player aggiornato
	 */
	public void handleDisenchantRequest(ClientHandler clientHandler, Message msg) {
		int clientId = clientHandler.getClientId();
		logger.info("Client #{}: ricevuta richiesta di disenchant.", clientId);

		if (clientHandler.getPlayer() == null) {
			logger.warn("Client #{}: richiesta disenchant rifiutata (utente non autenticato).", clientId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));
			return;
		}

		String nickname = clientHandler.getPlayer().getNickname();
		Server server = Server.getInstance();
		if (!server.isUserAuthenticated(nickname)) {
			logger.warn("Client #{}: richiesta disenchant rifiutata (utente {} non autenticato).", clientId, nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));
			return;
		}

		if (msg == null || msg.getData() == null) {
			logger.warn("Client #{}: richiesta disenchant rifiutata (messaggio o payload nullo).", clientId, nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));
			return;
		}

		if (!(msg.getData() instanceof CollectionData)) {
			logger.warn("Client #{}: richiesta disenchant rifiutata (payload non di tipo CollectionData).", clientId,
					nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));
			return;
		}

		CollectionData cardToDisenchant = (CollectionData) msg.getData();
		int cardId = cardToDisenchant.getCardId();

		Card card = MasterSet.getInstance().getCardById(cardId);
		if (card == null) {
			logger.warn("Client #{}: richiesta disenchant fallita (carta #{} non trovata nel MasterSet).", clientId,
					cardId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));
			return;
		}

		if (card.isConstrained()) {
			logger.warn("Client #{}: richiesta disenchant rifiutata (carta #{} vincolata).", clientId, cardId);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));
			return;
		}

		int disenchantValue = card.getDisenchantValue();

		OperationResult<Player> result = server.getDBManager().disenchantCard(nickname, cardId, disenchantValue);
		clientHandler.sendMessage(new Message(MessageType.DISENCHANT_RESPONSE, result));

		OperationStatus status = result.getStatus();
		logger.info("Client #{}: esito disenchant per carta #{} inviato (status {}).", clientId, cardId, status);
	}

}
