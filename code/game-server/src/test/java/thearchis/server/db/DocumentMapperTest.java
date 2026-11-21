package thearchis.server.db;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import thearchis.common.game.hero.HeroName;
import thearchis.common.game.state.Collection;
import thearchis.common.game.state.Player;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentMapperTest {

	private DocumentMapper documentMapper;

	@BeforeEach
	void setUp() {
		documentMapper = new DocumentMapper();
	}

	@Test
	@DisplayName("parsePlayerFromDocument: restituisce null se il documento utente è corrotto")
	void testParsePlayerCorruptedDocument() {
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

		assertNull(documentMapper.parsePlayerFromDocument(missingNicknameDoc));
		assertNull(documentMapper.parsePlayerFromDocument(blankNicknameDoc));
		assertNull(documentMapper.parsePlayerFromDocument(missingCollectionDoc));
		assertNull(documentMapper.parsePlayerFromDocument(missingCardsDoc));
		assertNull(documentMapper.parsePlayerFromDocument(missingDecksDoc));
		assertNull(documentMapper.parsePlayerFromDocument(wrongTypeDoc));
	}

	@Test
	@DisplayName("parsePlayerFromDocument: eseguito con successo con parsing del Player")
	void testParsePlayerSuccess() {
		Document fakeCollection = new Document().append("dust", 50).append("cards", new Document().append("101", 2));
		Document fakeDeckCards = new Document().append("101", 2);
		Document fakeDeck = new Document().append("hero", HeroName.values()[0].name()).append("cards", fakeDeckCards);

		Document fakeUserDoc = new Document().append("nickname", "ExistingPlayer").append("password", "CorrectPass")
				.append("coins", 150).append("wins", 5).append("losses", 2).append("collection", fakeCollection)
				.append("decks", List.of(fakeDeck));

		Player player = documentMapper.parsePlayerFromDocument(fakeUserDoc);

		assertNotNull(player);
		assertEquals("ExistingPlayer", player.getNickname());
		assertEquals(150, player.getCoins());
		assertEquals(5, player.getWins());
		assertEquals(2, player.getLosses());
		assertEquals(50, player.getCollection().getDust());
		assertEquals(2, player.getCollection().getCardQuantity(101));
		assertEquals(1, player.getDecks().size());
	}

	@Test
	@DisplayName("parseCollectionFromDocument: restituisce null se il campo cards manca o ha tipo errato")
	void testParseCollectionCorruptedDocument() {
		Document missingCardsDoc = new Document().append("dust", 50);
		Document wrongTypeCardsDoc = new Document().append("dust", 50).append("cards", "invalid");
		Document wrongTypeDustDoc = new Document().append("dust", "nonUnNumero").append("cards", new Document());

		assertNull(documentMapper.parseCollectionFromDocument(missingCardsDoc));
		assertNull(documentMapper.parseCollectionFromDocument(wrongTypeCardsDoc));
		assertNull(documentMapper.parseCollectionFromDocument(wrongTypeDustDoc));
	}

	@Test
	@DisplayName("parseCollectionFromDocument: recupera con successo la collezione")
	void testParseCollectionSuccess() {
		Document fakeCardsDoc = new Document().append("101", 2).append("102", 1);
		Document fakeCollectionDoc = new Document().append("dust", 75).append("cards", fakeCardsDoc);

		Collection collection = documentMapper.parseCollectionFromDocument(fakeCollectionDoc);

		assertNotNull(collection);
		assertEquals(75, collection.getDust());
		assertEquals(2, collection.getCardQuantity(101));
		assertEquals(1, collection.getCardQuantity(102));
	}

}