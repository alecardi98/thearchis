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
import thearchis.common.game.Deck;
import thearchis.common.game.Player;
import thearchis.common.hero.HeroName;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;
import org.bson.conversions.Bson;

public class DBManager {

	private static final Logger logger = LogManager.getLogger("[DBManager]");
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
			if (nickname == null || password == null || nickname.isBlank() || password.isBlank()) {
				logger.error("Registrazione : Errore nelle credenziali di registrazione (credenziali null o vuote).");
				OperationResult<Void> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);

			Document initialCollection = new Document();
			for (Card card : MasterSet.getInstance().getAllCards().values()) {
				if (card.isConstrained()) {
					initialCollection.append(String.valueOf(card.getId()), 2);
				}
			}

			Document newUser = new Document().append("nickname", nickname).append("password", password)
					.append("coins", 100).append("dust", 0).append("wins", 0).append("losses", 0)
					.append("collection", initialCollection).append("decks", new ArrayList<>());

			users.insertOne(newUser);

			logger.info("Registrazione : Utente {} : Nuovo account inizializzato con successo.", nickname);

			OperationResult<Void> result = new OperationResult<>(OperationStatus.REGISTER_OK, null);
			return result;

		} catch (MongoWriteException e) {
			if (e.getError().getCode() == 11000) { // DuplicateKeyError
				logger.warn("Registrazione : Utente {} : Registrazione fallita (nickname già in uso).", nickname);
				OperationResult<Void> result = new OperationResult<>(OperationStatus.NICKNAME_ALREADY_EXISTS, null);
				return result;
			}
			logger.error("Registrazione : Utente {} : Errore di scrittura MongoDB ({}).", nickname, e.getMessage());
			OperationResult<Void> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;

		} catch (MongoException e) {
			logger.error("Registrazione : Utente {} : Errore MongoDB durante la registrazione ({}).", nickname,
					e.getMessage());
			OperationResult<Void> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	public OperationResult<Player> loginUser(String nickname, String password) {
		try {
			if (nickname == null || password == null || nickname.isBlank() || password.isBlank()) {
				logger.error("Login : Errore nelle credenziali di login (presenza di dati null).");
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Login : Utente {} : Login fallito (utente non trovato).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.INVALID_CREDENTIALS, null);
				return result;
			}

			if (!password.equals(userDoc.getString("password"))) {
				logger.warn("Login : Utente {} : Login fallito (credenziali errate).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.INVALID_CREDENTIALS, null);
				return result;
			}

			Player player = parsePlayerFromDocument(userDoc);
			if (player == null) {
				logger.warn("Login : Utente {} : Errore durante la creazione di Player.", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			logger.info("Login : Utente {} : Login eseguito con successo.", nickname);

			OperationResult<Player> result = new OperationResult<>(OperationStatus.LOGIN_OK, player);
			return result;

		} catch (MongoException e) {
			logger.error("Login : Utente {} : Errore MongoDB durante il login ({}).", nickname, e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	private Player parsePlayerFromDocument(Document userDoc) {
		try {
			String nickname = userDoc.getString("nickname");
			if (nickname == null || nickname.isBlank()) {
				logger.error("parsePlayer: Documento utente corrotto ('nickname' mancante o vuoto).");
				return null;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			if (collectionDoc == null) {
				logger.error("parsePlayer: Utente {} : Documento corrotto ('collection' mancante).", nickname);
				return null;
			}

			List<Document> decksDocList = userDoc.getList("decks", Document.class);
			if (decksDocList == null) {
				logger.error("parsePlayer: Utente {} : Documento corrotto ('decks' mancante).", nickname);
				return null;
			}

			int coins = userDoc.getInteger("coins", 0);
			int dust = userDoc.getInteger("dust", 0);
			int wins = userDoc.getInteger("wins", 0);
			int losses = userDoc.getInteger("losses", 0);

			Collection collection = parseCollectionFromDocument(collectionDoc);
			List<Deck> decks = parseDecksFromDocumentList(decksDocList);

			return new Player(nickname, coins, dust, collection, decks, wins, losses);

		} catch (ClassCastException | IllegalArgumentException e) {
			logger.error("parsePlayer: Errore di parsing/tipo nel documento utente ({}).", e.getMessage());
			return null;
		}
	}

	private Collection parseCollectionFromDocument(Document collectionDoc) {
		Collection collection = new Collection();
		for (Map.Entry<String, Object> entry : collectionDoc.entrySet()) {
			try {
				int cardId = Integer.parseInt(entry.getKey());
				int quantity = collectionDoc.getInteger(entry.getKey(), 0);
				for (int i = 0; i < quantity; i++) {
					collection.addCard(cardId);
				}
			} catch (NumberFormatException e) {
				logger.warn("parseCollection : ID carta #{} non valido nella collezione.", entry.getKey());
			} catch (ClassCastException e) {
				logger.warn("parseCollection: Valore quantità {} per carta #{} non valido.", entry.getValue(),
						entry.getKey());
			}
		}
		return collection;
	}

	private List<Deck> parseDecksFromDocumentList(List<Document> decksDocList) {
		List<Deck> decks = new ArrayList<>();

		if (decksDocList.isEmpty()) {
			return decks;
		}

		for (Document deckDoc : decksDocList) {
			if (deckDoc == null) {
				continue;
			}

			try {
				String heroStr = deckDoc.getString("hero");
				if (heroStr == null || heroStr.isBlank()) {
					logger.warn("parseDeck : HeroName del mazzo non specificato.");
					continue;
				}

				HeroName hero = HeroName.valueOf(heroStr);
				Deck deck = new Deck(hero);

				Document cardsDoc = deckDoc.get("cards", Document.class);
				if (cardsDoc == null) {
					logger.warn("parseDeck : Mazzo {} : mazzo vuoto.", heroStr);
					continue;
				}

				for (Map.Entry<String, Object> entry : cardsDoc.entrySet()) {
					try {
						int cardId = Integer.parseInt(entry.getKey());
						int quantity = cardsDoc.getInteger(entry.getKey(), 0);

						if (quantity > 0) {
							deck.addCard(cardId, quantity);
						}
					} catch (NumberFormatException e) {
						logger.warn("parseDeck : Mazzo {} : ID carta #{} non valido.", heroStr, entry.getKey());
					} catch (ClassCastException e) {
						logger.warn("parseDeck : Mazzo {} : Quantità {} non valida per carta #{}.", heroStr,
								entry.getValue(), entry.getKey());
					}
				}
				decks.add(deck);

			} catch (IllegalArgumentException e) {
				logger.warn("parseDeck : HeroName {} non valido.", deckDoc.getString("hero"));
			} catch (Exception e) {
				logger.error("parseDeck : Errore imprevisto durante il parsing di un mazzo ({}).", e.getMessage());
			}
		}
		return decks;
	}

	/*
	 * - Recupero dei dati della collezione
	 */
	public OperationResult<Collection> getUserCollection(String nickname) {
		try {
			if (nickname == null || nickname.isBlank()) {
				logger.warn("Collection : Recupero collezione fallito (nickname null o vuoto).");
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Collection : Utente {} : recupero collezione fallito (utente non trovato).", nickname);
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			if (collectionDoc == null) {
				logger.error("Collection : Utente {} : Documento corrotto ('collection' mancante).", nickname);
				return new OperationResult<>(OperationStatus.DB_ERROR, null);
			}
			Collection collection = parseCollectionFromDocument(collectionDoc);

			logger.info("Collection : Utente {} : Collezione recuperata con successo.", nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.COLLECTION_OK, collection);
			return result;

		} catch (ClassCastException e) {
			logger.error("Collection : Utente {} : Documento corrotto (tipo 'collection' o 'dust' non valido).",
					nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		} catch (MongoException e) {
			logger.error("Collection : Utente {} : Errore MongoDB durante il recupero della collezione ({}).", nickname,
					e.getMessage());
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	public OperationResult<Player> craftCard(String nickname, int cardId, int craftCost) {
		try {
			if (nickname == null || nickname.isBlank()) {
				logger.error("Craft: Craft fallito (nickname null o vuoto).");
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Craft : Utente {} : Craft fallito (utente non trovato nel DB).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			int currentDust = userDoc.getInteger("dust", 0);
			if (currentDust < craftCost) {
				logger.warn(
						"Craft : Utente {} : Craft fallito (polvere insufficiente (richiesta: {} , posseduta: {})).",
						nickname, craftCost, currentDust);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.NOT_ENOUGH_DUST, null);
				return result;
			}

			String fieldPath = "collection." + cardId;
			users.updateOne(Filters.eq("nickname", nickname),
					Updates.combine(Updates.inc("dust", -craftCost), Updates.inc(fieldPath, 1)));

			Document updatedUserDoc = users.find(Filters.eq("nickname", nickname)).first();
			Player updatedPlayer = parsePlayerFromDocument(updatedUserDoc);
			if (updatedPlayer == null) {
				logger.error("Craft : Utente {} : Errore durante il parsing del Player post-craft.", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			logger.info("Craft : Utente {} : Carta #{} craftata con successo (nuova polvere: {}).", nickname, cardId,
					updatedPlayer.getDust());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.CRAFT_OK, updatedPlayer);
			return result;

		} catch (ClassCastException e) {
			logger.error("Craft : Utente {} : Documento corrotto (tipo 'dust' non valido).", nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		} catch (MongoException e) {
			logger.error("Craft : Utente {} : Errore MongoDB durante il craft della carta #{} ({}).", nickname, cardId,
					e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	public OperationResult<Player> disenchantCard(String nickname, int cardId, int disenchantValue) {
		try {
			if (nickname == null || nickname.isBlank()) {
				logger.error("Disenchant : Disenchant fallito (nickname null o vuoto).");
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Disenchant : Utente {} : Disenchant fallito (utente non trovato nel DB).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			if (collectionDoc == null) {
				logger.error("Disenchant : Utente {} : Documento corrotto ('collection' mancante).", nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			String cardIdKey = String.valueOf(cardId);
			int currentQuantity = collectionDoc.getInteger(cardIdKey, 0);

			if (currentQuantity <= 0) {
				logger.warn("Disenchant : Utente {} : Disenchant fallito (carta #{} non posseduta).", nickname, cardId);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.CARD_NOT_OWNED, null);
				return result;
			}

			String fieldPath = "collection." + cardIdKey;
			Bson updateQuery = (currentQuantity == 1)
					? Updates.combine(Updates.inc("dust", disenchantValue), Updates.unset(fieldPath))
					: Updates.combine(Updates.inc("dust", disenchantValue), Updates.inc(fieldPath, -1));

			users.updateOne(Filters.eq("nickname", nickname), updateQuery);

			Document updatedUserDoc = users.find(Filters.eq("nickname", nickname)).first();
			Player updatedPlayer = parsePlayerFromDocument(updatedUserDoc);
			if (updatedPlayer == null) {
				logger.error("Disenchant : Utente {} : Errore durante il parsing del Player post-disenchant.",
						nickname);
				OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			logger.info("Disenchant : Utente {} : Carta #{} disincantata con successo (Nuova polvere: {}).", nickname,
					cardId, updatedPlayer.getDust());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DISENCHANT_OK, updatedPlayer);
			return result;

		} catch (ClassCastException e) {
			logger.error("Disenchant : Utente {} : Documento corrotto (tipo non valido nel documento).", nickname);
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		} catch (MongoException e) {
			logger.error("Disenchant : Utente {} : Errore MongoDB durante il disenchant della carta #{} ({}).",
					nickname, cardId, e.getMessage());
			OperationResult<Player> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}
}