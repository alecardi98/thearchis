package thearchis.common.message;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperationResultTest {

	@Test
	@DisplayName("Costruttore: inizializza correttamente status e data")
	void testConstructor() {
		OperationResult<Void> operationResult = new OperationResult<>(OperationStatus.BAD_REQUEST, null);
		assertEquals(OperationStatus.BAD_REQUEST, operationResult.getStatus());
	}

	@Test
	@DisplayName("Costruttore: lancia IllegalArgumentException se status è null")
	void testConstructorInvalidFields() {
		assertDoesNotThrow(() -> new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		assertThrows(IllegalArgumentException.class, () -> new OperationResult<>(null, null));
	}

}
