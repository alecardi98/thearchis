package thearchis.server.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.game.state.ClientState;
import thearchis.common.message.Message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientHandlerTest {

	private static final int CLIENT_ID = 1;

	@Mock
	private Socket mockSocket;
	@Mock
	private MessageDispatcher mockDispatcher;
	@Mock
	private Server mockServer;

	private ClientHandler clientHandler;

	@BeforeEach
	void setUp() {
		clientHandler = new ClientHandler(mockSocket, CLIENT_ID);
	}

	/**
	 * Helper per preparare uno stream di oggetti serializzati in memoria da far
	 * leggere a ClientHandler.init().
	 */
	private byte[] serializeObjects(Object... objects) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			for (Object obj : objects) {
				oos.writeObject(obj);
			}
			oos.flush();
		}
		return baos.toByteArray();
	}

	@Test
	@DisplayName("Costruttore: inizializza clientId e stato NOT_AUTHENTICATED")
	void testConstructor() {
		assertEquals(CLIENT_ID, clientHandler.getClientId());
		assertEquals(ClientState.NOT_AUTHENTICATED, clientHandler.getClientState());
		assertNull(clientHandler.getPlayer());
	}

	@Test
	@DisplayName("init: apre correttamente ObjectOutputStream e ObjectInputStream dal socket")
	void testInitSuccess() throws IOException {
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		ByteArrayInputStream inBuffer = new ByteArrayInputStream(serializeObjects());

		when(mockSocket.getOutputStream()).thenReturn(outBuffer);
		when(mockSocket.getInputStream()).thenReturn(inBuffer);

		assertDoesNotThrow(() -> clientHandler.init());
		assertTrue(outBuffer.size() > 0, "L'header di ObjectOutputStream deve essere stato scritto e flushato");
	}

	@Test
	@DisplayName("run: instrada i Message al MessageDispatcher, ignora oggetti non riconosciuti ed esegue cleanUp a fine stream")
	void testRun() throws Exception {
		Message validMsg = mock(Message.class, withSettings().serializable());
		String unknownObj = "OggettoNonValido";

		byte[] serializedStream = serializeObjects(validMsg, unknownObj, null);
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		ByteArrayInputStream inBuffer = new ByteArrayInputStream(serializedStream);

		when(mockSocket.getOutputStream()).thenReturn(outBuffer);
		when(mockSocket.getInputStream()).thenReturn(inBuffer);
		when(mockSocket.isClosed()).thenReturn(false);

		clientHandler.init();

		try (MockedStatic<MessageDispatcher> dispatcherStatic = mockStatic(MessageDispatcher.class);
				MockedStatic<Server> serverStatic = mockStatic(Server.class)) {

			dispatcherStatic.when(MessageDispatcher::getInstance).thenReturn(mockDispatcher);
			serverStatic.when(Server::getInstance).thenReturn(mockServer);

			clientHandler.run();

			verify(mockDispatcher, times(1)).dispatch(eq(clientHandler), any(Message.class));
			verify(mockSocket).close();
			verify(mockServer).removeClient(CLIENT_ID);
		}
	}

	@Test
	@DisplayName("run: gestisce EOFException/IOException da disconnessione improvvisa ed esegue cleanUp")
	void testRunHandlesClientDisconnect() throws Exception {

		byte[] emptyStream = serializeObjects();
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		ByteArrayInputStream inBuffer = new ByteArrayInputStream(emptyStream);

		when(mockSocket.getOutputStream()).thenReturn(outBuffer);
		when(mockSocket.getInputStream()).thenReturn(inBuffer);
		when(mockSocket.isClosed()).thenReturn(false);

		clientHandler.init();

		try (MockedStatic<Server> serverStatic = mockStatic(Server.class)) {
			serverStatic.when(Server::getInstance).thenReturn(mockServer);

			assertDoesNotThrow(() -> clientHandler.run());

			verify(mockSocket).close();
			verify(mockServer).removeClient(CLIENT_ID);
		}
	}

	@Test
	@DisplayName("cleanUp: rimuove il client dal Server anche se gli stream e il socket sono null/già chiusi")
	void testCleanUpNullStreams() throws IOException {
		when(mockSocket.isClosed()).thenReturn(true);

		try (MockedStatic<Server> serverStatic = mockStatic(Server.class)) {
			serverStatic.when(Server::getInstance).thenReturn(mockServer);

			clientHandler.cleanUp();

			verify(mockSocket, never()).close();
			verify(mockServer).removeClient(CLIENT_ID);
		}
	}

	@Test
	@DisplayName("sendMessage: ignora l'invio se msg è null o se out non è inizializzato")
	void testSendMessageNullMessageNullStream() {
		assertDoesNotThrow(() -> clientHandler.sendMessage(null));
		assertDoesNotThrow(() -> clientHandler.sendMessage("Ping"));
	}

	@Test
	@DisplayName("sendMessage: ignora l'invio se msg è null anche con stream inizializzato")
	void testSendMessageNullMessage() throws IOException {
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		ByteArrayInputStream inBuffer = new ByteArrayInputStream(serializeObjects());

		when(mockSocket.getOutputStream()).thenReturn(outBuffer);
		when(mockSocket.getInputStream()).thenReturn(inBuffer);

		clientHandler.init();
		int sizeAfterHeader = outBuffer.size();

		clientHandler.sendMessage(null);

		assertEquals(sizeAfterHeader, outBuffer.size(), "Nessun byte aggiuntivo deve essere scritto se msg è null");
	}

	@Test
	@DisplayName("sendMessage: serializza e invia correttamente un messaggio valido sullo stream")
	void testSendMessageSuccess() throws Exception {
		ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
		ByteArrayInputStream inBuffer = new ByteArrayInputStream(serializeObjects());

		when(mockSocket.getOutputStream()).thenReturn(outBuffer);
		when(mockSocket.getInputStream()).thenReturn(inBuffer);

		clientHandler.init();
		clientHandler.sendMessage("PayloadDiTest");

		try (ObjectInputStream verificationStream = new ObjectInputStream(
				new ByteArrayInputStream(outBuffer.toByteArray()))) {
			Object received = verificationStream.readObject();
			assertEquals("PayloadDiTest", received);
		}
	}

}