package thearchis.common.card;

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
	@DisplayName("Il MasterSet deve caricare correttamente il catalogo dal file JSON")
	void testMasterSetLoading() {
		assertNotNull(masterSet, "L'istanza del MasterSet non deve essere null");
		Map<Integer, Card> cards = masterSet.getAllCards();
		assertFalse(cards.isEmpty(), "La mappa delle carte non deve essere vuota");
	}

	@Test
	@DisplayName("Recupero carta esistente tramite ID")
	void testGetCardByIdSuccess() {
		Card card = masterSet.getCardById(2);
		assertNotNull(card, "La carta con ID 2 deve esistere nel file cards.json");
		assertEquals(2, card.getId());
	}

	@Test
	@DisplayName("Recupero carta inesistente restituisce null")
	void testGetCardByIdNotFound() {
		Card card = masterSet.getCardById(999999);
		assertNull(card, "Una carta non presente nel catalogo deve restituire null");
	}

	@Test
	@DisplayName("Tutte le carte caricate nel MasterSet devono essere prive di valori null nei campi chiave")
	void testAllLoadedCardsAreValid() {
		Map<Integer, Card> cards = masterSet.getAllCards();
		for (Card card : cards.values()) {
			assertNotNull(card.getId());
			assertTrue(card.getId() > 0);
			assertNotNull(card.isConstrained());
		}
	}
}