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
import thearchis.server.db.AuthenticationRepository;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

	@Mock
	private ClientHandler clientHandler;
	@Mock
	private Server mockServer;
	@Mock
	private AuthenticationRepository mockAuthenticationRepository;
	@InjectMocks
	private AuthenticationController authenticationController;

	@BeforeEach
	void setUp() {
		lenient().when(clientHandler.getClientId()).thenReturn(1);
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
		AuthenticationData nullCredentials = mock(AuthenticationData.class);

		Message message = new Message(MessageType.REGISTER_REQUEST, nullCredentials);

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
		AuthenticationData emptyCredentials = mock(AuthenticationData.class);
		when(emptyCredentials.getNickname()).thenReturn("");
		when(emptyCredentials.getPassword()).thenReturn("");

		Message message = new Message(MessageType.REGISTER_REQUEST, emptyCredentials);

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
		when(mockAuthenticationRepository.registerUser("NewUser", "Pass123"))
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
		AuthenticationData nullCredentials = mock(AuthenticationData.class);

		Message message = new Message(MessageType.LOGIN_REQUEST, nullCredentials);

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
		AuthenticationData emptyCredentials = mock(AuthenticationData.class);
		when(emptyCredentials.getNickname()).thenReturn("");
		when(emptyCredentials.getPassword()).thenReturn("");

		Message message = new Message(MessageType.LOGIN_REQUEST, emptyCredentials);

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

			Map<Integer, Integer> cards = new HashMap<>();
			Player fakePlayer = new Player("ValidUser", 100, new Collection(cards, 50), new ArrayList<>(), 0, 0);
			when(mockAuthenticationRepository.loginUser("ValidUser", "Pass123"))
					.thenReturn(new OperationResult<>(OperationStatus.LOGIN_OK, fakePlayer));

			AuthenticationData credentials = new AuthenticationData("ValidUser", "Pass123");
			Message message = new Message(MessageType.LOGIN_REQUEST, credentials);

			authenticationController.handleLogin(clientHandler, message);

			verify(clientHandler).setPlayer(fakePlayer);
			verify(clientHandler).setClientState(ClientState.MAIN_MENU);
			verify(mockServer).registerLoggedUser("ValidUser", 1);

			ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
			verify(clientHandler).sendMessage(captor.capture());

			Message response = captor.getValue();
			assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

			OperationResult<?> result = (OperationResult<?>) response.getData();
			assertEquals(OperationStatus.LOGIN_OK, result.getStatus());
		}
	}

}