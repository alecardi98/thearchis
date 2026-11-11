package thearchis.server.db;

import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.game.Collection;
import thearchis.common.game.Player;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DBManagerTest {

	@Mock
	private MongoDatabase mockDatabase;
	@Mock
	private MongoCollection<Document> mockUsersCollection;
	@Mock
	private FindIterable<Document> mockFindIterable;
	@InjectMocks
	private DBManager dbManager;

	@BeforeEach
	void setUp() {
		lenient().when(mockDatabase.getCollection(anyString())).thenReturn(mockUsersCollection);
		lenient().when(mockUsersCollection.find(any(Bson.class))).thenReturn(mockFindIterable);
	}

	@Test
	@DisplayName("Rifiuta un nickname già esistente in registrazione (Errore 11000)")
	void testRegisterUserNicknameAlreadyUsed() {
		WriteError writeError = new WriteError(11000, "DuplicateKey", new BsonDocument());
		MongoWriteException duplicateException = new MongoWriteException(writeError, null);

		doThrow(duplicateException).when(mockUsersCollection).insertOne(any(Document.class));

		OperationResult<Void> result = dbManager.registerUser("ExistingPlayer", "Pass123");

		assertEquals(OperationStatus.NICKNAME_ALREADY_EXISTS, result.getStatus());
	}

	@Test
	@DisplayName("Popola correttamente le carte vincolate nel sotto-documento collection")
	void testRegisterUserInitialCollection() {
		OperationResult<Void> result = dbManager.registerUser("NewPlayer", "Pass123");

		assertEquals(OperationStatus.REGISTER_OK, result.getStatus());

		ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
		verify(mockUsersCollection).insertOne(docCaptor.capture());

		Document createdDoc = docCaptor.getValue();
		assertEquals("NewPlayer", createdDoc.getString("nickname"));

		Document initialCollection = (Document) createdDoc.get("collection");
		assertNotNull(initialCollection, "La collezione iniziale non deve essere null");
		assertFalse(initialCollection.isEmpty(), "La collezione iniziale deve contenere le carte base");
	}

	@Test
	@DisplayName("Rifiuta login per utente non trovato nel database")
	void testLoginUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Player> result = dbManager.loginUser("UnknownUser", "Pass123");

		assertEquals(OperationStatus.INVALID_CREDENTIALS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("Rifiuta login per password errata")
	void testLoginUserWrongPassword() {
		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("password",
				"CorrectPassword123");

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = dbManager.loginUser("ExistingPlayer", "WrongPassword");

		assertEquals(OperationStatus.INVALID_CREDENTIALS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("Login eseguito con successo con parsing del Player")
	void testLoginUserSuccess() {
		Document fakeCollection = new Document().append("101", 2);
		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("password", "CorrectPass")
				.append("coins", 150).append("dust", 50).append("wins", 5).append("losses", 2)
				.append("collection", fakeCollection);

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = dbManager.loginUser("ExistingPlayer", "CorrectPass");

		assertEquals(OperationStatus.LOGIN_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals("ExistingPlayer", result.getData().getNickname());
		assertEquals(150, result.getData().getCoins());
		assertEquals(50, result.getData().getDust());
	}

	@Test
	@DisplayName("Craft fallito se l'utente non esiste nel database (userDoc nullo)")
	void testCraftUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Player> result = dbManager.craftCard("UnknownPlayer", 101, 100);

		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
		assertNull(result.getData());
		verify(mockUsersCollection, never()).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Craft fallito se la polvere è insufficiente")
	void testCraftCardNotEnoughDust() {
		Document fakeUserDoc = new Document().append("nickname", "PlayerOne").append("dust", 20);

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = dbManager.craftCard("PlayerOne", 101, 100);

		assertEquals(OperationStatus.NOT_ENOUGH_DUST, result.getStatus());
		assertNull(result.getData());
		verify(mockUsersCollection, never()).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Craft eseguito con successo ed incremento quantità carta")
	void testCraftCardSuccess() {
		Document initialDoc = new Document().append("nickname", "PlayerOne").append("dust", 150).append("collection",
				new Document());

		Document updatedDoc = new Document().append("nickname", "PlayerOne").append("dust", 50).append("collection",
				new Document().append("101", 1));

		when(mockFindIterable.first()).thenReturn(initialDoc).thenReturn(updatedDoc);

		OperationResult<Player> result = dbManager.craftCard("PlayerOne", 101, 100);

		assertEquals(OperationStatus.CRAFT_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(50, result.getData().getDust());
		verify(mockUsersCollection).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Disenchant fallito se l'utente non esiste nel database (userDoc nullo)")
	void testDisenchantUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Player> result = dbManager.disenchantCard("UnknownPlayer", 101, 20);

		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
		assertNull(result.getData());
		verify(mockUsersCollection, never()).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Disenchant fallito se la carta non è posseduta nella collezione")
	void testDisenchantCardNotOwned() {
		Document fakeUserDoc = new Document().append("nickname", "PlayerOne").append("collection", new Document());

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = dbManager.disenchantCard("PlayerOne", 101, 20);

		assertEquals(OperationStatus.CARD_NOT_OWNED, result.getStatus());
		assertNull(result.getData());
		verify(mockUsersCollection, never()).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Disenchant di una carta con quantità > 1 (decrementa quantità)")
	void testDisenchantCardDecrementQuantity() {
		Document initialDoc = new Document().append("nickname", "PlayerOne").append("dust", 10).append("collection",
				new Document().append("101", 2));

		Document updatedDoc = new Document().append("nickname", "PlayerOne").append("dust", 30).append("collection",
				new Document().append("101", 1));

		when(mockFindIterable.first()).thenReturn(initialDoc).thenReturn(updatedDoc);

		OperationResult<Player> result = dbManager.disenchantCard("PlayerOne", 101, 20);

		assertEquals(OperationStatus.DISENCHANT_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(30, result.getData().getDust());
		verify(mockUsersCollection).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Disenchant di una carta con quantità = 1 (rimuove la chiave)")
	void testDisenchantCardLastCopyUnset() {
		Document initialDoc = new Document().append("nickname", "PlayerOne").append("dust", 10).append("collection",
				new Document().append("101", 1));

		Document updatedDoc = new Document().append("nickname", "PlayerOne").append("dust", 30).append("collection",
				new Document());

		when(mockFindIterable.first()).thenReturn(initialDoc).thenReturn(updatedDoc);

		OperationResult<Player> result = dbManager.disenchantCard("PlayerOne", 101, 20);

		assertEquals(OperationStatus.DISENCHANT_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(30, result.getData().getDust());
		verify(mockUsersCollection).updateOne(any(Bson.class), any(Bson.class));
	}

	@Test
	@DisplayName("Collezione non trovata se l'utente non viene trovato")
	void testGetUserCollectionUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Collection> result = dbManager.getUserCollection("UnknownUser");

		assertEquals(OperationStatus.DB_ERROR, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("Recupera con successo la collezione dell'utente")
	void testGetUserCollectionSuccess() {
		Document fakeCollectionDoc = new Document().append("101", 2).append("102", 1);
		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("collection",
				fakeCollectionDoc);

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = dbManager.getUserCollection("ExistingPlayer");

		assertEquals(OperationStatus.COLLECTION_OK, result.getStatus());
		assertNotNull(result.getData());
	}
}