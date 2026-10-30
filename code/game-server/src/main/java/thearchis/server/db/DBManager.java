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
import thearchis.common.db.DBStatus;

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

	/*
	 * Crea mongoClient e database
	 */
	public synchronized void connect() {
		if (mongoClient == null) {

			Dotenv dotenv = Dotenv.load();
			String CONNECTION_URI = dotenv.get("CONNECTION_URI");

			if (CONNECTION_URI == null || CONNECTION_URI.isBlank()) {
				logger.fatal("Impossibile avviare il DB: MONGO_CONNECTION_URI non trovata nel file .env .");
				throw new RuntimeException("Configurazione MongoDB mancante.");
			}

			try {
				// creazione di un MongoClient per gestire la connessione con il database
				mongoClient = MongoClients.create(CONNECTION_URI);
				// creazione di un MongoDatabase per gestire il database
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

	public DBStatus registerUser(String nickname, String password) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);

			// Tentativo di inserimento atomico del nuovo account
			Document newUser = new Document().append("nickname", nickname).append("password", password);
			users.insertOne(newUser);

			logger.info("Nuovo account registrato: '{}'.", nickname);
			return DBStatus.REGISTER_OK;

		} catch (MongoWriteException e) {
			if (e.getError().getCode() == 11000) { // Codice 11000 DuplicateKeyError (nickname già esistente)
				logger.warn("Registrazione fallita: il nickname '{}' è già in uso.", nickname);
				return DBStatus.NICKNAME_ALREADY_EXISTS;
			}
			logger.error("Errore di scrittura MongoDB per '{}': {}", nickname, e.getMessage());
			return DBStatus.DB_ERROR;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante la registrazione di '{}': {}", nickname, e.getMessage());
			return DBStatus.DB_ERROR;
		}
	}

	public DBStatus loginUser(String nickname, String password) {
		try {
			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null || !password.equals(userDoc.getString("password"))) {
				logger.warn("Login fallito per '{}' (credenziali errate).", nickname);
				return DBStatus.INVALID_CREDENTIALS;
			}

			logger.info("Login eseguito con successo per '{}'.", nickname);
			return DBStatus.LOGIN_OK;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante il login di '{}' ({}).", nickname, e.getMessage());
			return DBStatus.DB_ERROR;
		}
	}

}
