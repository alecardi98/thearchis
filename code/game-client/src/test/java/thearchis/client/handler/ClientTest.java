package thearchis.client.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import thearchis.common.game.state.ClientState;
import thearchis.common.message.AuthenticationData;
import thearchis.common.message.BackNavigationData;
import thearchis.common.message.CollectionData;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;

class ClientTest {

	private Client client;

	@BeforeEach
	void setUp() {
		client = new Client();
	}

	@Test
	@DisplayName("sendMessage con socket disconnesso gestisce l'assenza senza lanciare eccezioni")
	void testSendMessageNotConnected() {
		assertDoesNotThrow(() -> client.sendMessage("TestMessage"));
	}

	@Test
	@DisplayName("Tutti i metodi sendRequest non sollevano eccezioni anche senza connessione attiva")
	void testSendRequestsNotConnected() {
		assertDoesNotThrow(() -> {
			client.sendRegisterRequest("user", "pass");
			client.sendLoginRequest("user", "pass");
			client.sendCollectionRequest();
			client.sendCraftRequest(1);
			client.sendDisenchantRequest(1);
			client.sendBackNavigationRequest(ClientState.MAIN_MENU);
		});
	}

	@Test
	@DisplayName("serverDisconnection: chiamate successive non sollevano eccezioni")
	void testServerDisconnectionManyCalls() {
		assertDoesNotThrow(() -> client.serverDisconnection());
		assertDoesNotThrow(() -> client.serverDisconnection());
	}

	@Test
	@DisplayName("sendRegisterRequest con credenziali null/vuote non chiama sendMessage")
	void testSendRegisterRequestsBadCredentials() {
		Client spyClient = spy(new Client());

		spyClient.sendRegisterRequest(null, "psw");
		spyClient.sendRegisterRequest("user", null);
		spyClient.sendRegisterRequest(null, null);
		spyClient.sendRegisterRequest("", "psw");
		spyClient.sendRegisterRequest("user", "");
		spyClient.sendRegisterRequest("", "");

		verify(spyClient, never()).sendMessage(any());
	}

	@Test
	@DisplayName("sendRegisterRequest con credenziali valide costruisce il Message REGISTER_REQUEST corretto")
	void testSendRegisterRequestSuccess() {
		Client spyClient = spy(new Client());
		ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);

		spyClient.sendRegisterRequest("user", "psw");

		verify(spyClient).sendMessage(msgCaptor.capture());
		assertInstanceOf(Message.class, msgCaptor.getValue());

		Message sent = (Message) msgCaptor.getValue();
		assertEquals(MessageType.REGISTER_REQUEST, sent.getType());
		assertInstanceOf(AuthenticationData.class, sent.getData());
	}

	@Test
	@DisplayName("sendLoginRequest con credenziali null/vuote non chiama sendMessage")
	void testSendLoginRequestsBadCredentials() {
		Client spyClient = spy(new Client());

		spyClient.sendLoginRequest(null, "psw");
		spyClient.sendLoginRequest("user", null);
		spyClient.sendLoginRequest(null, null);
		spyClient.sendLoginRequest("", "psw");
		spyClient.sendLoginRequest("user", "");
		spyClient.sendLoginRequest("", "");

		verify(spyClient, never()).sendMessage(any());
	}

	@Test
	@DisplayName("sendLoginRequest con credenziali valide costruisce il Message LOGIN_REQUEST corretto")
	void testSendLoginRequestSuccess() {
		Client spyClient = spy(new Client());
		ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);

		spyClient.sendLoginRequest("user", "psw");

		verify(spyClient).sendMessage(msgCaptor.capture());
		assertInstanceOf(Message.class, msgCaptor.getValue());

		Message sent = (Message) msgCaptor.getValue();
		assertEquals(MessageType.LOGIN_REQUEST, sent.getType());
		assertInstanceOf(AuthenticationData.class, sent.getData());
	}

	@Test
	@DisplayName("sendCraftRequest con cardId <= 0 non chiama sendMessage")
	void testSendCraftRequestsInvalidCardId() {
		Client spyClient = spy(new Client());

		spyClient.sendCraftRequest(0);
		spyClient.sendCraftRequest(-1);

		verify(spyClient, never()).sendMessage(any());
	}

	@Test
	@DisplayName("sendCraftRequest con cardId valido costruisce il Message CRAFT_REQUEST corretto")
	void testSendCraftRequestSuccess() {
		Client spyClient = spy(new Client());
		ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);

		spyClient.sendCraftRequest(42);

		verify(spyClient).sendMessage(msgCaptor.capture());
		assertInstanceOf(Message.class, msgCaptor.getValue());

		Message sent = (Message) msgCaptor.getValue();
		assertEquals(MessageType.CRAFT_REQUEST, sent.getType());
		assertInstanceOf(CollectionData.class, sent.getData());
	}

	@Test
	@DisplayName("sendDisenchantRequest con cardId <= 0 non chiama sendMessage")
	void testSendDisenchantRequestsInvalidCardId() {
		Client spyClient = spy(new Client());

		spyClient.sendDisenchantRequest(0);
		spyClient.sendDisenchantRequest(-1);

		verify(spyClient, never()).sendMessage(any());
	}

	@Test
	@DisplayName("sendDisenchantRequest con cardId valido costruisce il Message DISENCHANT_REQUEST corretto")
	void testSendDisenchantRequestSucces() {
		Client spyClient = spy(new Client());
		ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);

		spyClient.sendDisenchantRequest(42);

		verify(spyClient).sendMessage(msgCaptor.capture());
		assertInstanceOf(Message.class, msgCaptor.getValue());

		Message sent = (Message) msgCaptor.getValue();
		assertEquals(MessageType.DISENCHANT_REQUEST, sent.getType());
		assertInstanceOf(CollectionData.class, sent.getData());
	}

	@Test
	@DisplayName("sendBackNavigatioRequests con destination null non chiama sendMessage")
	void testSendBackNavigatioRequestsNullDestination() {
		Client spyClient = spy(new Client());

		spyClient.sendBackNavigationRequest(null);

		verify(spyClient, never()).sendMessage(any());
	}

	@Test
	@DisplayName("sendBackNavigationRequest con destination valida costruisce il Message BACKNAVIGATION_REQUEST corretto")
	void testSendBackNavigationRequestSucces() {
		Client spyClient = spy(new Client());
		ArgumentCaptor<Object> msgCaptor = ArgumentCaptor.forClass(Object.class);

		spyClient.sendBackNavigationRequest(ClientState.MAIN_MENU);

		verify(spyClient).sendMessage(msgCaptor.capture());
		assertInstanceOf(Message.class, msgCaptor.getValue());

		Message sent = (Message) msgCaptor.getValue();
		assertEquals(MessageType.BACKNAVIGATION_REQUEST, sent.getType());
		assertInstanceOf(BackNavigationData.class, sent.getData());
	}

}