package thearchis.client.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thearchis.common.game.ClientState;
import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerListenerTest {

	@Mock
	private Client mockClient;

	private ServerListener serverListener;

	@BeforeEach
	void setUp() throws IOException {
		ObjectInputStream emptyIn = createInputStream();
		serverListener = new ServerListener(emptyIn, mockClient);
		lenient().doAnswer(inv -> {
			serverListener.stopListener();
			return null;
		}).when(mockClient).serverDisconnection();
	}

	private ObjectInputStream createInputStream(Object... objects) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			for (Object obj : objects) {
				oos.writeObject(obj);
			}
			oos.flush();
		}
		return new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
	}

	private Message mockMessage(MessageType type, Object data) {
		Message msg = mock(Message.class, withSettings().serializable());
		lenient().when(msg.getType()).thenReturn(type);
		lenient().when(msg.getData()).thenReturn(data);
		return msg;
	}

	@Test
	@DisplayName("run: processa messaggi validi, ignora oggetti sconosciuti e chiama serverDisconnection su EOF")
	void testRun() throws Exception {
		OperationResult<Void> registerResult = new OperationResult<>(OperationStatus.REGISTER_OK, null);
		Message validMsg = new Message(MessageType.REGISTER_RESPONSE, registerResult);
		String unknownObj = "OggettoNonValido";

		ObjectInputStream in = createInputStream(validMsg, unknownObj);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("run: termina il ciclo senza chiamare serverDisconnection se arrestato volontariamente con stopListener")
	void testRunStopped() throws Exception {
		ObjectInputStream in = createInputStream("PrimoOggetto", null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.stopListener();
		serverListener.run();

		verify(mockClient, never()).serverDisconnection();
	}

	@Test
	@DisplayName("run: cattura IOException senza chiamare serverDisconnection se stopListener è stato invocato")
	void testRunStopListener() throws Exception {
		ObjectInputStream in = createInputStream();
		serverListener = new ServerListener(in, mockClient);

		serverListener.stopListener();
		serverListener.run();

		verify(mockClient, never()).serverDisconnection();
	}

	@Test
	@DisplayName("processMessage: ignora Message null, MessageType null, MessageType non gestito e payload non validi")
	void testProcessMessageInvalidMessages() throws Exception {
		Message nullTypeMsg = mockMessage(null, new OperationResult<>(OperationStatus.REGISTER_OK, null));
		Message unhandledTypeMsg = mockMessage(MessageType.values()[MessageType.values().length - 1],
				new OperationResult<>(OperationStatus.REGISTER_OK, null));
		Message nullPayloadMsg = new Message(MessageType.REGISTER_RESPONSE, null);
		Message wrongPayloadTypeMsg = new Message(MessageType.REGISTER_RESPONSE, "PayloadErrato");

		@SuppressWarnings("unchecked")
		OperationResult<Void> nullStatusResult = mock(OperationResult.class, withSettings().serializable());
		Message nullStatusMsg = new Message(MessageType.REGISTER_RESPONSE, nullStatusResult);

		ObjectInputStream in = createInputStream(nullTypeMsg, nullPayloadMsg, wrongPayloadTypeMsg, nullStatusMsg, null);
		serverListener = new ServerListener(in, mockClient);

		assertDoesNotThrow(() -> serverListener.run());
		verify(mockClient, never()).setPlayer(any());
		verify(mockClient, times(1)).serverDisconnection();

		reset(mockClient);
		Method processMethod = ServerListener.class.getDeclaredMethod("processMessage", Message.class);
		processMethod.setAccessible(true);
		assertDoesNotThrow(() -> processMethod.invoke(serverListener, (Object) null));
		assertDoesNotThrow(() -> processMethod.invoke(serverListener, unhandledTypeMsg));
		verify(mockClient, never()).serverDisconnection();
	}

	@Test
	@DisplayName("handleRegisterResponse: gestisce tutti gli OperationStatus senza disconnessioni premature")
	void testHandleRegisterResponse() throws Exception {
		Message okMsg = new Message(MessageType.REGISTER_RESPONSE,
				new OperationResult<>(OperationStatus.REGISTER_OK, null));
		Message existsMsg = new Message(MessageType.REGISTER_RESPONSE,
				new OperationResult<>(OperationStatus.NICKNAME_ALREADY_EXISTS, null));
		Message badReqMsg = new Message(MessageType.REGISTER_RESPONSE,
				new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		Message dbErrMsg = new Message(MessageType.REGISTER_RESPONSE,
				new OperationResult<>(OperationStatus.DB_ERROR, null));
		Message otherMsg = new Message(MessageType.REGISTER_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, null));

		ObjectInputStream in = createInputStream(okMsg, existsMsg, badReqMsg, dbErrMsg, otherMsg, null);
		serverListener = new ServerListener(in, mockClient);

		assertDoesNotThrow(() -> serverListener.run());
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleLoginResponse: imposta Player su LOGIN_OK valido e gestisce gli altri status")
	void testHandleLoginResponse() throws Exception {
		Player mockPlayer = mock(Player.class, withSettings().serializable());

		Message okMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, mockPlayer));
		Message invalidCredMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.INVALID_CREDENTIALS, null));
		Message alreadyLoggedMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.ALREADY_LOGGED_IN, null));
		Message badReqMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		Message dbErrMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.DB_ERROR, null));
		Message unknownStatusMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.REGISTER_OK, null));

		ObjectInputStream in = createInputStream(okMsg, invalidCredMsg, alreadyLoggedMsg, badReqMsg, dbErrMsg,
				unknownStatusMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, times(1)).setPlayer(any(Player.class));
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleLoginResponse: disconnette il client se LOGIN_OK ha payload Player mancante")
	void testHandleLoginResponseInvalidPayload() throws Exception {
		Message invalidPayloadMsg = new Message(MessageType.LOGIN_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, null));

		ObjectInputStream in = createInputStream(invalidPayloadMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, never()).setPlayer(any());
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleCollectionResponse: aggiorna la collezione del Player su COLLECTION_OK e gestisce gli errori")
	void testHandleCollectionResponse() throws Exception {
		Player mockPlayer = mock(Player.class);
		when(mockClient.getPlayer()).thenReturn(mockPlayer);

		Map<Integer, Integer> cards = new HashMap<>();
		Collection collection = new Collection(cards, 100);

		Message okMsg = new Message(MessageType.COLLECTION_RESPONSE,
				new OperationResult<>(OperationStatus.COLLECTION_OK, collection));
		Message badReqMsg = new Message(MessageType.COLLECTION_RESPONSE,
				new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		Message dbErrMsg = new Message(MessageType.COLLECTION_RESPONSE,
				new OperationResult<>(OperationStatus.DB_ERROR, null));
		Message unknownStatusMsg = new Message(MessageType.COLLECTION_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, null));

		ObjectInputStream in = createInputStream(okMsg, badReqMsg, dbErrMsg, unknownStatusMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockPlayer, times(1)).setCollection(any(Collection.class));
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleCollectionResponse: disconnette il client se payload è null o Player locale è null")
	void testHandleCollectionResponseInvalidPayload() throws Exception {
		Message invalidPayloadMsg = new Message(MessageType.COLLECTION_RESPONSE,
				new OperationResult<>(OperationStatus.COLLECTION_OK, null));

		ObjectInputStream in1 = createInputStream(invalidPayloadMsg, null);
		serverListener = new ServerListener(in1, mockClient);
		serverListener.run();

		verify(mockClient, times(1)).serverDisconnection();

		reset(mockClient);
		doAnswer(inv -> {
			serverListener.stopListener();
			return null;
		}).when(mockClient).serverDisconnection();
		when(mockClient.getPlayer()).thenReturn(null);

		Map<Integer, Integer> cards = new HashMap<>();
		Message validPayloadNullPlayerMsg = new Message(MessageType.COLLECTION_RESPONSE,
				new OperationResult<>(OperationStatus.COLLECTION_OK, new Collection(cards, 50)));

		ObjectInputStream in2 = createInputStream(validPayloadNullPlayerMsg, null);
		serverListener = new ServerListener(in2, mockClient);
		serverListener.run();

		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleCraftResponse: aggiorna Player su CRAFT_OK e gestisce tutti gli altri OperationStatus")
	void testHandleCraftResponseSuccess() throws Exception {
		Player mockPlayer = mock(Player.class, withSettings().serializable());

		Message okMsg = new Message(MessageType.CRAFT_RESPONSE,
				new OperationResult<>(OperationStatus.CRAFT_OK, mockPlayer));
		Message notEnoughDustMsg = new Message(MessageType.CRAFT_RESPONSE,
				new OperationResult<>(OperationStatus.NOT_ENOUGH_DUST, null));
		Message badReqMsg = new Message(MessageType.CRAFT_RESPONSE,
				new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		Message dbErrMsg = new Message(MessageType.CRAFT_RESPONSE,
				new OperationResult<>(OperationStatus.DB_ERROR, null));
		Message unknownStatusMsg = new Message(MessageType.CRAFT_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, null));

		ObjectInputStream in = createInputStream(okMsg, notEnoughDustMsg, badReqMsg, dbErrMsg, unknownStatusMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, times(1)).setPlayer(any(Player.class));
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleCraftResponse: disconnette il client se CRAFT_OK ha payload null")
	void testHandleCraftResponseInvalidPayload() throws Exception {
		Message invalidPayloadMsg = new Message(MessageType.CRAFT_RESPONSE,
				new OperationResult<>(OperationStatus.CRAFT_OK, null));

		ObjectInputStream in = createInputStream(invalidPayloadMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, never()).setPlayer(any());
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleDisenchantResponse: aggiorna Player su DISENCHANT_OK e gestisce tutti gli altri OperationStatus")
	void testHandleDisenchantResponseSuccess() throws Exception {
		Player mockPlayer = mock(Player.class, withSettings().serializable());

		Message okMsg = new Message(MessageType.DISENCHANT_RESPONSE,
				new OperationResult<>(OperationStatus.DISENCHANT_OK, mockPlayer));
		Message notOwnedMsg = new Message(MessageType.DISENCHANT_RESPONSE,
				new OperationResult<>(OperationStatus.CARD_NOT_OWNED, null));
		Message badReqMsg = new Message(MessageType.DISENCHANT_RESPONSE,
				new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		Message dbErrMsg = new Message(MessageType.DISENCHANT_RESPONSE,
				new OperationResult<>(OperationStatus.DB_ERROR, null));
		Message unknownStatusMsg = new Message(MessageType.DISENCHANT_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, null));

		ObjectInputStream in = createInputStream(okMsg, notOwnedMsg, badReqMsg, dbErrMsg, unknownStatusMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, times(1)).setPlayer(any(Player.class));
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleDisenchantResponse: disconnette il client se DISENCHANT_OK ha payload null")
	void testHandleDisenchantResponseInvalidPayload() throws Exception {
		Message invalidPayloadMsg = new Message(MessageType.DISENCHANT_RESPONSE,
				new OperationResult<>(OperationStatus.DISENCHANT_OK, null));

		ObjectInputStream in = createInputStream(invalidPayloadMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, never()).setPlayer(any());
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleBackNavigationResponse: gestisce BACKNAVIGATION_OK, BAD_REQUEST e status sconosciuto")
	void testHandleBackNavigationResponseSuccess() throws Exception {
		Message okMsg = new Message(MessageType.BACKNAVIGATION_RESPONSE,
				new OperationResult<>(OperationStatus.BACKNAVIGATION_OK, ClientState.MAIN_MENU));
		Message badReqMsg = new Message(MessageType.BACKNAVIGATION_RESPONSE,
				new OperationResult<>(OperationStatus.BAD_REQUEST, null));
		Message unknownStatusMsg = new Message(MessageType.BACKNAVIGATION_RESPONSE,
				new OperationResult<>(OperationStatus.LOGIN_OK, null));

		ObjectInputStream in = createInputStream(okMsg, badReqMsg, unknownStatusMsg, null);
		serverListener = new ServerListener(in, mockClient);

		assertDoesNotThrow(() -> serverListener.run());
		verify(mockClient, times(1)).serverDisconnection();
	}

	@Test
	@DisplayName("handleBackNavigationResponse: disconnette il client se BACKNAVIGATION_OK ha payload null")
	void testHandleBackNavigationResponseInvalidPayload() throws Exception {
		Message invalidPayloadMsg = new Message(MessageType.BACKNAVIGATION_RESPONSE,
				new OperationResult<>(OperationStatus.BACKNAVIGATION_OK, null));

		ObjectInputStream in = createInputStream(invalidPayloadMsg, null);
		serverListener = new ServerListener(in, mockClient);

		serverListener.run();

		verify(mockClient, times(1)).serverDisconnection();
	}
}