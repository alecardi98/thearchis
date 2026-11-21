package thearchis.server.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.game.ClientState;
import thearchis.common.message.AuthenticationData;
import thearchis.common.message.BackNavigationData;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.controller.AuthenticationController;
import thearchis.server.controller.BackNavigationController;
import thearchis.server.controller.CollectionController;
import thearchis.server.controller.DeckBuilderController;
import thearchis.server.controller.GameController;
import thearchis.server.controller.ShopController;
import thearchis.server.db.MongoDBManager;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageDispatcherTest {

	@Mock
	private ClientHandler mockClientHandler;
	@Mock
	private Server mockServer;
	@Mock
	private MongoDBManager mockDbManager;

	private MockedStatic<Server> mockedServer;
	private MockedConstruction<AuthenticationController> mockedAuthController;
	private MockedConstruction<CollectionController> mockedCollectionController;
	private MockedConstruction<BackNavigationController> mockedBackController;
	private MockedConstruction<DeckBuilderController> mockedDeckController;
	private MockedConstruction<ShopController> mockedShopController;
	private MockedConstruction<GameController> mockedGameController;

	private MessageDispatcher messageDispatcher;

	@BeforeEach
	void setUp() throws Exception {
		Field instanceField = MessageDispatcher.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, null);

		mockedServer = mockStatic(Server.class);
		mockedServer.when(Server::getInstance).thenReturn(mockServer);
		lenient().when(mockServer.getMongoDBManager()).thenReturn(mockDbManager);

		mockedAuthController = mockConstruction(AuthenticationController.class);
		mockedCollectionController = mockConstruction(CollectionController.class);
		mockedBackController = mockConstruction(BackNavigationController.class);
		mockedDeckController = mockConstruction(DeckBuilderController.class);
		mockedShopController = mockConstruction(ShopController.class);
		mockedGameController = mockConstruction(GameController.class);

		messageDispatcher = MessageDispatcher.getInstance();
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
	}

	@AfterEach
	void tearDown() {
		mockedServer.close();
		mockedAuthController.close();
		mockedCollectionController.close();
		mockedBackController.close();
		mockedDeckController.close();
		mockedShopController.close();
		mockedGameController.close();
	}

	@Test
	@DisplayName("Rifiuta messaggio con MessageType null")
	void testDispatchInvalidMessage() {
		Message badMsg = mock(Message.class);
		messageDispatcher.dispatch(mockClientHandler, badMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.ERROR_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta REGISTER_REQUEST per utente non NOT_AUTHENTICATED")
	void testDispatchInvalidRegisterRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

		Message registerMsg = new Message(MessageType.REGISTER_REQUEST, null);
		messageDispatcher.dispatch(mockClientHandler, registerMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.REGISTER_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta LOGIN_REQUEST per utente non NOT_AUTHENTICATED")
	void testDispatchInvalidLoginRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.MAIN_MENU);

		AuthenticationData credentials = new AuthenticationData("nickname", "password");
		Message loginMsg = new Message(MessageType.LOGIN_REQUEST, credentials);
		messageDispatcher.dispatch(mockClientHandler, loginMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.LOGIN_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per utente non MAIN_MENU")
	void testDispatchInvalidCollectionRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

		Message collectionMsg = new Message(MessageType.COLLECTION_REQUEST, new CollectionData());
		messageDispatcher.dispatch(mockClientHandler, collectionMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per utente non IN_COLLECTION")
	void testDispatchInvalidCraftRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

		Message craftMsg = new Message(MessageType.CRAFT_REQUEST, new CollectionData(2));
		messageDispatcher.dispatch(mockClientHandler, craftMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per utente non IN_COLLECTION")
	void testDispatchInvalidDisenchantRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

		Message disenchantMsg = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(2));
		messageDispatcher.dispatch(mockClientHandler, disenchantMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta BACKNAVIGATION_REQUEST per utente in stato non valido")
	void testDispatchInvalidBackNavigationRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

		Message backMsg = new Message(MessageType.BACKNAVIGATION_REQUEST, new CollectionData(2));
		messageDispatcher.dispatch(mockClientHandler, backMsg);

		ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler, times(1)).sendMessage(messageCaptor.capture());

		Message response = messageCaptor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Inoltra REGISTER_REQUEST ad AuthenticationController")
	void testDispatchValidRegisterRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

		AuthenticationData data = new AuthenticationData("NewUser", "Pass123");
		Message validRegisterMsg = new Message(MessageType.REGISTER_REQUEST, data);

		messageDispatcher.dispatch(mockClientHandler, validRegisterMsg);

		AuthenticationController authController = mockedAuthController.constructed().get(0);
		verify(authController).handleRegister(mockClientHandler, validRegisterMsg);
	}

	@Test
	@DisplayName("Inoltra LOGIN_REQUEST ad AuthenticationController")
	void testDispatchValidLoginRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

		AuthenticationData data = new AuthenticationData("ValidUser", "Pass123");
		Message validLoginMsg = new Message(MessageType.LOGIN_REQUEST, data);

		messageDispatcher.dispatch(mockClientHandler, validLoginMsg);

		AuthenticationController authController = mockedAuthController.constructed().get(0);
		verify(authController).handleLogin(mockClientHandler, validLoginMsg);
	}

	@Test
	@DisplayName("Inoltra COLLECTION_REQUEST a CollectionController")
	void testDispatchValidCollectionRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.MAIN_MENU);

		Message validCollectionMsg = new Message(MessageType.COLLECTION_REQUEST, new CollectionData());

		messageDispatcher.dispatch(mockClientHandler, validCollectionMsg);

		CollectionController collectionController = mockedCollectionController.constructed().get(0);
		verify(collectionController).handleCollectionRequest(mockClientHandler, validCollectionMsg);
	}

	@Test
	@DisplayName("Inoltra CRAFT_REQUEST a CollectionController")
	void testDispatchValidCraftRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

		Message validCraftMsg = new Message(MessageType.CRAFT_REQUEST, new CollectionData(101));

		messageDispatcher.dispatch(mockClientHandler, validCraftMsg);

		CollectionController collectionController = mockedCollectionController.constructed().get(0);
		verify(collectionController).handleCraftRequest(mockClientHandler, validCraftMsg);
	}

	@Test
	@DisplayName("Inoltra DISENCHANT_REQUEST a CollectionController")
	void testDispatchValidDisenchantRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

		Message validDisenchantMsg = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(101));

		messageDispatcher.dispatch(mockClientHandler, validDisenchantMsg);

		CollectionController collectionController = mockedCollectionController.constructed().get(0);
		verify(collectionController).handleDisenchantRequest(mockClientHandler, validDisenchantMsg);
	}

	@Test
	@DisplayName("Inoltra BACKNAVIGATION_REQUEST a BackNavigationController")
	void testDispatchValidBackNavigationRequest() {
		when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

		Message validBackMsg = new Message(MessageType.BACKNAVIGATION_REQUEST,
				new BackNavigationData(ClientState.MAIN_MENU));

		messageDispatcher.dispatch(mockClientHandler, validBackMsg);

		BackNavigationController backController = mockedBackController.constructed().get(0);
		verify(backController).handleBackNavigationRequest(mockClientHandler, validBackMsg);
	}

}