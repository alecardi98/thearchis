package thearchis.common.card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import thearchis.common.hero.HeroName;

import static org.junit.jupiter.api.Assertions.*;

class CardConstructorTest {

	@Test
	@DisplayName("Costruttore Card accetta dati validi per una Spell")
	void testValidSpellCreation() {
		Spell spell = new Spell(1, "Fulmine", 2, CardType.SPELL, CardRarity.COMMON, HeroName.NONE, "Danno", true);
		assertEquals(1, spell.getId());
		assertTrue(spell.isConstrained());
	}

	@Test
	@DisplayName("Costruttore Card lancia InvalidCardDataException per ID <= 0")
	void testInvalidIdThrowsException() {
		assertThrows(InvalidCardDataException.class,
				() -> new Spell(0, "Palla di Fuoco", 3, CardType.SPELL, CardRarity.RARE, HeroName.NONE, "Desc", true));
	}

	@Test
	@DisplayName("Costruttore Card lancia InvalidCardDataException per manaCost fuori range")
	void testInvalidManaCostThrowsException() {
		assertThrows(InvalidCardDataException.class,
				() -> new Spell(1, "Super Spell", 15, CardType.SPELL, CardRarity.EPIC, HeroName.NONE, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card lancia InvalidCardDataException per type non valido")
	void testInvalidCardTypeException() {
		assertThrows(InvalidCardDataException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.CREATURE, CardRarity.EPIC, HeroName.NONE, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card lancia InvalidCardDataException per rarity non valida")
	void testInvalidRarityException() {
		assertThrows(InvalidCardDataException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.SPELL, null, HeroName.NONE, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card lancia InvalidCardDataException per hero non valido")
	void testInvalidHeroException() {
		assertThrows(InvalidCardDataException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.SPELL, CardRarity.COMMON, null, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card lancia InvalidCardDataException se constrained è null")
	void testConstrainedNullThrowsException() {
		InvalidCardDataException ex = assertThrows(InvalidCardDataException.class,
				() -> new Spell(1, "Palla di Fuoco", 3, CardType.SPELL, CardRarity.RARE, HeroName.NONE, "Desc", null));

		assertTrue(ex.getMessage().contains("constrained"));
	}

	@Test
	@DisplayName("Costruttore Creature lancia InvalidCardDataException per subtype non valido.")
	void testInvalidSubtypeThrowsException() {
		assertThrows(InvalidCardDataException.class, () -> new Creature(2, "Guerriero", 2, CardType.CREATURE,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, null, 2, 0));
	}

	@Test
	@DisplayName("Costruttore Creature lancia InvalidCardDataException per difesa <= 0")
	void testInvalidDefenseThrowsException() {
		assertThrows(InvalidCardDataException.class, () -> new Creature(2, "Guerriero", 2, CardType.CREATURE,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, CreatureSubtype.HUMAN, 2, 0));
	}

	@Test
	@DisplayName("Costruttore Creature lancia InvalidCardDataException per attacco < 0")
	void testNegativeAttackThrowsException() {
		assertThrows(InvalidCardDataException.class, () -> new Creature(3, "Bestia", 2, CardType.CREATURE,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, CreatureSubtype.BEAST, -1, 3));
	}
}