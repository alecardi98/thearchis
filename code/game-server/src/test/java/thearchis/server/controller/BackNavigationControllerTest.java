package thearchis.server.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
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
import thearchis.common.game.Player;
import thearchis.common.message.BackNavigationData;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.handler.ClientHandler;
import thearchis.server.handler.Server;

@ExtendWith(MockitoExtension.class)
class BackNavigationControllerTest {

	@Mock
	private ClientHandler clientHandler;
	@Mock
	private Player mockPlayer;
	@Mock
	private Server mockServer;
	@InjectMocks
	private BackNavigationController backNavigationController;

	private MockedStatic<Server> mockedServer;

	@BeforeEach
	void initStaticMock() {
		mockedServer = mockStatic(Server.class);
		mockedServer.when(Server::getInstance).thenReturn(mockServer);
	}

	@AfterEach
	void closeStaticMock() {
		mockedServer.close();
	}

	private void setupAuthenticatedUser() {
		lenient().when(clientHandler.getClientId()).thenReturn(1);
		when(clientHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("AuthenticatedUser");
		when(mockServer.isUserAuthenticated("AuthenticatedUser")).thenReturn(true);
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per player null")
	void testHandleBackNavigationNullPlayer() {
		when(clientHandler.getClientId()).thenReturn(1);
		when(clientHandler.getPlayer()).thenReturn(null);

		Message message = new Message(MessageType.BACKNAVIGATION_REQUEST,
				new BackNavigationData(ClientState.MAIN_MENU));
		backNavigationController.handleBackNavigationRequest(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per utente non autenticato")
	void testHandleBackNavigationUserNotAuthenticated() {
		when(clientHandler.getClientId()).thenReturn(1);
		when(clientHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("NotAuthenticatedUser");
		when(mockServer.isUserAuthenticated("NotAuthenticatedUser")).thenReturn(false);

		Message message = new Message(MessageType.BACKNAVIGATION_REQUEST,
				new BackNavigationData(ClientState.MAIN_MENU));
		backNavigationController.handleBackNavigationRequest(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per messaggio null")
	void testHandleBackNavigationNullMessage() {
		setupAuthenticatedUser();

		backNavigationController.handleBackNavigationRequest(clientHandler, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per payload null")
	void testHandleBackNavigationNullPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.BACKNAVIGATION_REQUEST, null);
		backNavigationController.handleBackNavigationRequest(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per payload errato")
	void testHandleBackNavigationBadPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.BACKNAVIGATION_REQUEST, new CollectionData());
		backNavigationController.handleBackNavigationRequest(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per transizione non valida")
	void testHandleBackNavigationInvalidTransition() {
		setupAuthenticatedUser();
		when(clientHandler.getClientState()).thenReturn(ClientState.MAIN_MENU);

		Message message = new Message(MessageType.BACKNAVIGATION_REQUEST,
				new BackNavigationData(ClientState.MAIN_MENU));
		backNavigationController.handleBackNavigationRequest(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Accetta BACKNAVIGATION_REQUEST per transizione valida")
	void testHandleBackNavigationValidTransition() {
		setupAuthenticatedUser();
		when(clientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

		ClientState destination = ClientState.MAIN_MENU;
		Message message = new Message(MessageType.BACKNAVIGATION_REQUEST, new BackNavigationData(destination));
		backNavigationController.handleBackNavigationRequest(clientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(clientHandler).setClientState(destination);
		verify(clientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BACKNAVIGATION_OK, result.getStatus());
	}

}