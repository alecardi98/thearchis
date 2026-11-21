package thearchis.server.db;

import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.WriteError;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.game.Player;
import thearchis.common.hero.HeroName;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class AuthenticationRepositoryTest {

	@Mock
	private MongoDBManager mockDbManager;
	@Mock
	private MongoCollection<Document> mockUsersCollection;
	@Mock
	private FindIterable<Document> mockFindIterable;

	private AuthenticationRepository authenticationRepository;

	@BeforeEach
	void setUp() {
		authenticationRepository = new AuthenticationRepository(mockDbManager, new DocumentMapper());
		lenient().when(mockDbManager.getUsersCollection()).thenReturn(mockUsersCollection);
		lenient().when(mockUsersCollection.find(any(Bson.class))).thenReturn(mockFindIterable);
	}

	@Test
	@DisplayName("registerUser: rifiuta le credenziali null/vuote senza toccare il DB")
	void testRegisterUserNullCredentials() {
		OperationResult<Void> resultBothNull = authenticationRepository.registerUser(null, null);
		assertEquals(OperationStatus.BAD_REQUEST, resultBothNull.getStatus());
		assertNull(resultBothNull.getData());

		OperationResult<Void> resultNullNick = authenticationRepository.registerUser(null, "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultNullNick.getStatus());
		assertNull(resultNullNick.getData());

		OperationResult<Void> resultNullPass = authenticationRepository.registerUser("nick", null);
		assertEquals(OperationStatus.BAD_REQUEST, resultNullPass.getStatus());
		assertNull(resultNullPass.getData());

		OperationResult<Void> resultBothEmpty = authenticationRepository.registerUser("", "");
		assertEquals(OperationStatus.BAD_REQUEST, resultBothEmpty.getStatus());
		assertNull(resultBothEmpty.getData());

		OperationResult<Void> resultEmptyNick = authenticationRepository.registerUser("   ", "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyNick.getStatus());
		assertNull(resultEmptyNick.getData());

		OperationResult<Void> resultEmptyPass = authenticationRepository.registerUser("nick", "   ");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyPass.getStatus());
		assertNull(resultEmptyPass.getData());

		verify(mockUsersCollection, never()).insertOne(any(Document.class));
	}

	@Test
	@DisplayName("registerUser: rifiuta un nickname già esistente (Errore 11000)")
	void testRegisterUserNicknameAlreadyUsed() {
		WriteError writeError = new WriteError(11000, "DuplicateKey", new BsonDocument());
		
		@SuppressWarnings("deprecation")
		MongoWriteException duplicateException = new MongoWriteException(writeError, null);

		doThrow(duplicateException).when(mockUsersCollection).insertOne(any(Document.class));

		OperationResult<Void> result = authenticationRepository.registerUser("existingPlayer", "psw");

		assertEquals(OperationStatus.NICKNAME_ALREADY_EXISTS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("registerUser: popola correttamente dust e carte vincolate nel sotto-documento collection")
	void testRegisterUserInitialCollection() {
		OperationResult<Void> result = authenticationRepository.registerUser("newPlayer", "psw");

		assertEquals(OperationStatus.REGISTER_OK, result.getStatus());

		ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
		verify(mockUsersCollection).insertOne(docCaptor.capture());

		Document createdDoc = docCaptor.getValue();
		assertEquals("newPlayer", createdDoc.getString("nickname"));

		Document initialCollection = createdDoc.get("collection", Document.class);
		assertNotNull(initialCollection, "La collezione iniziale non deve essere null");
		assertEquals(0, initialCollection.getInteger("dust"));

		Document initialCards = initialCollection.get("cards", Document.class);
		assertNotNull(initialCards, "La mappa cards iniziale non deve essere null");
		assertFalse(initialCards.isEmpty(), "La collezione iniziale deve contenere le carte base");
	}

	@Test
	@DisplayName("loginUser: rifiuta le credenziali null/vuote senza toccare il DB")
	void testLoginUserNullCredentials() {
		OperationResult<Player> resultBothNull = authenticationRepository.loginUser(null, null);
		assertEquals(OperationStatus.BAD_REQUEST, resultBothNull.getStatus());
		assertNull(resultBothNull.getData());

		OperationResult<Player> resultNullNick = authenticationRepository.loginUser(null, "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultNullNick.getStatus());
		assertNull(resultNullNick.getData());

		OperationResult<Player> resultNullPass = authenticationRepository.loginUser("nick", null);
		assertEquals(OperationStatus.BAD_REQUEST, resultNullPass.getStatus());
		assertNull(resultNullPass.getData());

		OperationResult<Player> resultBothEmpty = authenticationRepository.loginUser("", "");
		assertEquals(OperationStatus.BAD_REQUEST, resultBothEmpty.getStatus());
		assertNull(resultBothEmpty.getData());

		OperationResult<Player> resultEmptyNick = authenticationRepository.loginUser("   ", "psw");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyNick.getStatus());
		assertNull(resultEmptyNick.getData());

		OperationResult<Player> resultEmptyPass = authenticationRepository.loginUser("nick", "   ");
		assertEquals(OperationStatus.BAD_REQUEST, resultEmptyPass.getStatus());
		assertNull(resultEmptyPass.getData());

		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("loginUser: rifiuta login per utente non trovato nel database")
	void testLoginUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Player> result = authenticationRepository.loginUser("unknownUser", "psw");

		assertEquals(OperationStatus.INVALID_CREDENTIALS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("loginUser: rifiuta login per password errata")
	void testLoginUserWrongPassword() {
		Document fakeUserDoc = new Document().append("nickname", "existingPlayer").append("password",
				"correctPassword");

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Player> result = authenticationRepository.loginUser("existingPlayer", "wrongPassword");

		assertEquals(OperationStatus.INVALID_CREDENTIALS, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("loginUser: restituisce DB_ERROR se il documento utente è corrotto")
	void testLoginUserCorruptedDocument() {
		Document missingNicknameDoc = new Document().append("password", "psw")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()))
				.append("decks", new ArrayList<>());

		Document blankNicknameDoc = new Document().append("nickname", "   ").append("password", "psw")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()))
				.append("decks", new ArrayList<>());

		Document missingCollectionDoc = new Document().append("nickname", "nick").append("password", "psw")
				.append("decks", new ArrayList<>());

		Document missingCardsDoc = new Document().append("nickname", "nick").append("password", "psw")
				.append("collection", new Document().append("dust", 10)).append("decks", new ArrayList<>());

		Document missingDecksDoc = new Document().append("nickname", "nick").append("password", "psw")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()));

		Document wrongTypeDoc = new Document().append("nickname", "nick").append("password", "psw")
				.append("coins", "NaN")
				.append("collection", new Document().append("dust", 10).append("cards", new Document()))
				.append("decks", new ArrayList<>());

		List<Document> corruptedDocs = List.of(missingNicknameDoc, blankNicknameDoc, missingCollectionDoc,
				missingCardsDoc, missingDecksDoc, wrongTypeDoc);

		for (Document doc : corruptedDocs) {
			when(mockFindIterable.first()).thenReturn(doc);
			assertEquals(OperationStatus.DB_ERROR, authenticationRepository.loginUser("nick", "psw").getStatus());
		}
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

		OperationResult<Player> result = authenticationRepository.loginUser("ExistingPlayer", "CorrectPass");

		assertEquals(OperationStatus.LOGIN_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals("ExistingPlayer", result.getData().getNickname());
		assertEquals(150, result.getData().getCoins());
		assertEquals(50, result.getData().getCollection().getDust());
		assertEquals(2, result.getData().getCollection().getCardQuantity(101));
	}

	@Test
	@DisplayName("Gestisce MongoException restituendo DB_ERROR su tutte le operazioni")
	void testMongoExceptionReturnsDbError() {
		doThrow(new MongoException("Write failed")).when(mockUsersCollection).insertOne(any(Document.class));
		when(mockUsersCollection.find(any(Bson.class))).thenThrow(new MongoException("Connection lost"));

		assertEquals(OperationStatus.DB_ERROR,
				authenticationRepository.registerUser("PlayerOne", "Pass123").getStatus());
		assertEquals(OperationStatus.DB_ERROR, authenticationRepository.loginUser("PlayerOne", "Pass123").getStatus());
	}

}