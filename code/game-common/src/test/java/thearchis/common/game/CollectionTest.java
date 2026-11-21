package thearchis.common.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import thearchis.common.game.state.Collection;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectionTest {

	@Test
	@DisplayName("Costruttore: crea correttamente una Collection")
	void testValidCollectionCreation() {
		Map<Integer, Integer> cards = new HashMap<>();
		cards.put(1, 2);

		Collection collection = new Collection(cards, 0);

		assertEquals(0, collection.getDust());
		assertEquals(2, collection.getCardQuantity(1));
	}

	@Test
	@DisplayName("Costruttore: IllegalArgumentException se cards è null")
	void testConstructorNullCards() {
		assertThrows(IllegalArgumentException.class, () -> new Collection(null, 100));
	}

	@Test
	@DisplayName("Costruttore: lancia IllegalArgumentException se dust < 0")
	void testConstructorNegativeDust() {
		Map<Integer, Integer> cards = new HashMap<>();
		assertThrows(IllegalArgumentException.class, () -> new Collection(cards, -1));
	}

	@Test
	@DisplayName("addCard: aggiunge una nuova carta e incrementa la quantità se già presente")
	void testAddCard() {
		Collection collection = new Collection(new HashMap<>(), 50);

		assertEquals(0, collection.getCardQuantity(10));

		collection.addCard(10);
		assertEquals(1, collection.getCardQuantity(10));

		collection.addCard(10);
		assertEquals(2, collection.getCardQuantity(10));
	}

	@Test
	@DisplayName("removeCard: gestisce carta assente, copia singola e copie multiple")
	void testRemoveCard() {
		Map<Integer, Integer> cards = new HashMap<>();
		cards.put(5, 2);
		Collection collection = new Collection(cards, 50);

		assertFalse(collection.removeCard(99));

		assertTrue(collection.removeCard(5));
		assertEquals(1, collection.getCardQuantity(5));

		assertTrue(collection.removeCard(5));
		assertEquals(0, collection.getCardQuantity(5));
		assertFalse(cards.containsKey(5));

		assertFalse(collection.removeCard(5));
	}

}