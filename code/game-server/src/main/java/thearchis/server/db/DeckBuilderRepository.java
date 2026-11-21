package thearchis.server.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeckBuilderRepository {
	private static final Logger logger = LogManager.getLogger("[ShopRepository]");

	private final MongoDBManager mongoDBManager;
	private final DocumentMapper documentMapper;

	public DeckBuilderRepository(MongoDBManager mongoDBManager, DocumentMapper documentMapper) {
		this.mongoDBManager = mongoDBManager;
		this.documentMapper = documentMapper;
	}

}
