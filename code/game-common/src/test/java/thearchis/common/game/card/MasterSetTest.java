package thearchis.common.game.card;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MasterSetTest {

	private static MasterSet masterSet;

	@BeforeAll
	static void setUp() {
		masterSet = MasterSet.getInstance();
	}

	@Test
	@DisplayName("Tutti i file JSON dei CardSet devono esistere nel classpath")
	void testAllCardSetFilesExist() {
		for (CardSet set : CardSet.values()) {
			String filePath = "/" + set.name() + ".json";
			assertNotNull(MasterSet.class.getResourceAsStream(filePath),
					"Il file JSON per il set " + set + " (" + filePath + ") deve esistere nel classpath");
		}
	}

	@Test
	@DisplayName("Il MasterSet deve caricare correttamente il catalogo da tutti i CardSet")
	void testMasterSetLoading() {
		assertNotNull(masterSet, "L'istanza del MasterSet non deve essere null");
		Map<Integer, Card> cards = masterSet.getAllCards();
		assertFalse(cards.isEmpty(), "La mappa delle carte non deve essere vuota");
	}

	@Test
	@DisplayName("Recupero carta esistente tramite ID")
	void testGetCardByIdSuccess() {
		Card card = masterSet.getCardById(2);
		assertNotNull(card, "La carta con ID 2 deve esistere nei set caricati");
		assertEquals(2, card.getId());
	}

	@Test
	@DisplayName("Recupero carta inesistente restituisce null")
	void testGetCardByIdNotFound() {
		Card card = masterSet.getCardById(999999);
		assertNull(card, "Una carta non presente nel catalogo deve restituire null");
	}

	@Test
	@DisplayName("Tutte le carte caricate nel MasterSet devono essere istanze valide di Creature o Spell")
	void testAllLoadedCardsAreValid() {
		Map<Integer, Card> cards = masterSet.getAllCards();
		for (Map.Entry<Integer, Card> entry : cards.entrySet()) {
			Card card = entry.getValue();
			assertNotNull(card);
			assertEquals(entry.getKey(), card.getId());
			assertTrue(card.getId() > 0);
			assertTrue(card instanceof Creature || card instanceof Spell);
		}
	}

	@Test
	@DisplayName("getInstance: restituisce lo stesso singleton")
	void testGetInstance() {
		assertSame(masterSet, MasterSet.getInstance());
	}

}