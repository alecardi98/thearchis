package thearchis.server.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.card.Card;
import thearchis.common.card.CardRarity;
import thearchis.common.card.CardType;
import thearchis.common.card.MasterSet;
import thearchis.common.card.Spell;
import thearchis.common.game.ClientState;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.hero.HeroName;
import thearchis.common.message.AuthenticationData;
import thearchis.common.message.BackNavigationData;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;
import thearchis.server.db.DBManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageDispatcherTest {

	@Mock
	private ClientHandler mockClientHandler;

	private MessageDispatcher messageDispatcher;

	@BeforeEach
	void setUp() {
		messageDispatcher = MessageDispatcher.getInstance();
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
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

		Message loginMsg = new Message(MessageType.LOGIN_REQUEST, null);
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

		Message disenchantMsg = new Message(MessageType.BACKNAVIGATION_REQUEST, new CollectionData(2));
		messageDispatcher.dispatch(mockClientHandler, disenchantMsg);

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
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			Server mockServer = mock(Server.class);
			DBManager mockDbManager = mock(DBManager.class);
			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockServer.getDBManager()).thenReturn(mockDbManager);

			when(mockDbManager.registerUser("NewUser", "Pass123"))
					.thenReturn(new OperationResult<>(OperationStatus.REGISTER_OK, null));

			when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

			AuthenticationData data = new AuthenticationData("NewUser", "Pass123");
			Message validRegisterMsg = new Message(MessageType.REGISTER_REQUEST, data);

			messageDispatcher.dispatch(mockClientHandler, validRegisterMsg);

			verify(mockDbManager).registerUser("NewUser", "Pass123");
		}
	}

	@Test
	@DisplayName("Inoltra LOGIN_REQUEST ad AuthenticationController")
	void testDispatchValidLoginRequest() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			Server mockServer = mock(Server.class);
			DBManager mockDbManager = mock(DBManager.class);
			Player fakePlayer = new Player("ValidUser", 100, 50, new Collection(), null, 0, 0);

			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockServer.getDBManager()).thenReturn(mockDbManager);
			when(mockServer.isUserAuthenticated("ValidUser")).thenReturn(false);
			when(mockDbManager.loginUser("ValidUser", "Pass123"))
					.thenReturn(new OperationResult<>(OperationStatus.LOGIN_OK, fakePlayer));

			when(mockClientHandler.getClientState()).thenReturn(ClientState.NOT_AUTHENTICATED);

			AuthenticationData data = new AuthenticationData("ValidUser", "Pass123");
			Message validLoginMsg = new Message(MessageType.LOGIN_REQUEST, data);

			messageDispatcher.dispatch(mockClientHandler, validLoginMsg);

			verify(mockClientHandler).setPlayer(fakePlayer);
			verify(mockClientHandler).setClientState(ClientState.MAIN_MENU);
		}
	}

	@Test
	@DisplayName("Inoltra COLLECTION_REQUEST a CollectionController")
	void testDispatchValidCollectionRequest() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			Server mockServer = mock(Server.class);
			DBManager mockDbManager = mock(DBManager.class);
			Player mockPlayer = mock(Player.class);

			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockServer.getDBManager()).thenReturn(mockDbManager);
			when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
			when(mockPlayer.getNickname()).thenReturn("AuthUser");
			when(mockServer.isUserAuthenticated("AuthUser")).thenReturn(true);

			when(mockDbManager.getUserCollection("AuthUser"))
					.thenReturn(new OperationResult<>(OperationStatus.COLLECTION_OK, new Collection()));

			when(mockClientHandler.getClientState()).thenReturn(ClientState.MAIN_MENU);

			Message validCollectionMsg = new Message(MessageType.COLLECTION_REQUEST, new CollectionData());

			messageDispatcher.dispatch(mockClientHandler, validCollectionMsg);

			verify(mockDbManager).getUserCollection("AuthUser");
			verify(mockClientHandler).setClientState(ClientState.IN_COLLECTION);
		}
	}

	@Test
	@DisplayName("Inoltra CRAFT_REQUEST a CollectionController")
	void testDispatchValidCraftRequest() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class);
				MockedStatic<MasterSet> mockedMasterSet = mockStatic(MasterSet.class)) {

			Server mockServer = mock(Server.class);
			DBManager mockDbManager = mock(DBManager.class);
			MasterSet mockMasterSet = mock(MasterSet.class);
			Player mockPlayer = mock(Player.class);

			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			mockedMasterSet.when(MasterSet::getInstance).thenReturn(mockMasterSet);

			when(mockServer.getDBManager()).thenReturn(mockDbManager);
			when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
			when(mockPlayer.getNickname()).thenReturn("AuthUser");
			when(mockServer.isUserAuthenticated("AuthUser")).thenReturn(true);

			Card validCard = new Spell(101, "Fireball", 3, CardType.SPELL, CardRarity.RARE, HeroName.AETHELGARD,
					"Deal damage", false);
			when(mockMasterSet.getCardById(101)).thenReturn(validCard);

			Player updatedPlayer = new Player("AuthUser", 100, 200, new Collection(), null, 0, 0);
			when(mockDbManager.craftCard("AuthUser", 101, validCard.getCraftCost()))
					.thenReturn(new OperationResult<>(OperationStatus.CRAFT_OK, updatedPlayer));

			when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

			Message validCraftMsg = new Message(MessageType.CRAFT_REQUEST, new CollectionData(101));

			messageDispatcher.dispatch(mockClientHandler, validCraftMsg);

			verify(mockDbManager).craftCard("AuthUser", 101, validCard.getCraftCost());
		}
	}

	@Test
	@DisplayName("Inoltra DISENCHANT_REQUEST a CollectionController")
	void testDispatchValidDisenchantRequest() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class);
				MockedStatic<MasterSet> mockedMasterSet = mockStatic(MasterSet.class)) {

			Server mockServer = mock(Server.class);
			DBManager mockDbManager = mock(DBManager.class);
			MasterSet mockMasterSet = mock(MasterSet.class);
			Player mockPlayer = mock(Player.class);

			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			mockedMasterSet.when(MasterSet::getInstance).thenReturn(mockMasterSet);

			when(mockServer.getDBManager()).thenReturn(mockDbManager);
			when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
			when(mockPlayer.getNickname()).thenReturn("AuthUser");
			when(mockServer.isUserAuthenticated("AuthUser")).thenReturn(true);

			Card validCard = new Spell(101, "Fireball", 3, CardType.SPELL, CardRarity.RARE, HeroName.AETHELGARD,
					"Deal damage", false);
			when(mockMasterSet.getCardById(101)).thenReturn(validCard);

			Player updatedPlayer = new Player("AuthUser", 100, 300, new Collection(), null, 0, 0);
			when(mockDbManager.disenchantCard("AuthUser", 101, validCard.getDisenchantValue()))
					.thenReturn(new OperationResult<>(OperationStatus.DISENCHANT_OK, updatedPlayer));

			when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

			Message validDisenchantMsg = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(101));

			messageDispatcher.dispatch(mockClientHandler, validDisenchantMsg);

			verify(mockDbManager).disenchantCard("AuthUser", 101, validCard.getDisenchantValue());
		}
	}

	@Test
	@DisplayName("Inoltra BACKNAVIGATION_REQUEST a BackNavigationController")
	void testDispatchValidBackNavigationRequest() {
		try (MockedStatic<Server> mockedServer = mockStatic(Server.class)) {
			Server mockServer = mock(Server.class);
			Player mockPlayer = mock(Player.class);

			mockedServer.when(Server::getInstance).thenReturn(mockServer);
			when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
			when(mockPlayer.getNickname()).thenReturn("AuthUser");
			when(mockServer.isUserAuthenticated("AuthUser")).thenReturn(true);

			when(mockClientHandler.getClientState()).thenReturn(ClientState.IN_COLLECTION);

			Message validBackMsg = new Message(MessageType.BACKNAVIGATION_REQUEST,
					new BackNavigationData(ClientState.MAIN_MENU));

			messageDispatcher.dispatch(mockClientHandler, validBackMsg);

			verify(mockClientHandler).setClientState(ClientState.MAIN_MENU);
		}
	}

}