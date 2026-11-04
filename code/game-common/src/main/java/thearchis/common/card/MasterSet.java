package thearchis.common.card;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
				Gson gson = new GsonBuilder().create();
				JsonArray jsonArray = JsonParser.parseReader(reader).getAsJsonArray();

				for (JsonElement element : jsonArray) {
					JsonObject obj = element.getAsJsonObject();
					CardType type = CardType.valueOf(obj.get("type").getAsString());

					Card card = switch (type) {
						case CREATURE -> gson.fromJson(obj, Creature.class);
						case SPELL -> gson.fromJson(obj, Spell.class);
					};

					cardsMap.put(card.getId(), card);
				}

				logger.info("MasterSet caricato con successo: {} carte presenti in memoria.", cardsMap.size());
			}
		} catch (Exception e) {
			logger.fatal("Errore durante il parsing del file JSON delle carte: {}", e.getMessage(), e);
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