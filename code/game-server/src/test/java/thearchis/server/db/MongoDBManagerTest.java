package thearchis.server.db;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvBuilder;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoDBManagerTest {

	@Mock
	private DotenvBuilder mockDotenvBuilder;
	@Mock
	private Dotenv mockDotenv;
	@Mock
	private MongoClient mockMongoClient;
	@Mock
	private MongoDatabase mockDatabase;
	@Mock
	private MongoCollection<Document> mockUsersCollection;

	private MongoDBManager dbManager;

	@BeforeEach
	void setUp() {
		dbManager = new MongoDBManager();
	}

	private void setupDotenvMock(MockedStatic<Dotenv> mockedDotenv, String uri) {
		mockedDotenv.when(Dotenv::configure).thenReturn(mockDotenvBuilder);
		when(mockDotenvBuilder.ignoreIfMalformed()).thenReturn(mockDotenvBuilder);
		when(mockDotenvBuilder.ignoreIfMissing()).thenReturn(mockDotenvBuilder);
		when(mockDotenvBuilder.load()).thenReturn(mockDotenv);
		when(mockDotenv.get("CONNECTION_URI")).thenReturn(uri);
	}

	private void injectField(String fieldName, Object value) throws Exception {
		Field field = MongoDBManager.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(dbManager, value);
	}

	@Test
	@DisplayName("dbConnect: connette con successo con URI standard")
	void testDbConnectSuccessWithStandardUri() {
		String standardUri = "mongodb://localhost:27017";

		try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class);
				MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {

			setupDotenvMock(mockedDotenv, standardUri);
			mockedMongoClients.when(() -> MongoClients.create(standardUri)).thenReturn(mockMongoClient);
			when(mockMongoClient.getDatabase("thearchis_db")).thenReturn(mockDatabase);
			when(mockDatabase.getCollection("users")).thenReturn(mockUsersCollection);

			dbManager.dbConnect();

			assertSame(mockDatabase, dbManager.getDatabase());
			verify(mockDatabase).runCommand(any(BsonDocument.class));

			ArgumentCaptor<IndexOptions> indexOptionsCaptor = ArgumentCaptor.forClass(IndexOptions.class);
			verify(mockUsersCollection).createIndex(any(Bson.class), indexOptionsCaptor.capture());
			assertTrue(indexOptionsCaptor.getValue().isUnique());
		}
	}

	@Test
	@DisplayName("dbConnect: converte correttamente un URI mongodb+srv:// nel formato multi-host")
	void testDbConnectSanitizesSrvUri() {
		String srvUri = "mongodb+srv://userTest:passTest@cluster0.jp638fv.mongodb.net/?retryWrites=true&w=majority";
		String expectedSanitizedUri = "mongodb://userTest:passTest@ac-1ss9wkl-shard-00-00.jp638fv.mongodb.net:27017,"
				+ "ac-1ss9wkl-shard-00-01.jp638fv.mongodb.net:27017,"
				+ "ac-1ss9wkl-shard-00-02.jp638fv.mongodb.net:27017/"
				+ "thearchis_db?ssl=true&replicaSet=atlas-wejixd-shard-0&authSource=admin&retryWrites=true&w=majority";

		try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class);
				MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {

			setupDotenvMock(mockedDotenv, srvUri);
			mockedMongoClients.when(() -> MongoClients.create(expectedSanitizedUri)).thenReturn(mockMongoClient);
			when(mockMongoClient.getDatabase("thearchis_db")).thenReturn(mockDatabase);
			when(mockDatabase.getCollection("users")).thenReturn(mockUsersCollection);

			dbManager.dbConnect();

			mockedMongoClients.verify(() -> MongoClients.create(expectedSanitizedUri), times(1));
		}
	}

	@Test
	@DisplayName("dbConnect: non crea una seconda connessione se mongoClient è già inizializzato")
	void testDbConnectAlreadyConnectedDoesNothing() throws Exception {
		injectField("mongoClient", mockMongoClient);

		try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class);
				MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {

			dbManager.dbConnect();

			mockedDotenv.verifyNoInteractions();
			mockedMongoClients.verifyNoInteractions();
		}
	}

	@Test
	@DisplayName("dbConnect: RuntimeException se CONNECTION_URI è mancante o vuota")
	void testDbConnectMissingUriThrowsRuntimeException() {
		if (System.getenv("CONNECTION_URI") != null) {
			return;
		}

		try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class);
				MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {

			setupDotenvMock(mockedDotenv, "   ");

			assertThrows(RuntimeException.class, () -> dbManager.dbConnect());
			mockedMongoClients.verifyNoInteractions();
		}
	}

	@Test
	@DisplayName("dbConnect: RuntimeException se il ping al DB fallisce")
	void testDbConnectFailureClosesClientAndThrows() {
		String standardUri = "mongodb://localhost:27017";

		try (MockedStatic<Dotenv> mockedDotenv = mockStatic(Dotenv.class);
				MockedStatic<MongoClients> mockedMongoClients = mockStatic(MongoClients.class)) {

			setupDotenvMock(mockedDotenv, standardUri);
			mockedMongoClients.when(() -> MongoClients.create(standardUri)).thenReturn(mockMongoClient);
			when(mockMongoClient.getDatabase("thearchis_db")).thenReturn(mockDatabase);
			when(mockDatabase.runCommand(any(BsonDocument.class))).thenThrow(new MongoException("Ping timeout"));

			assertThrows(RuntimeException.class, () -> dbManager.dbConnect());
			verify(mockMongoClient).close();
		}
	}

	@Test
	@DisplayName("dbClose: chiude correttamente il MongoClient se connesso")
	void testDbClose() throws Exception {
		assertDoesNotThrow(() -> dbManager.dbClose());

		injectField("mongoClient", mockMongoClient);
		dbManager.dbClose();

		verify(mockMongoClient, times(1)).close();
	}

	@Test
	@DisplayName("getUsersCollection: IllegalStateException se database è null")
	void testGetUsersCollectionWhenNotConnected() {
		assertThrows(IllegalStateException.class, () -> dbManager.getUsersCollection());
	}

	@Test
	@DisplayName("getUsersCollection: restituisce la collection 'users' quando connesso")
	void testGetUsersCollectionWhenConnected() throws Exception {
		injectField("database", mockDatabase);
		when(mockDatabase.getCollection("users")).thenReturn(mockUsersCollection);

		MongoCollection<Document> result = dbManager.getUsersCollection();

		assertSame(mockUsersCollection, result);
		verify(mockDatabase).getCollection("users");
	}

}