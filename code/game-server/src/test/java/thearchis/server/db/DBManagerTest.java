package thearchis.server.db;

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
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
import thearchis.common.hero.HeroName;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

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

	// =========================================================================
	// 1. registerUser
	// =========================================================================

	@Test
	@DisplayName("registerUser: rifiuta le credenziali null/vuote senza toccare il DB")
	void testRegisterUserNullCredentials() {
		OperationResult<Void> resultBothNull = dbManager.registerUser(null, null);
		assertEquals(OperationStatus.BAD_REQUEST, resultBothNull.getStatus());
		assertNull(resultBothNull.getData());

		OperationResult<Void> resultNullNick = dbManager.registerUser(null, "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultNullNick.getStatus());
		assertNull(resultNullNick.getData());

		OperationResult<Void> resultNullPass = dbManager.registerUser("nick", null);
		assertEquals(OperationStatus.BAD_REQUEST, resultNullPass.getStatus());
		assertNull(resultNullPass.getData());

		OperationResult<Void> resultBothEmpty = dbManager.registerUser("", "");
		assertEquals(OperationStatus.BAD_REQUEST, resultBothEmpty.getStatus());
		assertNull(resultBothEmpty.getData());

		OperationResult<Void> resultEmptyNick = dbManager.registerUser("   ", "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyNick.getStatus());
		assertNull(resultEmptyNick.getData());

		OperationResult<Void> resultEmptyPass = dbManager.registerUser("nick", "   ");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyPass.getStatus());
		assertNull(resultEmptyPass.getData());

		verify(mockUsersCollection, never()).insertOne(any(Document.class));
	}

	@Test
	@DisplayName("registerUser: rifiuta un nickname già esistente (Errore 11000)")
	void testRegisterUserNicknameAlreadyUsed() {
		WriteError writeError = new WriteError(11000, "DuplicateKey", new BsonDocument());
		MongoWriteException duplicateException = new MongoWriteException(writeError, null);

		doThrow(duplicateException).when(mockUsersCollection).insertOne(any(Document.class));

		OperationResult<Void> result = dbManager.registerUser("ExistingPlayer", "Pass123");

		assertEquals(OperationStatus.NICKNAME_ALREADY_EXISTS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("registerUser: popola correttamente dust e carte vincolate nel sotto-documento collection")
	void testRegisterUserInitialCollection() {
		OperationResult<Void> result = dbManager.registerUser("NewPlayer", "Pass123");

		assertEquals(OperationStatus.REGISTER_OK, result.getStatus());

		ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
		verify(mockUsersCollection).insertOne(docCaptor.capture());

		Document createdDoc = docCaptor.getValue();
		assertEquals("NewPlayer", createdDoc.getString("nickname"));

		Document initialCollection = createdDoc.get("collection", Document.class);
		assertNotNull(initialCollection, "La collezione iniziale non deve essere null");
		assertEquals(0, initialCollection.getInteger("dust"));

		Document initialCards = initialCollection.get("cards", Document.class);
		assertNotNull(initialCards, "La mappa cards iniziale non deve essere null");
		assertFalse(initialCards.isEmpty(), "La collezione iniziale deve contenere le carte base");
	}

	// =========================================================================
	// 2. loginUser (e parsing Player / Collection / Decks)
	// =========================================================================

	@Test
	@DisplayName("loginUser: rifiuta le credenziali null/vuote senza toccare il DB")
	void testLoginUserNullCredentials() {
		OperationResult<Player> resultBothNull = dbManager.loginUser(null, null);
		assertEquals(OperationStatus.BAD_REQUEST, resultBothNull.getStatus());
		assertNull(resultBothNull.getData());

		OperationResult<Player> resultNullNick = dbManager.loginUser(null, "password");
		assertEquals(OperationStatus.BAD_REQUEST, resultNullNick.getStatus());
		assertNull(resultNullNick.getData());

		OperationResult<Player> resultNullPass = dbManager.loginUser("nickname", null);
		assertEquals(OperationStatus.BAD_REQUEST, resultNullPass.getStatus());
		assertNull(resultNullPass.getData());

		OperationResult<Player> resultBothEmpty = dbManager.loginUser("", "");
		assertEquals(OperationStatus.BAD_REQUEST, resultBothEmpty.getStatus());
		assertNull(resultBothEmpty.getData());

		OperationResult<Player> resultEmptyNick = dbManager.loginUser("   ", "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyNick.getStatus());
		assertNull(resultEmptyNick.getData());

		OperationResult<Player> resultEmptyPass = dbManager.loginUser("nick", "   ");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyPass.getStatus());
		assertNull(resultEmptyPass.getData());

		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("loginUser: rifiuta login per utente non trovato nel database")
	void testLoginUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Player> result = dbManager.loginUser("UnknownUser", "Pass123");

		assertEquals(OperationStatus.INVALID_CREDENTIALS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("loginUser: rifiuta login per password errata")
	void testLoginUserWrongPassword() {
		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("password",
				"CorrectPassword123");

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = dbManager.loginUser("ExistingPlayer", "WrongPassword");

		assertEquals(OperationStatus.INVALID_CREDENTIALS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("loginUser: restituisce DB_ERROR se il documento utente è corrotto")
	void testLoginUserCorruptedDocument() {
		Document missingNicknameDoc = new Document().append("password", "Pass123")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()))
				.append("decks", new ArrayList<>());

		Document blankNicknameDoc = new Document().append("nickname", "   ").append("password", "Pass123")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()))
				.append("decks", new ArrayList<>());

		Document missingCollectionDoc = new Document().append("nickname", "PlayerOne").append("password", "Pass123")
				.append("decks", new ArrayList<>());

		Document missingCardsDoc = new Document().append("nickname", "PlayerOne").append("password", "Pass123")
				.append("collection", new Document().append("dust", 10)).append("decks", new ArrayList<>());

		Document missingDecksDoc = new Document().append("nickname", "PlayerOne").append("password", "Pass123")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()));

		Document wrongTypeDoc = new Document().append("nickname", "PlayerOne").append("password", "Pass123")
				.append("coins", "nonUnNumero")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()))
				.append("decks", new ArrayList<>());

		when(mockFindIterable.first()).thenReturn(missingNicknameDoc).thenReturn(blankNicknameDoc)
				.thenReturn(missingCollectionDoc).thenReturn(missingCardsDoc).thenReturn(missingDecksDoc)
				.thenReturn(wrongTypeDoc);

		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
	}

	@Test
	@DisplayName("loginUser: eseguito con successo con parsing del Player")
	void testLoginUserSuccess() {
		Document fakeCollection = new Document().append("dust", 50).append("cards", new Document().append("101", 2));
		Document fakeDeckCards = new Document().append("101", 2);
		Document fakeDeck = new Document().append("hero", HeroName.values()[0].name()).append("cards", fakeDeckCards);

		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("password", "CorrectPass")
				.append("coins", 150).append("wins", 5).append("losses", 2).append("collection", fakeCollection)
				.append("decks", List.of(fakeDeck));

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = dbManager.loginUser("ExistingPlayer", "CorrectPass");

		assertEquals(OperationStatus.LOGIN_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals("ExistingPlayer", result.getData().getNickname());
		assertEquals(150, result.getData().getCoins());
		assertEquals(50, result.getData().getCollection().getDust());
		assertEquals(2, result.getData().getCollection().getCardQuantity(101));
	}

	// =========================================================================
	// 3. getUserCollection
	// =========================================================================

	@Test
	@DisplayName("getUserCollection: rifiuta nickname null o vuoto")
	void testGetUserCollectionInvalidNickname() {
		assertEquals(OperationStatus.BAD_REQUEST, dbManager.getUserCollection(null).getStatus());
		assertEquals(OperationStatus.BAD_REQUEST, dbManager.getUserCollection("   ").getStatus());

		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("getUserCollection: rifiuta se l'utente non viene trovato")
	void testGetUserCollectionUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Collection> result = dbManager.getUserCollection("UnknownUser");

		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("getUserCollection: restituisce DB_ERROR se il campo collection/cards manca o ha tipo errato")
	void testGetUserCollectionCorruptedDocument() {
		Document missingCollectionDoc = new Document().append("nickname", "PlayerOne");
		Document wrongTypeCollectionDoc = new Document().append("nickname", "PlayerOne").append("collection",
				"invalid");
		Document missingCardsDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 50));

		when(mockFindIterable.first()).thenReturn(missingCollectionDoc).thenReturn(wrongTypeCollectionDoc)
				.thenReturn(missingCardsDoc);

		assertEquals(OperationStatus.DB_ERROR, dbManager.getUserCollection("PlayerOne").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.getUserCollection("PlayerOne").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.getUserCollection("PlayerOne").getStatus());
	}

	@Test
	@DisplayName("getUserCollection: recupera con successo la collezione dell'utente")
	void testGetUserCollectionSuccess() {
		Document fakeCardsDoc = new Document().append("101", 2).append("102", 1);
		Document fakeCollectionDoc = new Document().append("dust", 75).append("cards", fakeCardsDoc);
		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("collection",
				fakeCollectionDoc);

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = dbManager.getUserCollection("ExistingPlayer");

		assertEquals(OperationStatus.COLLECTION_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(75, result.getData().getDust());
		assertEquals(2, result.getData().getCardQuantity(101));
		assertEquals(1, result.getData().getCardQuantity(102));
	}

	// =========================================================================
	// 4. craftCard
	// =========================================================================

	@Test
	@DisplayName("craftCard: fallito per nickname null o vuoto")
	void testCraftInvalidNickname() {
		assertEquals(OperationStatus.BAD_REQUEST, dbManager.craftCard(null, 101, 100).getStatus());
		assertEquals(OperationStatus.BAD_REQUEST, dbManager.craftCard("   ", 101, 100).getStatus());

		verify(mockUsersCollection, never()).findOneAndUpdate(any(Bson.class), any(Bson.class),
				any(FindOneAndUpdateOptions.class));
		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("craftCard: fallito se l'utente non esiste nel database (diagnostica su null)")
	void testCraftUserNotFound() {
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Collection> result = dbManager.craftCard("UnknownPlayer", 101, 100);

		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("craftCard: restituisce DB_ERROR se il documento diagnostico ha collection/dust mancante o non valido")
	void testCraftCorruptedDiagnosticDocument() {
		Document missingCollectionDoc = new Document().append("nickname", "PlayerOne");
		Document invalidDustDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", "nonUnNumero"));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(missingCollectionDoc).thenReturn(invalidDustDoc);

		assertEquals(OperationStatus.DB_ERROR, dbManager.craftCard("PlayerOne", 101, 100).getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.craftCard("PlayerOne", 101, 100).getStatus());
	}

	@Test
	@DisplayName("craftCard: fallito se la polvere è insufficiente")
	void testCraftCardNotEnoughDust() {
		Document fakeUserDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 20).append("cards", new Document()));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = dbManager.craftCard("PlayerOne", 101, 100);

		assertEquals(OperationStatus.NOT_ENOUGH_DUST, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("craftCard: restituisce DB_ERROR se il documento aggiornato ha collection o cards mancante")
	void testCraftCorruptedUpdatedDocument() {
		Document missingCollectionUpdatedDoc = new Document().append("nickname", "PlayerOne");
		Document missingCardsUpdatedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 50));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(missingCollectionUpdatedDoc).thenReturn(missingCardsUpdatedDoc);

		assertEquals(OperationStatus.DB_ERROR, dbManager.craftCard("PlayerOne", 101, 100).getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.craftCard("PlayerOne", 101, 100).getStatus());
	}

	@Test
	@DisplayName("craftCard: eseguito con successo in una singola operazione atomica")
	void testCraftCardSuccess() {
		Document updatedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 50).append("cards", new Document().append("101", 1)));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(updatedDoc);

		OperationResult<Collection> result = dbManager.craftCard("PlayerOne", 101, 100);

		assertEquals(OperationStatus.CRAFT_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(50, result.getData().getDust());
		assertEquals(1, result.getData().getCardQuantity(101));
		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	// =========================================================================
	// 5. disenchantCard
	// =========================================================================

	@Test
	@DisplayName("disenchantCard: fallito per nickname null o vuoto")
	void testDisenchantInvalidNickname() {
		assertEquals(OperationStatus.BAD_REQUEST, dbManager.disenchantCard(null, 101, 20).getStatus());
		assertEquals(OperationStatus.BAD_REQUEST, dbManager.disenchantCard("   ", 101, 20).getStatus());

		verify(mockUsersCollection, never()).findOneAndUpdate(any(Bson.class), anyList(),
				any(FindOneAndUpdateOptions.class));
		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("disenchantCard: fallito se l'utente non esiste nel database (diagnostica su null)")
	void testDisenchantUserNotFound() {
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Collection> result = dbManager.disenchantCard("UnknownPlayer", 101, 20);

		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("disenchantCard: restituisce DB_ERROR se il documento diagnostico ha collection o cards mancante")
	void testDisenchantCorruptedDiagnosticDocument() {
		Document missingCollectionDoc = new Document().append("nickname", "PlayerOne");
		Document missingCardsDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 10));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(missingCollectionDoc).thenReturn(missingCardsDoc);

		assertEquals(OperationStatus.DB_ERROR, dbManager.disenchantCard("PlayerOne", 101, 20).getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.disenchantCard("PlayerOne", 101, 20).getStatus());
	}

	@Test
	@DisplayName("disenchantCard: fallito se la carta non è posseduta nella collezione")
	void testDisenchantCardNotOwned() {
		Document fakeUserDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 10).append("cards", new Document()));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = dbManager.disenchantCard("PlayerOne", 101, 20);

		assertEquals(OperationStatus.CARD_NOT_OWNED, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("disenchantCard: restituisce DB_ERROR se il documento aggiornato ha collection o cards mancante")
	void testDisenchantCorruptedUpdatedDocument() {
		Document missingCollectionUpdatedDoc = new Document().append("nickname", "PlayerOne");
		Document missingCardsUpdatedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 30));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenReturn(missingCollectionUpdatedDoc).thenReturn(missingCardsUpdatedDoc);

		assertEquals(OperationStatus.DB_ERROR, dbManager.disenchantCard("PlayerOne", 101, 20).getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.disenchantCard("PlayerOne", 101, 20).getStatus());
	}

	@Test
	@DisplayName("disenchantCard: eseguito con successo in una singola operazione atomica (decremento e rimozione ultima copia)")
	void testDisenchantCardSuccess() {
		Document decrementedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 30).append("cards", new Document().append("101", 1)));

		Document removedLastCopyDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 50).append("cards", new Document()));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenReturn(decrementedDoc).thenReturn(removedLastCopyDoc);

		OperationResult<Collection> resultDecrement = dbManager.disenchantCard("PlayerOne", 101, 20);
		assertEquals(OperationStatus.DISENCHANT_OK, resultDecrement.getStatus());
		assertNotNull(resultDecrement.getData());
		assertEquals(30, resultDecrement.getData().getDust());
		assertEquals(1, resultDecrement.getData().getCardQuantity(101));

		OperationResult<Collection> resultRemove = dbManager.disenchantCard("PlayerOne", 101, 20);
		assertEquals(OperationStatus.DISENCHANT_OK, resultRemove.getStatus());
		assertNotNull(resultRemove.getData());
		assertEquals(50, resultRemove.getData().getDust());
		assertEquals(0, resultRemove.getData().getCardQuantity(101));

		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	// =========================================================================
	// 6. Gestione trasversale MongoException
	// =========================================================================

	@Test
	@DisplayName("Gestisce MongoException restituendo DB_ERROR su tutte le operazioni")
	void testMongoExceptionReturnsDbError() {
		doThrow(new MongoException("Write failed")).when(mockUsersCollection).insertOne(any(Document.class));
		when(mockUsersCollection.find(any(Bson.class))).thenThrow(new MongoException("Connection lost"));
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenThrow(new MongoException("Connection lost"));
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenThrow(new MongoException("Connection lost"));

		assertEquals(OperationStatus.DB_ERROR, dbManager.registerUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.loginUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.getUserCollection("PlayerOne").getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.craftCard("PlayerOne", 101, 100).getStatus());
		assertEquals(OperationStatus.DB_ERROR, dbManager.disenchantCard("PlayerOne", 101, 20).getStatus());
	}
}