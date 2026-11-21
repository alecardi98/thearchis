package thearchis.common.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import thearchis.common.game.state.ClientState;

class BackNavigationDataTest {

	@Test
	@DisplayName("Costruttore: inizializza correttamente destination")
	void testConstructor() {
		BackNavigationData backNavigationData = new BackNavigationData(ClientState.MAIN_MENU);
		assertEquals(ClientState.MAIN_MENU, backNavigationData.getDestination());
	}

	@Test
	@DisplayName("Costruttore: lancia IllegalArgumentException se destination è null")
	void testConstructorInvalidFields() {
		assertDoesNotThrow(() -> new BackNavigationData(ClientState.MAIN_MENU));
		assertThrows(IllegalArgumentException.class, () -> new BackNavigationData(null));
	}

}
