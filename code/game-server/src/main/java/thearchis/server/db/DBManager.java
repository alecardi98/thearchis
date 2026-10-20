package thearchis.server.db;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import io.github.cdimascio.dotenv.Dotenv;
import thearchis.common.db.DBStatus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

			// creazione di un MongoClient per gestire la connessione con il database
			mongoClient = MongoClients.create(CONNECTION_URI);
			// creazione di un MongoDatabase per gestire il database
			database = mongoClient.getDatabase(DB_NAME);
			logger.info("Connessione a MongoDB ({}) creata con successo.", DB_NAME);
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

			// Controllo se il nickname esiste già filtrando solo il primo (univoci)
			Document existingUser = users.find(Filters.eq("nickname", nickname)).first();

			if (existingUser != null) {
				logger.warn("Registrazione fallita: il nickname '{}' è già in uso.", nickname);
				return DBStatus.NICKNAME_ALREADY_EXISTS;
			}

			// Inserimento del nuovo account
			Document newUser = new Document().append("nickname", nickname).append("password", password);
			users.insertOne(newUser);

			logger.info("Nuovo account registrato: '{}'.", nickname);
			return DBStatus.REGISTER_OK;

		} catch (MongoException e) {
			logger.error("Errore MongoDB durante la registrazione di '{}': {}", nickname, e.getMessage());
			return DBStatus.DB_ERROR;
		}
	}

//	public DBStatus loginUser(String nickname, String password) {
//		try {
//			MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
//
//			// Cerca il documento dell'utente tramite nickname
//			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();
//
//			// Se l'utente non esiste o la password non corrisponde -> Dati errati
//			if (userDoc == null || !userDoc.getString("password").equals(password)) {
//				logger.warn("Login fallito per il nickname '{}': credenziali errate.", nickname);
//				return DBStatus.INVALID_CREDENTIALS;
//			}
//
//			logger.info("Login eseguito con successo per '{}'.", nickname);
//			return DBStatus.LOGIN_OK;
//
//		} catch (MongoException e) {
//			logger.error("Errore MongoDB durante il login di '{}': {}", nickname, e.getMessage());
//			return DBStatus.DB_ERROR;
//		}
//	}

}
