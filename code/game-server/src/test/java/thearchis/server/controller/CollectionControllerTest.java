package thearchis.server.controller;

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

import thearchis.common.card.Card;
import thearchis.common.card.CardRarity;
import thearchis.common.card.CardType;
import thearchis.common.card.MasterSet;
import thearchis.common.card.Spell;
import thearchis.common.game.ClientState;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.hero.HeroName;
import thearchis.common.message.BackNavigationData;
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
class CollectionControllerTest {

	@Mock
	private ClientHandler mockClientHandler;
	@Mock
	private Server mockServer;
	@Mock
	private Player mockPlayer;
	@Mock
	private DBManager mockDbManager;
	@Mock
	private MasterSet mockMasterSet;
	@InjectMocks
	private CollectionController collectionController;

	private MockedStatic<Server> mockedServer;
	private MockedStatic<MasterSet> mockedMasterSet;

	@BeforeEach
	void initStaticMock() {
		mockedServer = mockStatic(Server.class);
		mockedServer.when(Server::getInstance).thenReturn(mockServer);
		mockedMasterSet = mockStatic(MasterSet.class);
		mockedMasterSet.when(MasterSet::getInstance).thenReturn(mockMasterSet);
	}

	@AfterEach
	void closeStaticMock() {
		mockedServer.close();
		mockedMasterSet.close();
	}

	private void setupAuthenticatedUser() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("AuthenticatedUser");
		when(mockServer.isUserAuthenticated("AuthenticatedUser")).thenReturn(true);
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per player null")
	void testHandleCollectionRequestNullPlayer() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(null);

		Message message = new Message(MessageType.COLLECTION_REQUEST, new CollectionData());

		collectionController.handleCollectionRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per utente non autenticato")
	void testHandleCollectionRequestUserNotAuthenticated() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("NotAuthenticatedUser");
		when(mockServer.isUserAuthenticated("NotAuthenticatedUser")).thenReturn(false);

		Message message = new Message(MessageType.COLLECTION_REQUEST, new CollectionData());

		collectionController.handleCollectionRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per messaggio null")
	void testHandleCollectionRequestNullMessage() {
		setupAuthenticatedUser();

		collectionController.handleCollectionRequest(mockClientHandler, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per payload null")
	void testHandleCollectionRequestNullPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.COLLECTION_REQUEST, null);

		collectionController.handleCollectionRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per payload di tipo errato")
	void testHandleCollectionRequestBadPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.COLLECTION_REQUEST, new BackNavigationData(ClientState.MAIN_MENU));

