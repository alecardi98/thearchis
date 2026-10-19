package thearchis.server.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import io.github.cdimascio.dotenv.Dotenv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBManager {

	private static final Logger logger = LogManager.getLogger(DBManager.class);
	private static final String DB_NAME = "thearchis_db";

	private static MongoClient mongoClient;
	private static MongoDatabase database;

	public static synchronized void connect() {
		if (mongoClient == null) {

			Dotenv dotenv = Dotenv.load();
			String CONNECTION_URI = dotenv.get("CONNECTION_URI");

			if (CONNECTION_URI == null || CONNECTION_URI.isBlank()) {
				logger.fatal("Impossibile avviare il DB: MONGO_CONNECTION_URI non trovata nel file .env .");
				throw new RuntimeException("Configurazione MongoDB mancante.");
			}

			mongoClient = MongoClients.create(CONNECTION_URI);
			database = mongoClient.getDatabase(DB_NAME);
			logger.info("Connessione a MongoDB ({}) creata con successo.", DB_NAME);
		}
	}

	public static MongoDatabase getDatabase() {
		if (database == null) {
			connect();
		}
		return database;
	}

	public static void close() {
		if (mongoClient != null) {
			mongoClient.close();
			logger.info("Connessione a MongoDB chiusa.");
		}
	}
}
