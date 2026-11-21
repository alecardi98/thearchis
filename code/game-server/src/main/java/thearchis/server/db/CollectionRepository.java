package thearchis.server.db;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;

import thearchis.common.game.state.Collection;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

public class CollectionRepository {

	private static final Logger logger = LogManager.getLogger("[CollectionRepository]");

	private final MongoDBManager mongoDBManager;
	private final DocumentMapper documentMapper;

	public CollectionRepository(MongoDBManager mongoDBManager, DocumentMapper documentMapper) {
		this.mongoDBManager = mongoDBManager;
		this.documentMapper = documentMapper;
	}

	public OperationResult<Collection> getUserCollection(String nickname) {
		try {
			if (nickname == null || nickname.isBlank()) {
				logger.warn("getCollection : Recupero collezione fallito (nickname null o vuoto).");
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			MongoCollection<Document> users = mongoDBManager.getUsersCollection();
			Document userDoc = users.find(Filters.eq("nickname", nickname)).first();

			if (userDoc == null) {
				logger.warn("Utente {} : getCollection : Recupero collezione fallito (utente non trovato).", nickname);
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				return result;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			if (collectionDoc == null) {
				logger.error("Utente {} : getCollection : Documento corrotto ('collection' mancante).", nickname);
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			Collection collection = documentMapper.parseCollectionFromDocument(collectionDoc);
			if (collection == null) {
				logger.error("Utente {} : getCollection : Documento corrotto (contenuto 'collection' non valido).",
						nickname);
				OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
				return result;
			}

			logger.info("Utente {} : getCollection : Collezione recuperata con successo.", nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.COLLECTION_OK, collection);
			return result;

		} catch (ClassCastException e) {
			logger.error("Utente {} : getCollection : Documento corrotto (tipo 'collection' non valido).", nickname);
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		} catch (MongoException e) {
			logger.error("Utente {} : getCollection : Errore MongoDB durante il recupero della collezione ({}).",
					nickname, e.getMessage());
			OperationResult<Collection> result = new OperationResult<>(OperationStatus.DB_ERROR, null);
			return result;
		}
	}

	public OperationResult<Collection> craftCard(String nickname, int cardId, int craftCost) {
		try {
			if (nickname == null || nickname.isBlank()) {
				logger.error("craftCard : Craft fallito (nickname null o vuoto).");
				return new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			}

			MongoCollection<Document> users = mongoDBManager.getUsersCollection();
			String fieldPath = "collection.cards." + cardId;

			Document updatedUserDoc = users.findOneAndUpdate(
					Filters.and(Filters.eq("nickname", nickname), Filters.gte("collection.dust", craftCost)),
					Updates.combine(Updates.inc("collection.dust", -craftCost), Updates.inc(fieldPath, 1)),
					new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

			if (updatedUserDoc == null) {
				Document userDoc = users.find(Filters.eq("nickname", nickname)).first();
				if (userDoc == null) {
					logger.warn("Utente {} : craftCard : Craft fallito (utente non trovato nel DB).", nickname);
					return new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				}

				Document collectionDoc = userDoc.get("collection", Document.class);
				if (collectionDoc == null || !(collectionDoc.get("dust") instanceof Integer)) {
					logger.error(
							"Utente {} : craftCard : Documento corrotto ('collection' o 'dust' mancante/non valido).",
							nickname);
					return new OperationResult<>(OperationStatus.DB_ERROR, null);
				}

				int currentDust = collectionDoc.getInteger("dust", 0);
				logger.warn(
						"Utente {} : craftCard : Craft fallito (polvere insufficiente (richiesta: {} , posseduta: {})).",
						nickname, craftCost, currentDust);
				return new OperationResult<>(OperationStatus.NOT_ENOUGH_DUST, null);
			}

			Document updatedCollectionDoc = updatedUserDoc.get("collection", Document.class);
			if (updatedCollectionDoc == null) {
				logger.error("Utente {} : craftCard : Documento corrotto ('collection' mancante).", nickname);
				return new OperationResult<>(OperationStatus.DB_ERROR, null);
			}

			Collection updatedCollection = documentMapper.parseCollectionFromDocument(updatedCollectionDoc);
			if (updatedCollection == null) {
				logger.error("Utente {} : craftCard : Errore durante il parsing della Collection.", nickname);
				return new OperationResult<>(OperationStatus.DB_ERROR, null);
			}

			logger.info("Utente {} : craftCard : Carta #{} craftata con successo (nuova polvere: {}).", nickname,
					cardId, updatedCollection.getDust());
			return new OperationResult<>(OperationStatus.CRAFT_OK, updatedCollection);

		} catch (ClassCastException e) {
			logger.error("Utente {} : craftCard : Documento corrotto (tipo 'collection' o 'dust' non valido).",
					nickname);
			return new OperationResult<>(OperationStatus.DB_ERROR, null);
		} catch (MongoException e) {
			logger.error("Utente {} : craftCard : Errore MongoDB durante il craft della carta #{} ({}).", nickname,
					cardId, e.getMessage());
			return new OperationResult<>(OperationStatus.DB_ERROR, null);
		}
	}

	public OperationResult<Collection> disenchantCard(String nickname, int cardId, int disenchantValue) {
		try {
			if (nickname == null || nickname.isBlank()) {
				logger.error("disenchantCard : Disenchant fallito (nickname null o vuoto).");
				return new OperationResult<>(OperationStatus.BAD_REQUEST, null);
			}

			MongoCollection<Document> users = mongoDBManager.getUsersCollection();
			String cardIdKey = String.valueOf(cardId);
			String fieldPath = "collection.cards." + cardIdKey;

			List<Bson> updatePipeline = List.of(new Document("$set",
					new Document("collection.dust",
							new Document("$add",
									List.of(new Document("$ifNull", List.of("$collection.dust", 0)), disenchantValue)))
							.append(fieldPath,
									new Document("$cond", List.of(new Document("$lte", List.of("$" + fieldPath, 1)),
											"$$REMOVE", new Document("$subtract", List.of("$" + fieldPath, 1)))))));

			Document updatedUserDoc = users.findOneAndUpdate(
					Filters.and(Filters.eq("nickname", nickname), Filters.gt(fieldPath, 0)), updatePipeline,
					new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

			if (updatedUserDoc == null) {
				Document userDoc = users.find(Filters.eq("nickname", nickname)).first();
				if (userDoc == null) {
					logger.warn("Utente {} : disenchantCard : Disenchant fallito (utente non trovato nel DB).",
							nickname);
					return new OperationResult<>(OperationStatus.BAD_REQUEST, null);
				}

				Document collectionDoc = userDoc.get("collection", Document.class);
				if (collectionDoc == null) {
					logger.error("Utente {} : disenchantCard : Documento corrotto ('collection' mancante).", nickname);
					return new OperationResult<>(OperationStatus.DB_ERROR, null);
				}

				Document cardsDoc = collectionDoc.get("cards", Document.class);
				if (cardsDoc == null) {
					logger.error("Utente {} : disenchantCard : Documento corrotto ('collection.cards' mancante).",
							nickname);
					return new OperationResult<>(OperationStatus.DB_ERROR, null);
				}

				logger.warn("Utente {} : disenchantCard : Disenchant fallito (carta #{} non posseduta).", nickname,
						cardId);
				return new OperationResult<>(OperationStatus.CARD_NOT_OWNED, null);
			}

			Document updatedCollectionDoc = updatedUserDoc.get("collection", Document.class);
			if (updatedCollectionDoc == null) {
				logger.error("Utente {} : disenchantCard : Documento corrotto ('collection' mancante).", nickname);
				return new OperationResult<>(OperationStatus.DB_ERROR, null);
			}

			Collection updatedCollection = documentMapper.parseCollectionFromDocument(updatedCollectionDoc);
			if (updatedCollection == null) {
				logger.error("Utente {} : disenchantCard : Errore durante il parsing della Collection.", nickname);
				return new OperationResult<>(OperationStatus.DB_ERROR, null);
			}

			logger.info("Utente {} : disenchantCard : Carta #{} disincantata con successo (nuova polvere: {}).",
					nickname, cardId, updatedCollection.getDust());
			return new OperationResult<>(OperationStatus.DISENCHANT_OK, updatedCollection);

		} catch (ClassCastException e) {
			logger.error("Utente {} : disenchantCard : Documento corrotto (tipo non valido nel documento).", nickname);
			return new OperationResult<>(OperationStatus.DB_ERROR, null);
		} catch (MongoException e) {
			logger.error("Utente {} : disenchantCard : Errore MongoDB durante il disenchant della carta #{} ({}).",
					nickname, cardId, e.getMessage());
			return new OperationResult<>(OperationStatus.DB_ERROR, null);
		}
	}
}
