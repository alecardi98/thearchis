package thearchis.server.db;

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import io.github.cdimascio.dotenv.Dotenv;
import thearchis.common.collection.Collection;
import thearchis.common.db.DBResult;
import thearchis.common.db.DBStatus;
import thearchis.common.game.Player;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

public class DBManager {

	private static final Logger logger = LogManager.getLogger(DBManager.class);
	private static final String DB_NAME = "thearchis_db";
	private static final String USERS_COLLECTION = "users";

	private MongoClient mongoClient;
	private MongoDatabase database;

	public DBManager() {
	}

	public synchronized void connect() {
		if (mongoClient == null) {

			Dotenv dotenv = Dotenv.load();
			String CONNECTION_URI = dotenv.get("CONNECTION_URI");

			if (CONNECTION_URI == null || CONNECTION_URI.isBlank()) {
				logger.fatal("Impossibile avviare il DB: MONGO_CONNECTION_URI non trovata nel file .env .");
				throw new RuntimeException("Configurazione MongoDB mancante.");
			}

			try {
				mongoClient = MongoClients.create(CONNECTION_URI);
				database = mongoClient.getDatabase(DB_NAME);

				// ping per verificare la connessione
				database.runCommand(new BsonDocument("ping", new BsonInt32(1)));

				MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
				users.createIndex(Indexes.ascending("nickname"), new IndexOptions().unique(true));

				logger.info("Connessione a MongoDB ({}) verificata e creata con successo.", DB_NAME);

			} catch (Exception e) {
				logger.fatal("Impossibile connettersi a MongoDB Atlas: {}", e.getMessage());
				close();
				throw new RuntimeException("Errore critico di connessione al Database.", e);
			}

		}
	}

	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
			logger.info("Connessione a MongoDB chiusa.");
		}
	}

	public DBResult<Void> registerUser(String nickname, String password) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);

			Document newUser = new Document().append("nickname", nickname).append("password", password)
					.append("coins", 100).append("dust", 0).append("wins", 0).append("losses", 0)
					.append("collection", new Document()).append("decks", new java.util.ArrayList<>());

			// TODO aggiungere le carte di base che hanno tutti

			users.insertOne(newUser);

			logger.info("Nuovo account e profilo inizializzati con successo per: '{}'.", nickname);

			DBResult<Void> result = new DBResult<>(DBStatus.REGISTER_OK, null);
			return result;

		} catch (MongoWriteException e) {
			if (e.getError().getCode() == 11000) { // Codice 11000 DuplicateKeyError (nickname già esistente)
				logger.warn("Registrazione fallita: il nickname '{}' è già in uso.", nickname);
				DBResult<Void> result = new DBResult<>(DBStatus.NICKNAME_ALREADY_EXISTS, null);
				return result;
			}
			logger.error("Errore di scrittura MongoDB per '{}': {}", nickname, e.getMessage());
			DBResult<Void> result = new DBResult<>(DBStatus.DB_ERROR, null);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante la registrazione di '{}': {}", nickname, e.getMessage());
			DBResult<Void> result = new DBResult<>(DBStatus.DB_ERROR, null);
			return result;
		}
	}

	public DBResult<Player> loginUser(String nickname, String password) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null || !password.equals(userDoc.getString("password"))) {
				logger.warn("Login fallito per '{}' (credenziali errate).", nickname);
				DBResult<Player> result = new DBResult<>(DBStatus.INVALID_CREDENTIALS, null);
				return result;
			}

			logger.info("Login eseguito con successo per '{}'.", nickname);

			Player player = parsePlayerFromDocument(userDoc);
			DBResult<Player> result = new DBResult<>(DBStatus.LOGIN_OK, player);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il login di '{}' ({}).", nickname, e.getMessage());
			DBResult<Player> result = new DBResult<>(DBStatus.DB_ERROR, null);
			return result;
		}
	}

	private Player parsePlayerFromDocument(Document userDoc) {
		if (userDoc == null) {
			return null;
		}

		String nickname = userDoc.getString("nickname");
		int coins = userDoc.getInteger("coins", 0);
		int dust = userDoc.getInteger("dust", 0);
		int wins = userDoc.getInteger("wins", 0);
		int losses = userDoc.getInteger("losses", 0);

		Document collectionDoc = userDoc.get("collection", Document.class);
		Collection collection = parseCollectionFromDocument(collectionDoc);

		// TODO implementare il recupero dei Deck

		Player player = new Player(nickname, coins, dust, collection, wins, losses);
		return player;
	}

	public DBResult<Collection> getUserCollection(String nickname) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Recupero collezione fallito: utente '{}' non trovato.", nickname);
				DBResult<Collection> result = new DBResult<>(DBStatus.DB_ERROR, null);
				return result;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			Collection collection = parseCollectionFromDocument(collectionDoc);

			logger.info("Collezione recuperata con successo per l'utente '{}'.", nickname);
			DBResult<Collection> result = new DBResult<>(DBStatus.COLLECTION_OK, collection);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il recupero della collezione per '{}' ({})", nickname, e.getMessage());
			DBResult<Collection> result = new DBResult<>(DBStatus.DB_ERROR, null);
			return result;
		}
	}

	private Collection parseCollectionFromDocument(Document collectionDoc) {
		Collection collection = new Collection();

		if (collectionDoc == null) {
			return collection;
		}

		for (Map.Entry<String, Object> entry : collectionDoc.entrySet()) {
			try {
				int cardId = Integer.parseInt(entry.getKey());
				int quantity = collectionDoc.getInteger(entry.getKey(), 0);

				for (int i = 0; i < quantity; i++) {
					collection.addCard(cardId);
				}

			} catch (NumberFormatException e) {
				logger.warn("ID carta non valido nella collezione: '{}'", entry.getKey());
			} catch (ClassCastException e) {
				logger.warn("Valore quantità non valido per la carta '{}'", entry.getKey());
			}
		}
		return collection;
	}

}
