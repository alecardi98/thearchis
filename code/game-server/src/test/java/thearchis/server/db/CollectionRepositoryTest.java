package thearchis.server.db;

import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import thearchis.common.game.state.Collection;
import thearchis.common.message.OperationResult;
import thearchis.common.message.OperationStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CollectionRepositoryTest {

	@Mock
	private MongoDBManager mockDbManager;
	@Mock
	private MongoCollection<Document> mockUsersCollection;
	@Mock
	private FindIterable<Document> mockFindIterable;

	private CollectionRepository collectionRepository;

	@BeforeEach
	void setUp() {
		collectionRepository = new CollectionRepository(mockDbManager, new DocumentMapper());
		lenient().when(mockDbManager.getUsersCollection()).thenReturn(mockUsersCollection);
		lenient().when(mockUsersCollection.find(any(Bson.class))).thenReturn(mockFindIterable);
	}

	@Test
	@DisplayName("getUserCollection: rifiuta nickname null o vuoto")
	void testGetUserCollectionInvalidNickname() {
		assertEquals(OperationStatus.BAD_REQUEST, collectionRepository.getUserCollection(null).getStatus());
		assertEquals(OperationStatus.BAD_REQUEST, collectionRepository.getUserCollection("   ").getStatus());

		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("getUserCollection: rifiuta se l'utente non viene trovato")
	void testGetUserCollectionUserNotFound() {
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Collection> result = collectionRepository.getUserCollection("UnknownUser");

		assertEquals(OperationStatus.BAD_REQUEST, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("getUserCollection: restituisce DB_ERROR se il campo collection/cards manca o ha tipo errato")
	void testGetUserCollectionCorruptedDocument() {
		Document missingCollectionDoc = new Document().append("nickname", "nick");
		Document wrongTypeCollectionDoc = new Document().append("nickname", "nick").append("collection", "invalid");
		Document missingCardsDoc = new Document().append("nickname", "nick").append("collection",
				new Document().append("dust", 50));

		List<Document> corruptedDocs = List.of(missingCollectionDoc, wrongTypeCollectionDoc, missingCardsDoc);

		for (Document doc : corruptedDocs) {
			when(mockFindIterable.first()).thenReturn(doc);
			assertEquals(OperationStatus.DB_ERROR, collectionRepository.getUserCollection("nick").getStatus());
		}
	}

	@Test
	@DisplayName("getUserCollection: recupera con successo la collezione dell'utente")
	void testGetUserCollectionSuccess() {
		Document fakeCardsDoc = new Document().append("101", 2).append("102", 1);
		Document fakeCollectionDoc = new Document().append("dust", 75).append("cards", fakeCardsDoc);
		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("collection",
				fakeCollectionDoc);

		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = collectionRepository.getUserCollection("ExistingPlayer");

		assertEquals(OperationStatus.COLLECTION_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(75, result.getData().getDust());
		assertEquals(2, result.getData().getCardQuantity(101));
		assertEquals(1, result.getData().getCardQuantity(102));
	}

	@Test
	@DisplayName("craftCard: fallito per nickname null o vuoto")
	void testCraftInvalidNickname() {
		assertEquals(OperationStatus.BAD_REQUEST, collectionRepository.craftCard(null, 101, 100).getStatus());
		assertEquals(OperationStatus.BAD_REQUEST, collectionRepository.craftCard("   ", 101, 100).getStatus());

		verify(mockUsersCollection, never()).findOneAndUpdate(any(Bson.class), any(Bson.class),
				any(FindOneAndUpdateOptions.class));
		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("craftCard: fallito se l'utente non esiste nel database (documento su null)")
	void testCraftUserNotFound() {
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(null);

		OperationResult<Collection> result = collectionRepository.craftCard("UnknownPlayer", 101, 100);

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

		for (Document doc : List.of(missingCollectionDoc, invalidDustDoc)) {
			when(mockFindIterable.first()).thenReturn(doc);
			assertEquals(OperationStatus.DB_ERROR, collectionRepository.craftCard("PlayerOne", 101, 100).getStatus());
		}
	}

	@Test
	@DisplayName("craftCard: fallito se la polvere è insufficiente")
	void testCraftCardNotEnoughDust() {
		Document fakeUserDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 20).append("cards", new Document()));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = collectionRepository.craftCard("PlayerOne", 101, 100);

		assertEquals(OperationStatus.NOT_ENOUGH_DUST, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("craftCard: restituisce DB_ERROR se il documento aggiornato ha collection o cards mancante")
	void testCraftCorruptedUpdatedDocument() {
		Document missingCollectionUpdatedDoc = new Document().append("nickname", "PlayerOne");
		Document missingCardsUpdatedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 50));

		for (Document doc : List.of(missingCollectionUpdatedDoc, missingCardsUpdatedDoc)) {
			when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class),
					any(FindOneAndUpdateOptions.class))).thenReturn(doc);
			assertEquals(OperationStatus.DB_ERROR, collectionRepository.craftCard("PlayerOne", 101, 100).getStatus());
		}
	}

	@Test
	@DisplayName("craftCard: eseguito con successo in una singola operazione atomica")
	void testCraftCardSuccess() {
		Document updatedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 50).append("cards", new Document().append("101", 1)));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenReturn(updatedDoc);

		OperationResult<Collection> result = collectionRepository.craftCard("PlayerOne", 101, 100);

		assertEquals(OperationStatus.CRAFT_OK, result.getStatus());
		assertNotNull(result.getData());
		assertEquals(50, result.getData().getDust());
		assertEquals(1, result.getData().getCardQuantity(101));
		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("disenchantCard: fallito per nickname null o vuoto")
	void testDisenchantInvalidNickname() {
		assertEquals(OperationStatus.BAD_REQUEST, collectionRepository.disenchantCard(null, 101, 20).getStatus());
		assertEquals(OperationStatus.BAD_REQUEST, collectionRepository.disenchantCard("   ", 101, 20).getStatus());

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

		OperationResult<Collection> result = collectionRepository.disenchantCard("UnknownPlayer", 101, 20);

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

		for (Document doc : List.of(missingCollectionDoc, missingCardsDoc)) {
			when(mockFindIterable.first()).thenReturn(doc);
			assertEquals(OperationStatus.DB_ERROR,
					collectionRepository.disenchantCard("PlayerOne", 101, 20).getStatus());
		}
	}

	@Test
	@DisplayName("disenchantCard: fallito se la carta non è posseduta nella collezione")
	void testDisenchantCardNotOwned() {
		Document fakeUserDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 10).append("cards", new Document()));

		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenReturn(null);
		when(mockFindIterable.first()).thenReturn(fakeUserDoc);

		OperationResult<Collection> result = collectionRepository.disenchantCard("PlayerOne", 101, 20);

		assertEquals(OperationStatus.CARD_NOT_OWNED, result.getStatus());
		assertNull(result.getData());
	}

	@Test
	@DisplayName("disenchantCard: restituisce DB_ERROR se il documento aggiornato ha collection o cards mancante")
	void testDisenchantCorruptedUpdatedDocument() {
		Document missingCollectionUpdatedDoc = new Document().append("nickname", "PlayerOne");
		Document missingCardsUpdatedDoc = new Document().append("nickname", "PlayerOne").append("collection",
				new Document().append("dust", 30));

		for (Document doc : List.of(missingCollectionUpdatedDoc, missingCardsUpdatedDoc)) {
			when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
					.thenReturn(doc);
			assertEquals(OperationStatus.DB_ERROR,
					collectionRepository.disenchantCard("PlayerOne", 101, 20).getStatus());
		}
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

		OperationResult<Collection> resultDecrement = collectionRepository.disenchantCard("PlayerOne", 101, 20);
		assertEquals(OperationStatus.DISENCHANT_OK, resultDecrement.getStatus());
		assertNotNull(resultDecrement.getData());
		assertEquals(30, resultDecrement.getData().getDust());
		assertEquals(1, resultDecrement.getData().getCardQuantity(101));

		OperationResult<Collection> resultRemove = collectionRepository.disenchantCard("PlayerOne", 101, 20);
		assertEquals(OperationStatus.DISENCHANT_OK, resultRemove.getStatus());
		assertNotNull(resultRemove.getData());
		assertEquals(50, resultRemove.getData().getDust());
		assertEquals(0, resultRemove.getData().getCardQuantity(101));

		verify(mockUsersCollection, never()).find(any(Bson.class));
	}

	@Test
	@DisplayName("Gestisce MongoException restituendo DB_ERROR su tutte le operazioni")
	void testMongoExceptionReturnsDbError() {
		when(mockUsersCollection.find(any(Bson.class))).thenThrow(new MongoException("Connection lost"));
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), any(Bson.class), any(FindOneAndUpdateOptions.class)))
				.thenThrow(new MongoException("Connection lost"));
		when(mockUsersCollection.findOneAndUpdate(any(Bson.class), anyList(), any(FindOneAndUpdateOptions.class)))
				.thenThrow(new MongoException("Connection lost"));

		assertEquals(OperationStatus.DB_ERROR, collectionRepository.getUserCollection("PlayerOne").getStatus());
		assertEquals(OperationStatus.DB_ERROR, collectionRepository.craftCard("PlayerOne", 101, 100).getStatus());
		assertEquals(OperationStatus.DB_ERROR, collectionRepository.disenchantCard("PlayerOne", 101, 20).getStatus());
	}

}