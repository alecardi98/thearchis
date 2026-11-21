package thearchis.common.game.card;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import thearchis.common.game.hero.HeroName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MasterSet {

	private static final Logger logger = LogManager.getLogger("[MasterSet]");
	private static MasterSet instance;
	private final Map<Integer, Card> cardsMap = new HashMap<>();

	private MasterSet() {
		loadAllSetsFromJson();
	}

	public static synchronized MasterSet getInstance() {
		if (instance == null) {
			instance = new MasterSet();
		}
		return instance;
	}

	private void loadAllSetsFromJson() {
		for (CardSet set : CardSet.values()) {
			String filePath = "/" + set.name() + ".json";
			loadCardsFromFile(set, filePath);
		}
		logger.info("loadAllSetsFromJson : MasterSet caricato con successo ({} carte totali da {} set).",
				cardsMap.size(), CardSet.values().length);
	}

	private void loadCardsFromFile(CardSet set, String filePath) {
		try (InputStream inputStream = getClass().getResourceAsStream(filePath)) {
			if (inputStream == null) {
				logger.fatal("loadCardsFromFile : Impossibile trovare il file delle carte {}.", filePath);
				throw new RuntimeException("File " + filePath + " non trovato nel classpath.");
			}

			try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();
				int countBefore = cardsMap.size();

				for (JsonElement element : jsonArray) {
					JsonObject obj = element.getAsJsonObject();

					int id = (obj.has("id") && !obj.get("id").isJsonNull() && obj.get("id").isJsonPrimitive())
							? obj.get("id").getAsInt()
							: -1;

					try {
						String name = obj.has("name") && !obj.get("name").isJsonNull() ? obj.get("name").getAsString()
								: null;
						int manaCost = obj.has("manaCost") && !obj.get("manaCost").isJsonNull()
								? obj.get("manaCost").getAsInt()
								: -1;
						CardType type = obj.has("type") && !obj.get("type").isJsonNull()
								? CardType.valueOf(obj.get("type").getAsString())
								: null;
						CardRarity rarity = obj.has("rarity") && !obj.get("rarity").isJsonNull()
								? CardRarity.valueOf(obj.get("rarity").getAsString())
								: null;
						HeroName hero = obj.has("hero") && !obj.get("hero").isJsonNull()
								? HeroName.valueOf(obj.get("hero").getAsString())
								: null;
						String description = obj.has("description") && !obj.get("description").isJsonNull()
								? obj.get("description").getAsString()
								: "";

						Boolean constrained = null;
						if (obj.has("constrained") && obj.get("constrained").isJsonPrimitive()
								&& obj.get("constrained").getAsJsonPrimitive().isBoolean()) {
							constrained = obj.get("constrained").getAsBoolean();
						}

						if (type == null) {
							throw new IllegalArgumentException("Card : type non può essere null.");
						}

						Card card = null;

						switch (type) {
						case CREATURE:
							CreatureSubtype subtype = obj.has("subtype") && !obj.get("subtype").isJsonNull()
									? CreatureSubtype.valueOf(obj.get("subtype").getAsString())
									: null;
							int atk = obj.has("atk") && !obj.get("atk").isJsonNull() ? obj.get("atk").getAsInt() : -1;
							int def = obj.has("def") && !obj.get("def").isJsonNull() ? obj.get("def").getAsInt() : -1;

							card = new Creature(id, name, manaCost, type, rarity, hero, description, constrained,
									subtype, atk, def);
							break;

						case SPELL:
							card = new Spell(id, name, manaCost, type, rarity, hero, description, constrained);
							break;
						}

						if (cardsMap.containsKey(card.getId())) {
							throw new IllegalArgumentException("ID carta duplicato: " + card.getId() + ".");
						}

						cardsMap.put(card.getId(), card);

					} catch (Exception e) {
						throw new IllegalArgumentException("Errore nel parsing della carta con id " + id + " nel file "
								+ filePath + ": " + e.getMessage(), e);
					}
				}
				logger.info("loadCardsFromFile : Set {} caricato con successo ({} carte).", set,
						cardsMap.size() - countBefore);
			}
		} catch (Exception e) {
			logger.fatal("loadCardsFromFile : Errore critico durante la lettura del file {} ({}).", filePath,
					e.getMessage(), e);
			throw new RuntimeException(
					"Impossibile caricare il catalogo carte da " + filePath + " (" + e.getMessage() + ").", e);
		}
	}

	public Card getCardById(int id) {
		return cardsMap.get(id);
	}

	public Map<Integer, Card> getAllCards() {
		return Collections.unmodifiableMap(cardsMap);
	}
}