		collectionController.handleCollectionRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta COLLECTION_REQUEST per contenuto payload errato (cardId != 0)")
	void testHandleCollectionRequestWrongPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.COLLECTION_REQUEST, new CollectionData(1));

		collectionController.handleCollectionRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Accetta COLLECTION_REQUEST e aggiorna stato client a IN_COLLECTION")
	void testHandleCollectionRequestSuccess() {
		setupAuthenticatedUser();
		when(mockServer.getDBManager()).thenReturn(mockDbManager);

		Collection fakeCollection = new Collection();
		when(mockDbManager.getUserCollection("AuthenticatedUser"))
				.thenReturn(new OperationResult<>(OperationStatus.COLLECTION_OK, fakeCollection));

		Message message = new Message(MessageType.COLLECTION_REQUEST, new CollectionData());

		collectionController.handleCollectionRequest(mockClientHandler, message);

		verify(mockClientHandler).setClientState(ClientState.IN_COLLECTION);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.COLLECTION_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.COLLECTION_OK, result.getStatus());
		assertEquals(fakeCollection, result.getData());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per player null")
	void testHandleCraftRequestNullPlayer() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(null);

		Message message = new Message(MessageType.CRAFT_REQUEST, new CollectionData(1));

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per utente non autenticato")
	void testHandleCraftRequestUserNotAuthenticated() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("NotAuthenticatedUser");
		when(mockServer.isUserAuthenticated("NotAuthenticatedUser")).thenReturn(false);

		Message message = new Message(MessageType.CRAFT_REQUEST, new CollectionData(1));

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per messaggio null")
	void testHandleCraftRequestNullMessage() {
		setupAuthenticatedUser();

		collectionController.handleCraftRequest(mockClientHandler, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per payload null")
	void testHandleCraftRequestNullPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.CRAFT_REQUEST, null);

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per payload di tipo errato")
	void testHandleCraftRequestBadPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.CRAFT_REQUEST, new BackNavigationData(ClientState.MAIN_MENU));

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per carta non trovata (card null)")
	void testHandleCraftRequestBadCard() {
		setupAuthenticatedUser();
		when(mockMasterSet.getCardById(1)).thenReturn(null);

		Message message = new Message(MessageType.CRAFT_REQUEST, new CollectionData(1));

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta CRAFT_REQUEST per carta vincolata")
	void testHandleCraftRequestConstrainedCard() {
		setupAuthenticatedUser();
		Card constrainedCard = new Spell(1, "constrainedCard", 1, CardType.SPELL, CardRarity.COMMON,
				HeroName.AETHELGARD, "description", true);
		when(mockMasterSet.getCardById(1)).thenReturn(constrainedCard);

		Message message = new Message(MessageType.CRAFT_REQUEST, new CollectionData(1));

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Accetta CRAFT_REQUEST")
	void testHandleCraftRequestSuccess() {
		setupAuthenticatedUser();
		when(mockServer.getDBManager()).thenReturn(mockDbManager);

		Card validCard = new Spell(101, "Fireball", 3, CardType.SPELL, CardRarity.RARE, HeroName.AETHELGARD,
				"Deal damage", false);
		when(mockMasterSet.getCardById(101)).thenReturn(validCard);

		Player updatedPlayer = new Player("AuthenticatedUser", 100, 200, new Collection(), null, 0, 0);
		when(mockDbManager.craftCard("AuthenticatedUser", 101, validCard.getCraftCost()))
				.thenReturn(new OperationResult<>(OperationStatus.CRAFT_OK, updatedPlayer));

		Message message = new Message(MessageType.CRAFT_REQUEST, new CollectionData(101));

		collectionController.handleCraftRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.CRAFT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.CRAFT_OK, result.getStatus());
		assertEquals(updatedPlayer, result.getData());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per player null")
	void testHandleDisenchantRequestNullPlayer() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(null);

		Message message = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(1));

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per utente non autenticato")
	void testHandleDisenchantRequestUserNotAuthenticated() {
		lenient().when(mockClientHandler.getClientId()).thenReturn(1);
		when(mockClientHandler.getPlayer()).thenReturn(mockPlayer);
		when(mockPlayer.getNickname()).thenReturn("NotAuthenticatedUser");
		when(mockServer.isUserAuthenticated("NotAuthenticatedUser")).thenReturn(false);

		Message message = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(1));

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per messaggio null")
	void testHandleDisenchantRequestNullMessage() {
		setupAuthenticatedUser();

		collectionController.handleDisenchantRequest(mockClientHandler, null);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per payload null")
	void testHandleDisenchantRequestNullPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.DISENCHANT_REQUEST, null);

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per payload di tipo errato")
	void testHandleDisenchantRequestBadPayload() {
		setupAuthenticatedUser();

		Message message = new Message(MessageType.DISENCHANT_REQUEST, new BackNavigationData(ClientState.MAIN_MENU));

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per carta non trovata (card null)")
	void testHandleDisenchantRequestBadCard() {
		setupAuthenticatedUser();
		when(mockMasterSet.getCardById(1)).thenReturn(null);

		Message message = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(1));

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Rifiuta DISENCHANT_REQUEST per carta vincolata")
	void testHandleDisenchantRequestConstrainedCard() {
		setupAuthenticatedUser();
		Card constrainedCard = new Spell(1, "constrainedCard", 1, CardType.SPELL, CardRarity.COMMON,
				HeroName.AETHELGARD, "description", true);
		when(mockMasterSet.getCardById(1)).thenReturn(constrainedCard);

		Message message = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(1));

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
	}

	@Test
	@DisplayName("Accetta DISENCHANT_REQUEST")
	void testHandleDisenchantRequestSuccess() {
		setupAuthenticatedUser();
		when(mockServer.getDBManager()).thenReturn(mockDbManager);

		Card validCard = new Spell(101, "Fireball", 3, CardType.SPELL, CardRarity.RARE, HeroName.AETHELGARD,
				"Deal damage", false);
		when(mockMasterSet.getCardById(101)).thenReturn(validCard);

		Player updatedPlayer = new Player("AuthenticatedUser", 100, 300, new Collection(), null, 0, 0);
		when(mockDbManager.disenchantCard("AuthenticatedUser", 101, validCard.getDisenchantValue()))
				.thenReturn(new OperationResult<>(OperationStatus.DISENCHANT_OK, updatedPlayer));

		Message message = new Message(MessageType.DISENCHANT_REQUEST, new CollectionData(101));

		collectionController.handleDisenchantRequest(mockClientHandler, message);

		ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
		verify(mockClientHandler).sendMessage(captor.capture());

		Message response = captor.getValue();
		assertEquals(MessageType.DISENCHANT_RESPONSE, response.getType());

		OperationResult<?> result = (OperationResult<?>) response.getData();
		assertEquals(OperationStatus.DISENCHANT_OK, result.getStatus());
		assertEquals(updatedPlayer, result.getData());
	}

}