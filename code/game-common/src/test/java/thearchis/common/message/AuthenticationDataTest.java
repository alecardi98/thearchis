package thearchis.common.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationDataTest {

	@Test
	@DisplayName("Costruttore: inizializza correttamente nickname e password")
	void testConstructor() {
		AuthenticationData authData = new AuthenticationData("PlayerOne", "Secret123");

		assertEquals("PlayerOne", authData.getNickname());
		assertEquals("Secret123", authData.getPassword());
	}

	@Test
	@DisplayName("Costruttore: lancia IllegalArgumentException se nickname o password sono null o vuoti")
	void testConstructorInvalidFields() {
		assertThrows(IllegalArgumentException.class, () -> new AuthenticationData(null, "psw"));
		assertThrows(IllegalArgumentException.class, () -> new AuthenticationData("", "psw"));
		assertThrows(IllegalArgumentException.class, () -> new AuthenticationData("   ", "psw"));

		assertThrows(IllegalArgumentException.class, () -> new AuthenticationData("user", null));
		assertThrows(IllegalArgumentException.class, () -> new AuthenticationData("user", ""));
		assertThrows(IllegalArgumentException.class, () -> new AuthenticationData("user", "   "));
	}

}