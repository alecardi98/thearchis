package thearchis.server.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.game.ClientState;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.message.AuthenticationData;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.db.DBManager;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

	@Mock
	private ClientHandler clientHandler;
	@Mock
	private Server mockServer;
	@Mock
	private DBManager mockDbManager;
	@InjectMocks
	private AuthenticationController authenticationController;

	@BeforeEach
	void setUp() {
		lenient().when(clientHandler.getClientId()).thenReturn(1);
	}

	@Test
	@DisplayName("Rifiuta registrazione per messaggio nullo")
	void testHandleRegisterNullMessage() {
		authenticationController.handleRegister(clientHandler, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta registrazione per payload nullo")
	void testHandleRegisterNullPayload() {
		Message message = new Message(MessageType.REGISTER_REQUEST, null);

		authenticationController.handleRegister(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta registrazione per payload errato")
	void testHandleRegisterBadPayload() {
		Message message = new Message(MessageType.REGISTER_REQUEST, new CollectionData(2));

		authenticationController.handleRegister(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta registrazione per credenziali nulle")
	void testHandleRegisterNullCredentials() {
		Message message = new Message(MessageType.REGISTER_REQUEST, new AuthenticationData(null, null));

		authenticationController.handleRegister(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta registrazione per credenziali vuote")
	void testHandleRegisterEmptyCredentials() {
		Message message = new Message(MessageType.REGISTER_REQUEST, new AuthenticationData("", ""));

		authenticationController.handleRegister(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Registrazione eseguita con successo.")
	void testHandleRegisterSuccess() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockServer.getDBManager()).thenReturn(mockDbManager);
			when(mockDbManager.registerUser("NewUser", "Pass123"))
					.thenReturn(new OperationResult<>(OperationStatus.REGISTER_OK, null));

			AuthenticationData credentials = new AuthenticationData("NewUser", "Pass123");
			Message message = new Message(MessageType.REGISTER_REQUEST, credentials);

			authenticationController.handleRegister(clientHandler, message);

			ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
			verify(clientHandler).sendMessage(captor.capture());

			Message response = captor.getValue();
			assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

			OperationResult<?> result = (OperationResult<?>) response.getData();
			assertEquals(OperationStatus.REGISTER_OK, result.getStatus());
			assertNull(result.getData(), "Il payload di esito della registrazione deve essere null");
		}
	}

	@Test
	@DisplayName("Rifiuta login per messaggio nullo")
	void testHandleLoginNullMessage() {
		authenticationController.handleLogin(clientHandler, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta login per payload nullo")
	void testHandleLoginBadMessage() {
		Message message = new Message(MessageType.LOGIN_REQUEST, null);

		authenticationController.handleLogin(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta login per payload errato")
	void testHandleLoginBadPayload() {
		Message message = new Message(MessageType.LOGIN_REQUEST, new CollectionData(2));

		authenticationController.handleLogin(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta login per credenziali nulle")
	void testHandleLoginNullCredentials() {
		Message message = new Message(MessageType.LOGIN_REQUEST, new AuthenticationData(null, null));

		authenticationController.handleLogin(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta login per credenziali vuote")
	void testHandleLoginEmptyCredentials() {
		Message message = new Message(MessageType.LOGIN_REQUEST, new AuthenticationData("", ""));

		authenticationController.handleLogin(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta login di un utente già loggato")
	void testHandleLoginAlreadyLoggedIn() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockServer.isUserAuthenticated("alreadyLoggedUser")).thenReturn(true);

			AuthenticationData credentials = new AuthenticationData("alreadyLoggedUser", "password123");
			Message message = new Message(MessageType.LOGIN_REQUEST, credentials);

			authenticationController.handleLogin(clientHandler, message);

			ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
			verify(clientHandler).sendMessage(captor.capture());

			Message response = captor.getValue();
			assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

			OperationResult<?> result = (OperationResult<?>) response.getData();
			assertEquals(OperationStatus.ALREADY_LOGGED_IN, result.getStatus());
		}
	}

	@Test
	@DisplayName("Login eseguito con successo.")
	void testHandleLoginSuccess() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockServer.isUserAuthenticated("ValidUser")).thenReturn(false);

			Player fakePlayer = new Player("ValidUser", 100, 50, new Collection(), null, 0, 0);
			when(mockServer.getDBManager()).thenReturn(mockDbManager);
			when(mockDbManager.loginUser("ValidUser", "Pass123"))
					.thenReturn(new OperationResult<>(OperationStatus.LOGIN_OK, fakePlayer));

			AuthenticationData credentials = new AuthenticationData("ValidUser", "Pass123");
			Message message = new Message(MessageType.LOGIN_REQUEST, credentials);

			authenticationController.handleLogin(clientHandler, message);

			verify(clientHandler).setPlayer(fakePlayer);
			verify(clientHandler).setClientState(ClientState.MAIN_MENU);

			ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
			verify(clientHandler).sendMessage(captor.capture());

			Message response = captor.getValue();
			assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

			OperationResult<?> result = (OperationResult<?>) response.getData();
			assertEquals(OperationStatus.LOGIN_OK, result.getStatus());
		}
	}

}