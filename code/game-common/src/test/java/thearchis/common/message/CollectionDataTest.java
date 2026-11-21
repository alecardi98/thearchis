package thearchis.common.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectionDataTest {

	@Test
	@DisplayName("Costruttore: inizializza correttamente cardId")
	void testConstructor() {
		CollectionData collectionData = new CollectionData(1);
		assertEquals(1, collectionData.getCardId());
	}

	@Test
	@DisplayName("Costruttore: lancia IllegalArgumentException se cardId <= 0")
	void testConstructorInvalidFields() {
		assertDoesNotThrow(() -> new CollectionData(1));
		assertThrows(IllegalArgumentException.class, () -> new CollectionData(0));
		assertThrows(IllegalArgumentException.class, () -> new CollectionData(-1));
	}

}
