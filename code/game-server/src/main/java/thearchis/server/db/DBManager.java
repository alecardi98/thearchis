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
import com.mongodb.client.model.Updates;

import io.github.cdimascio.dotenv.Dotenv;
import thearchis.common.card.Card;
import thearchis.common.card.MasterSet;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;

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

			Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();

			String rawUri = dotenv.get("CONNECTION_URI");
			if (rawUri == null || rawUri.isBlank()) {
				rawUri = System.getenv("CONNECTION_URI");
			}

			logger.info("URI Caricato dal file .env/ambiente.");

			if (rawUri == null || rawUri.isBlank()) {
				logger.fatal(
						"Impossibile avviare il DB: CONNECTION_URI non trovata nel file .env o nelle variabili di sistema.");
				throw new RuntimeException("Configurazione MongoDB mancante.");
			}

			String connectionUri = sanitizeSrvUri(rawUri);

			try {
				mongoClient = MongoClients.create(connectionUri);
				database = mongoClient.getDatabase(DB_NAME);

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

	private String sanitizeSrvUri(String uri) {
		if (!uri.contains("mongodb+srv://")) {
			return uri;
		}

		logger.info("Rilevato formato SRV: conversione dinamica ai nodi diretti dello shard.");

		int schemeEnd = uri.indexOf("://") + 3;
		String afterScheme = uri.substring(schemeEnd);
		int atIndex = afterScheme.indexOf("@");

		if (atIndex == -1) {
			return uri;
		}

		String credentials = afterScheme.substring(0, atIndex);

		return String.format(
				"mongodb://%s@ac-1ss9wkl-shard-00-00.jp638fv.mongodb.net:27017,"
						+ "ac-1ss9wkl-shard-00-01.jp638fv.mongodb.net:27017,"
						+ "ac-1ss9wkl-shard-00-02.jp638fv.mongodb.net:27017/"
						+ "%s?ssl=true&replicaSet=atlas-wejixd-shard-0&authSource=admin&retryWrites=true&w=majority",
				credentials, DB_NAME);
	}

	public void close() {
		if (mongoClient != null) {
			mongoClient.close();
			logger.info("Connessione a MongoDB chiusa.");
		}
	}

	public OperationResult<Void> registerUser(String nickname, String password) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);

			Document initialCollection = new Document();
			for (Card card : MasterSet.getInstance().getAllCards().values()) {
				if (card.isConstrained()) {
					initialCollection.append(String.valueOf(card.getId()), 2);
				}
			}

			// TODO aggiungere mazzi vuoti

			Document newUser = new Document().append("nickname", nickname).append("password", password)
					.append("coins", 100).append("dust", 0).append("wins", 0).append("losses", 0)
					.append("collection", initialCollection).append("decks", new java.util.ArrayList<>());

			users.insertOne(newUser);

			logger.info("Nuovo account e profilo inizializzati con successo per: '{}' con {} carte base.", nickname,
					initialCollection.size());

			return new OperationResult<>(OperationStatus.REGISTER_OK, null);

		} catch (MongoWriteException e) {
			if (e.getError().getCode() == 11000) { // DuplicateKeyError
				logger.warn("Registrazione fallita: il nickname '{}' è già in uso.", nickname);
				return new OperationResult<>(OperationStatus.NICKNAME_ALREADY_EXISTS, null);
			}
			logger.error("Errore di scrittura MongoDB per '{}': {}", nickname, e.getMessage());
			return new OperationResult<>(OperationStatus.DB_ERROR, null);

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante la registrazione di '{}': {}", nickname, e.getMessage());
			return new OperationResult<>(OperationStatus.DB_ERROR, null);
		}
	}

	public OperationResult<Player> loginUser(String nickname, String password) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null || !password.equals(userDoc.getString("password"))) {
				logger.warn("Login fallito per '{}' (credenziali errate).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.INVALID_CREDENTIALS, null);
				return result;
			}

			logger.info("Login eseguito con successo per '{}'.", nickname);

			Player player = parsePlayerFromDocument(userDoc);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.LOGIN_OK, player);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il login di '{}' ({}).", nickname, e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
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

	public OperationResult<Collection> getUserCollection(String nickname) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Recupero collezione fallito: utente '{}' non trovato.", nickname);
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			Collection collection = parseCollectionFromDocument(collectionDoc);

			logger.info("Collezione recuperata con successo per l'utente '{}'.", nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.COLLECTION_OK, collection);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il recupero della collezione per '{}' ({})", nickname, e.getMessage());
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
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

	public OperationResult<Player> craftCard(String nickname, int cardId, int craftCost) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Craft fallito: utente '{}' non trovato nel DB.", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			int currentDust = userDoc.getInteger("dust", 0);
			if (currentDust < craftCost) {
				logger.warn("Craft fallito per '{}': polvere insufficiente (richiesta: {}, posseduta: {}).", nickname,
						craftCost, currentDust);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.NOT_ENOUGH_DUST, null);
				return result;
			}

			String fieldPath = "collection." + cardId;
			users.updateOne(Filters.eq("nickname", nickname),
					Updates.combine(Updates.inc("dust", -craftCost), Updates.inc(fieldPath, 1)));

			Document updatedUserDoc = users.find(Filters.eq("nickname", nickname)).first();
			Player updatedPlayer = parsePlayerFromDocument(updatedUserDoc);

			logger.info("Carta #{} craftata con successo da '{}'. Nuova polvere: {}.", cardId, nickname,
					updatedPlayer.getDust());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.CRAFT_OK, updatedPlayer);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il craft della carta #{} per '{}' ({}).", cardId, nickname,
					e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	public OperationResult<Player> disenchantCard(String nickname, int cardId, int disenchantValue) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Disenchant fallito: utente '{}' non trovato nel DB.", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			String cardIdKey = String.valueOf(cardId);

			if (collectionDoc == null || !collectionDoc.containsKey(cardIdKey)) {
				logger.warn("Disenchant fallito per '{}': carta #{} non posseduta.", nickname, cardId);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.CARD_NOT_OWNED, null);
				return result;
			}

			int currentQuantity = collectionDoc.getInteger(cardIdKey, 0);
			if (currentQuantity <= 0) {
				logger.warn("Disenchant fallito per '{}': quantità insufficiente per la carta #{}.", nickname, cardId);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.CARD_NOT_OWNED, null);
				return result;
			}

			String fieldPath = "collection." + cardIdKey;
			Bson updateQuery;

			if (currentQuantity == 1) {
				updateQuery = Updates.combine(Updates.inc("dust", disenchantValue), Updates.unset(fieldPath));
			} else {
				updateQuery = Updates.combine(Updates.inc("dust", disenchantValue), Updates.inc(fieldPath, -1));
			}

			users.updateOne(Filters.eq("nickname", nickname), updateQuery);

			Document updatedUserDoc = users.find(Filters.eq("nickname", nickname)).first();
			Player updatedPlayer = parsePlayerFromDocument(updatedUserDoc);

			logger.info("Carta #{} disincantata con successo da '{}'. Nuova polvere: {}.", cardId, nickname,
					updatedPlayer.getDust());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DISENCHANT_OK, updatedPlayer);
			return result;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il disenchant della carta #{} per '{}' ({}).", cardId, nickname,
					e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

}
