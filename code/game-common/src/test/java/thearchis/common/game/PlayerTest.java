package thearchis.common.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlayerTest {

	@Test
	@DisplayName("Costruttore: inizializza correttamente un Player")
	void testValidPlayerCreation() {
		Collection collection = mock(Collection.class);
		List<Deck> decks = new ArrayList<>();
		decks.add(mock(Deck.class));

		Player player = new Player("nick", 100, collection, decks, 1, 0);

		assertEquals("nick", player.getNickname());
		assertEquals(100, player.getCoins());
		assertSame(collection, player.getCollection());
		assertSame(decks, player.getDecks());
		assertSame(1, player.getWins());
		assertSame(0, player.getLosses());
	}

	@Test
	@DisplayName("Costruttore: IllegalArgumentException per nickname null o vuoto")
	void testConstructorInvalidNickname() {
		Collection collection = mock(Collection.class);
		List<Deck> decks = Collections.emptyList();

		assertThrows(IllegalArgumentException.class, () -> new Player(null, 100, collection, decks, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new Player("   ", 100, collection, decks, 0, 0));
	}

	@Test
	@DisplayName("Costruttore: IllegalArgumentException per valori numerici negativi")
	void testConstructorNegativeNumbers() {
		Collection collection = mock(Collection.class);
		List<Deck> decks = Collections.emptyList();

		assertThrows(IllegalArgumentException.class, () -> new Player("nick", -1, collection, decks, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new Player("nick", 100, collection, decks, -1, 0));
		assertThrows(IllegalArgumentException.class, () -> new Player("nick", 100, collection, decks, 0, -1));
	}

	@Test
	@DisplayName("Costruttore: IllegalArgumentException per collection o decks null")
	void testConstructorNullObjects() {
		Collection collection = mock(Collection.class);
		List<Deck> decks = Collections.emptyList();

		assertThrows(IllegalArgumentException.class, () -> new Player("nick", 100, null, decks, 0, 0));
		assertThrows(IllegalArgumentException.class, () -> new Player("nick", 100, collection, null, 0, 0));
	}

	@Test
	@DisplayName("setNickname: IllegalArgumentException se nickname null o vuoto")
	void testSetNickname() {
		Player player = new Player("nick", 50, mock(Collection.class), Collections.emptyList(), 1, 1);

		player.setNickname("user");
		assertEquals("user", player.getNickname());

		assertThrows(IllegalArgumentException.class, () -> player.setNickname(null));
		assertThrows(IllegalArgumentException.class, () -> player.setNickname("  "));
	}

	@Test
	@DisplayName("setCollection: IllegalArgumentException se collection null")
	void testSetCollection() {
		Player player = new Player("nick", 50, mock(Collection.class), Collections.emptyList(), 1, 1);
		Collection newCollection = mock(Collection.class);

		player.setCollection(newCollection);
		assertSame(newCollection, player.getCollection());

		assertThrows(IllegalArgumentException.class, () -> player.setCollection(null));
	}

}