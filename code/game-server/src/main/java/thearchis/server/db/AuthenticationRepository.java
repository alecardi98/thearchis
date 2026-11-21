package thearchis.server.db;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import thearchis.common.game.card.Card;
import thearchis.common.game.card.MasterSet;
import thearchis.common.game.state.Player;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

public class AuthenticationRepository {

	private static final Logger logger = LogManager.getLogger("[AuthenticationRepository]");

	private final MongoDBManager mongoDBManager;
	private final DocumentMapper documentMapper;

	public AuthenticationRepository(MongoDBManager mongoDBManager, DocumentMapper documentMapper) {
		this.mongoDBManager = mongoDBManager;
		this.documentMapper = documentMapper;
	}

	public OperationResult<Void> registerUser(String nickname, String password) {
		try {
			if (nickname == null || password == null || nickname.isBlank() || password.isBlank()) {
				logger.error("registerUser : Errore nelle credenziali di registrazione (credenziali null o vuote).");
				OperationResult<Void> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = mongoDBManager.getUsersCollection();

			Document initialCards = new Document();
			for (Card card : MasterSet.getInstance().getAllCards().values()) {
				if (card.isConstrained()) {
					initialCards.append(String.valueOf(card.getId()), 2);
				}
			}
			Document initialCollection = new Document().append("dust", 0).append("cards", initialCards);

			Document newUser = new Document().append("nickname", nickname).append("password", password)
					.append("coins", 100).append("wins", 0).append("losses", 0).append("collection", initialCollection)
					.append("decks", new ArrayList<>());

			users.insertOne(newUser);

			logger.info("Utente {} : registerUser : Nuovo account inizializzato con successo.", nickname);
			OperationResult<Void> result = new OperationResult<>(OperationStatus.REGISTER_OK, null);
			return result;

		} catch (MongoWriteException e) {
			if (e.getError().getCode() == 11000) { // DuplicateKeyError
				logger.warn("Utente {} : registerUser : Registrazione fallita (nickname già in uso).", nickname);
				OperationResult<Void> result = new OperationResult<>(OperationStatus.NICKNAME_ALREADY_EXISTS, null);
				return result;
			}
			logger.error("Utente {} : registerUser : Errore di scrittura MongoDB ({}).", nickname, e.getMessage());
			OperationResult<Void> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;

		} catch (MongoException e) {
			logger.error("Utente {} : registerUser : Errore MongoDB durante la registrazione ({}).", nickname,
					e.getMessage());
			OperationResult<Void> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	public OperationResult<Player> loginUser(String nickname, String password) {
		try {
			if (nickname == null || password == null || nickname.isBlank() || password.isBlank()) {
				logger.error("loginUser : Errore nelle credenziali di login (presenza di dati null).");
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = mongoDBManager.getUsersCollection();

			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();
			if (userDoc == null) {
				logger.warn("Utente {} : loginUser : Login fallito (utente non trovato).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.INVALID_CREDENTIALS, null);
				return result;
			}

			if (!password.equals(userDoc.getString("password"))) {
				logger.warn("Utente {} : loginUser : Login fallito (credenziali errate).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.INVALID_CREDENTIALS, null);
				return result;
			}

			Player player = documentMapper.parsePlayerFromDocument(userDoc);
			if (player == null) {
				logger.warn("Utente {} : loginUser : Errore durante la creazione di Player.", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			logger.info("Utente {} : loginUser : Login eseguito con successo.", nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.LOGIN_OK, player);
			return result;

		} catch (MongoException e) {
			logger.error("Utente {} : loginUser : Errore MongoDB durante il login ({}).", nickname, e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}
}
