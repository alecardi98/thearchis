package thearchis.server.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import thearchis.common.game.Player;
import thearchis.server.db.DBManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerTest {

	@Mock
	private DBManager mockDbManager;
	@Mock
	private ServerSocket mockServerSocket;
	@Mock
	private Socket mockSocket;
	@Mock
	private ExecutorService mockThreadPool;

	private Server server;

	@BeforeEach
	void setUp() throws Exception {
		resetSingleton();
		server = Server.getInstance();
	}

	private void resetSingleton() throws Exception {
		Field instanceField = Server.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, null);
	}

	private void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}

	@SuppressWarnings("unchecked")
	private <T> T getField(Object target, String fieldName) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(target);
	}

	private void invokePrivateMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args)
			throws Exception {
		Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
		method.setAccessible(true);
		method.invoke(target, args);
	}

	@Test
	@DisplayName("getInstance: restituisce sempre la stessa istanza Singleton")
	void testGetInstance() {
		Server first = Server.getInstance();
		Server second = Server.getInstance();

		assertNotNull(first);
		assertSame(first, second);
	}

	@Test
	@DisplayName("run: connette il DB, accetta connessioni client e arresta il server su SocketException")
	void testRun() throws Exception {
		setField(server, "clientThreadPool", mockThreadPool);

		when(mockSocket.getInetAddress()).thenReturn(InetAddress.getLoopbackAddress());
		when(mockServerSocket.accept()).thenReturn(mockSocket).thenAnswer(invocation -> {
			invokePrivateMethod(server, "stopServer", new Class<?>[] {});
			throw new SocketException("Socket closed");
		});
		when(mockServerSocket.isClosed()).thenReturn(true);

		try (MockedConstruction<DBManager> dbConstruction = mockConstruction(DBManager.class);
				MockedConstruction<ServerSocket> socketConstruction = mockConstruction(ServerSocket.class,
						(mock, context) -> {
							when(mock.accept()).thenAnswer(inv -> mockServerSocket.accept());
							when(mock.isClosed()).thenAnswer(inv -> mockServerSocket.isClosed());
						})) {

			server.run();

			assertEquals(1, dbConstruction.constructed().size());
			DBManager constructedDb = dbConstruction.constructed().get(0);
			verify(constructedDb).dbConnect();
			verify(constructedDb).dbClose();

			verify(mockThreadPool).submit(any(Runnable.class));
			verify(mockThreadPool).shutdown();
		}
	}

	@Test
	@DisplayName("initializeClient: inizializza ClientHandler, lo registra in activeClients e lo sottomette al thread pool")
	void testInitializeClientSuccess() throws Exception {
		setField(server, "clientThreadPool", mockThreadPool);

		try (MockedConstruction<ClientHandler> handlerConstruction = mockConstruction(ClientHandler.class)) {
			invokePrivateMethod(server, "initializeClient", new Class<?>[] { Socket.class }, mockSocket);

			assertEquals(1, handlerConstruction.constructed().size());
			ClientHandler createdHandler = handlerConstruction.constructed().get(0);

			verify(createdHandler).init();
			verify(mockThreadPool).submit(createdHandler);

			ConcurrentMap<Integer, ClientHandler> activeClients = getField(server, "activeClients");
			assertEquals(1, activeClients.size());
			assertSame(createdHandler, activeClients.get(1));

			AtomicInteger idGen = getField(server, "idGenerator");
			assertEquals(2, idGen.get());
		}
	}

	@Test
	@DisplayName("removeClient: rimuove client attivo e relativa autenticazione se presente")
	void testRemoveClientAuthenticated() throws Exception {
		ConcurrentMap<Integer, ClientHandler> activeClients = getField(server, "activeClients");
		ClientHandler mockHandler = mock(ClientHandler.class);
		Player mockPlayer = mock(Player.class);

		when(mockHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("HeroPlayer");

		activeClients.put(10, mockHandler);
		server.registerLoggedUser("HeroPlayer", 10);

		assertTrue(server.isUserAuthenticated("HeroPlayer"));

		server.removeClient(10);

		assertFalse(activeClients.containsKey(10));
		assertFalse(server.isUserAuthenticated("HeroPlayer"));
	}

	@Test
	@DisplayName("removeClient: rimuove client non autenticato (player null) senza toccare authenticatedUsers")
	void testRemoveClientNotAuthenticated() throws Exception {
		ConcurrentMap<Integer, ClientHandler> activeClients = getField(server, "activeClients");
		ClientHandler mockHandler = mock(ClientHandler.class);

		when(mockHandler.getPlayer()).thenReturn(null);

		activeClients.put(20, mockHandler);
		server.registerLoggedUser("OtherPlayer", 99);

		server.removeClient(20);

		assertFalse(activeClients.containsKey(20));
		assertTrue(server.isUserAuthenticated("OtherPlayer"));
	}

	@Test
	@DisplayName("removeClient: gestisce senza errori la rimozione di un clientId inesistente")
	void testRemoveClientUnknownId() {
		assertDoesNotThrow(() -> server.removeClient(999));
	}

	@Test
	@DisplayName("stopServer: pulisce client attivi, chiude ServerSocket, arresta thread pool e chiude DB")
	void testStopServer() throws Exception {
		setField(server, "dbManager", mockDbManager);
		setField(server, "serverSocket", mockServerSocket);
		setField(server, "clientThreadPool", mockThreadPool);

		ConcurrentMap<Integer, ClientHandler> activeClients = getField(server, "activeClients");
		ClientHandler handler1 = mock(ClientHandler.class);
		ClientHandler handler2 = mock(ClientHandler.class);
		activeClients.put(1, handler1);
		activeClients.put(2, handler2);

		when(mockServerSocket.isClosed()).thenReturn(false);

		invokePrivateMethod(server, "stopServer", new Class<?>[] {});

		verify(handler1).cleanUp();
		verify(handler2).cleanUp();
		assertTrue(activeClients.isEmpty());
		verify(mockServerSocket).close();
		verify(mockThreadPool).shutdown();
		verify(mockDbManager).dbClose();

		boolean running = getField(server, "running");
		assertFalse(running);
	}

	@Test
	@DisplayName("stopServer: è idempotente se chiamato quando running è già false")
	void testStopServerManyCalls() throws Exception {
		setField(server, "running", false);
		setField(server, "dbManager", mockDbManager);
		setField(server, "clientThreadPool", mockThreadPool);

		invokePrivateMethod(server, "stopServer", new Class<?>[] {});

		verify(mockDbManager, never()).dbClose();
		verify(mockThreadPool, never()).shutdown();
	}

	@Test
	@DisplayName("registerLoggedUser / isUserAuthenticated / getAuthenticatedUsers: gestiscono correttamente gli utenti autenticati")
	void testAuthenticatedUsersManagement() {
		assertFalse(server.isUserAuthenticated("PlayerOne"));

		server.registerLoggedUser("PlayerOne", 5);

		assertTrue(server.isUserAuthenticated("PlayerOne"));
		assertEquals(5, server.getAuthenticatedUsers().get("PlayerOne"));
	}

}