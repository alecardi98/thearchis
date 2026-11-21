package thearchis.common.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MessageTest {

	@Test
	@DisplayName("Costruttore: inizializza correttamente type e data")
	void testConstructor() {

		AuthenticationData data = new AuthenticationData("user", "psw");
		Message message = new Message(MessageType.LOGIN_REQUEST, data);
		assertEquals(MessageType.LOGIN_REQUEST, message.getType());
		assertEquals(data, message.getData());
	}

	@Test
	@DisplayName("Costruttore: lancia IllegalArgumentException se type è null")
	void testConstructorInvalidFields() {

		assertDoesNotThrow(() -> new Message(MessageType.COLLECTION_REQUEST, null));
		assertThrows(IllegalArgumentException.class, () -> new Message(null, null));
	}

}
