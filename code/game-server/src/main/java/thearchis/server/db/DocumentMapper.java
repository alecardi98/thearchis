package thearchis.server.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;

import thearchis.common.game.hero.HeroName;
import thearchis.common.game.state.Collection;
import thearchis.common.game.state.Deck;
import thearchis.common.game.state.Player;

public class DocumentMapper {

	private static final Logger logger = LogManager.getLogger("[DocumentMapper]");

	public DocumentMapper() {
	}
	
	public Player parsePlayerFromDocument(Document userDoc) {
		try {
			String nickname = userDoc.getString("nickname");
			if (nickname == null || nickname.isBlank()) {
				logger.error("parsePlayer : Documento corrotto ('nickname' mancante o vuoto).");
				return null;
			}

			Document collectionDoc = userDoc.get("collection", Document.class);
			if (collectionDoc == null) {
				logger.error("Utente {} : parsePlayer : Documento corrotto ('collection' mancante).", nickname);
				return null;
			}
			Collection collection = parseCollectionFromDocument(collectionDoc);
			if (collection == null) {
				logger.error("Utente {} : parsePlayer : Sotto-documento 'collection' corrotto.", nickname);
				return null;
			}

			List<Document> decksDocList = userDoc.getList("decks", Document.class);
			if (decksDocList == null) {
				logger.error("Utente {} : parsePlayer : Documento corrotto ('decks' mancante).", nickname);
				return null;
			}
			List<Deck> decks = parseDecksFromDocumentList(decksDocList);

			int coins = userDoc.getInteger("coins", 0);
			int wins = userDoc.getInteger("wins", 0);
			int losses = userDoc.getInteger("losses", 0);

			Player result = new Player(nickname, coins, collection, decks, wins, losses);
			return result;

		} catch (ClassCastException | IllegalArgumentException e) {
			logger.error("parsePlayer : Errore di parsing/tipo nel documento utente ({}).", e.getMessage());
			return null;
		}
	}

	public Collection parseCollectionFromDocument(Document collectionDoc) {
		try {
			Document cardsDoc = collectionDoc.get("cards", Document.class);
			if (cardsDoc == null) {
				logger.error("parseCollection : Sotto-documento 'collection' corrotto ('cards' mancante).");
				return null;
			}

			int dust = collectionDoc.getInteger("dust", 0);
			Collection collection = new Collection(new HashMap<>(), dust);

			for (Map.Entry<String, Object> entry : cardsDoc.entrySet()) {
				try {
					int cardId = Integer.parseInt(entry.getKey());
					int quantity = cardsDoc.getInteger(entry.getKey(), 0);
					for (int i = 0; i < quantity; i++) {
						collection.addCard(cardId);
					}
				} catch (NumberFormatException e) {
					logger.warn("parseCollection : Errore ID carta #{} non valido nella collezione.", entry.getKey());
				} catch (ClassCastException e) {
					logger.warn("parseCollection : Errore valore quantità {} per carta #{} non valido.",
							entry.getValue(), entry.getKey());
				}
			}
			return collection;

		} catch (ClassCastException e) {
			logger.error("parseCollection : Errore tipo non valido in 'collection' ({}).", e.getMessage());
			return null;
		}
	}

	public List<Deck> parseDecksFromDocumentList(List<Document> decksDocList) {
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
					logger.warn("parseDeck : Errore HeroName del mazzo non specificato.");
					continue;
				}

				HeroName hero = HeroName.valueOf(heroStr);
				Deck deck = new Deck(hero, new HashMap<>());

				Document cardsDoc = deckDoc.get("cards", Document.class);
				if (cardsDoc == null) {
					logger.warn("Mazzo {} : parseDeck : Errore mazzo vuoto.", heroStr);
					decks.add(deck);
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
						logger.warn("Mazzo {} : parseDeck : Errore ID carta #{} non valido.", heroStr, entry.getKey());
					} catch (ClassCastException e) {
						logger.warn("Mazzo {} : parseDeck : Errore quantità {} non valida per carta #{}.", heroStr,
								entry.getValue(), entry.getKey());
					}
				}
				decks.add(deck);

			} catch (IllegalArgumentException e) {
				logger.warn("parseDeck : Errore HeroName {} non valido.", deckDoc.getString("hero"));
			} catch (Exception e) {
				logger.error("parseDeck : Errore imprevisto durante il parsing di un mazzo ({}).", e.getMessage());
			}
		}
		return decks;
	}

}
