package thearchis.server.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import io.github.cdimascio.dotenv.Dotenv;

public class MongoDBManager {

	private static final Logger logger = LogManager.getLogger("[MongoDBManager]");

	private static final String DB_NAME = "thearchis_db";
	private static final String USERS_COLLECTION = "users";

	private MongoClient mongoClient;
	private MongoDatabase database;

	public MongoDBManager() {
	}

	public synchronized void dbConnect() {
		if (mongoClient == null) {

			Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();

			String rawUri = dotenv.get("CONNECTION_URI");
			if (rawUri == null || rawUri.isBlank()) {
				rawUri = System.getenv("CONNECTION_URI");
			}

			logger.info("dbConnect : URI Caricato dal file .env/ambiente.");

			if (rawUri == null || rawUri.isBlank()) {
				logger.fatal("dbConnect : Impossibile avviare il DB (CONNECTION_URI non trovata nel file .env).");
				throw new RuntimeException("dbConnect : Configurazione MongoDB mancante.");
			}

			String connectionUri = sanitizeSrvUri(rawUri);

			try {
				mongoClient = MongoClients.create(connectionUri);
				database = mongoClient.getDatabase(DB_NAME);

				database.runCommand(new BsonDocument("ping", new BsonInt32(1)));

				MongoCollection<Document> users = database.getCollection(USERS_COLLECTION);
				users.createIndex(Indexes.ascending("nickname"), new IndexOptions().unique(true));

				logger.info("dbConnect : Connessione a MongoDB ({}) verificata e creata con successo.", DB_NAME);

			} catch (Exception e) {
				logger.fatal("dbConnect : Impossibile connettersi a MongoDB Atlas ({}).", e.getMessage());
				dbClose();
				throw new RuntimeException("dbConnect : Errore critico di connessione al Database.", e);
			}
		}
	}

	private String sanitizeSrvUri(String uri) {
		if (!uri.contains("mongodb+srv://")) {
			return uri;
		}

		logger.info("sanitizeSrvUri : Rilevato formato SRV, conversione dinamica.");

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

	public void dbClose() {
		if (mongoClient != null) {
			mongoClient.close();
			logger.info("dbClose : Connessione a MongoDB chiusa.");
		}
	}

	public MongoDatabase getDatabase() {
		return database;
	}

	public MongoCollection<Document> getUsersCollection() {
		if (database == null) {
			throw new IllegalStateException("getUsersCollection : Database non connesso.");
		}
		return database.getCollection(USERS_COLLECTION);
	}

}
