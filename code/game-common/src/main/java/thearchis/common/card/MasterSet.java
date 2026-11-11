package thearchis.common.card;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import thearchis.common.hero.HeroName;

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

	private static final Logger logger = LogManager.getLogger(MasterSet.class);
	private static final String CARDS_FILE_PATH = "/cards.json";

	private static MasterSet instance;
	private final Map<Integer, Card> cardsMap = new HashMap<>();

	private MasterSet() {
		loadCardsFromJson();
	}

	public static synchronized MasterSet getInstance() {
		if (instance == null) {
			instance = new MasterSet();
		}
		return instance;
	}

	private void loadCardsFromJson() {
		try (InputStream inputStream = getClass().getResourceAsStream(CARDS_FILE_PATH)) {
			if (inputStream == null) {
				logger.fatal("Impossibile trovare il file delle carte: {}", CARDS_FILE_PATH);
				throw new RuntimeException("File " + CARDS_FILE_PATH + " non trovato nel classpath.");
			}

			try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

				for (JsonElement element : jsonArray) {
					JsonObject obj = element.getAsJsonObject();

					int id = (obj.has("id") && !obj.get("id").isJsonNull() && obj.get("id").isJsonPrimitive())
							? obj.get("id").getAsInt()
							: -1;

					try {
						String name = obj.has("name") ? obj.get("name").getAsString() : null;
						int manaCost = obj.has("manaCost") ? obj.get("manaCost").getAsInt() : -1;
						CardType type = obj.has("type") ? CardType.valueOf(obj.get("type").getAsString()) : null;
						CardRarity rarity = obj.has("rarity") ? CardRarity.valueOf(obj.get("rarity").getAsString())
								: null;
						HeroName hero = obj.has("hero") ? HeroName.valueOf(obj.get("hero").getAsString()) : null;
						String description = obj.has("description") ? obj.get("description").getAsString() : "";

						Boolean constrained = null;
						if (obj.has("constrained") && obj.get("constrained").isJsonPrimitive()
								&& obj.get("constrained").getAsJsonPrimitive().isBoolean()) {
							constrained = obj.get("constrained").getAsBoolean();
						}

						Card card = null;

						if (type != null) {
							switch (type) {
							case CREATURE:
								CreatureSubtype subtype = obj.has("subtype")
										? CreatureSubtype.valueOf(obj.get("subtype").getAsString())
										: null;
								int atk = obj.has("atk") ? obj.get("atk").getAsInt() : -1;
								int def = obj.has("def") ? obj.get("def").getAsInt() : -1;

								card = new Creature(id, name, manaCost, type, rarity, hero, description, constrained,
										subtype, atk, def);
								break;

							case SPELL:
								card = new Spell(id, name, manaCost, type, rarity, hero, description, constrained);
								break;
							}
						}
						cardsMap.put(card.getId(), card);

					} catch (InvalidCardDataException e) {
						throw e;
					} catch (Exception e) {
						throw new InvalidCardDataException(id, "Formato JSON non valido: " + e.getMessage());
					}
				}
				logger.info("MasterSet caricato con successo: {} carte presenti in memoria.", cardsMap.size());
			}
		} catch (InvalidCardDataException e) {
			logger.fatal("Caricamento MasterSet INTERROTTO: {}", e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.fatal("Errore critico durante la lettura del file JSON delle carte: {}", e.getMessage(), e);
			throw new RuntimeException("Impossibile caricare il catalogo carte da JSON.", e);
		}
	}

	public Card getCardById(int id) {
		return cardsMap.get(id);
	}

	public Map<Integer, Card> getAllCards() {
		return Collections.unmodifiableMap(cardsMap);
	}
}