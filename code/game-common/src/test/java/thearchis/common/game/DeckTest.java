package thearchis.common.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import thearchis.common.game.hero.HeroName;
import thearchis.common.game.state.Deck;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

	@Test
	@DisplayName("Costruttore: crea correttamente un Deck")
	void testValidDeckCreation() {
		Map<Integer, Integer> cards = new HashMap<>();
		cards.put(1, 2);

		assertDoesNotThrow(() -> new Deck(HeroName.AETHELGARD, cards));
	}

	@Test
	@DisplayName("Costruttore: IllegalArgumentException se hero è null")
	void testConstructorNullHero() {
		Map<Integer, Integer> cards = new HashMap<>();
		assertThrows(IllegalArgumentException.class, () -> new Deck(null, cards));
	}

	@Test
	@DisplayName("Costruttore: IllegalArgumentException se cards è null")
	void testConstructorNullCards() {
		assertThrows(IllegalArgumentException.class, () -> new Deck(HeroName.LYRA, null));
	}

}