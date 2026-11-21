package thearchis.common.card;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import thearchis.common.game.card.CardRarity;
import thearchis.common.game.card.CardType;
import thearchis.common.game.card.Creature;
import thearchis.common.game.card.CreatureSubtype;
import thearchis.common.game.card.Spell;
import thearchis.common.game.hero.HeroName;

import static org.junit.jupiter.api.Assertions.*;

class CardConstructorTest {

	@Test
	@DisplayName("Costruttore Spell: accetta dati validi")
	void testValidSpellCreation() {
		Spell spell = new Spell(1, "Fulmine", 2, CardType.SPELL, CardRarity.COMMON, HeroName.NONE, "Danno", true);
		assertEquals(1, spell.getId());
		assertTrue(spell.isConstrained());
	}

	@Test
	@DisplayName("Costruttore Creature: accetta dati validi")
	void testValidCreatureCreation() {
		Creature creature = new Creature(2, "Scudiero", 0, CardType.CREATURE, CardRarity.COMMON, HeroName.AETHELGARD,
				"Difensore", false, CreatureSubtype.HUMAN, 0, 1);
		assertEquals(2, creature.getId());
		assertFalse(creature.isConstrained());
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per id < 1")
	void testInvalidIdThrowsException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(0, "Palla di Fuoco", 3, CardType.SPELL, CardRarity.RARE, HeroName.NONE, "Desc", true));
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per name null o vuoto")
	void testInvalidNameThrowsException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, null, 3, CardType.SPELL, CardRarity.RARE, HeroName.NONE, "Desc", true));
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "   ", 3, CardType.SPELL, CardRarity.RARE, HeroName.NONE, "Desc", true));
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per manaCost fuori range")
	void testInvalidManaCostThrowsException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", -1, CardType.SPELL, CardRarity.EPIC, HeroName.NONE, "Desc", false));
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", 15, CardType.SPELL, CardRarity.EPIC, HeroName.NONE, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card: lancia IllegalArgumentException per type null o errato")
	void testInvalidCardTypeException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", 1, null, CardRarity.EPIC, HeroName.NONE, "Desc", false));
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.CREATURE, CardRarity.EPIC, HeroName.NONE, "Desc", false));
		assertThrows(IllegalArgumentException.class, () -> new Creature(2, "Guerriero", 2, CardType.SPELL,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, CreatureSubtype.HUMAN, 2, 2));
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per rarity null")
	void testInvalidRarityException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.SPELL, null, HeroName.NONE, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per hero null")
	void testInvalidHeroException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.SPELL, CardRarity.COMMON, null, "Desc", false));
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per description null")
	void testInvalidDescriptionException() {
		assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Super Spell", 1, CardType.SPELL, CardRarity.COMMON, HeroName.NONE, null, false));
	}

	@Test
	@DisplayName("Costruttore Card: IllegalArgumentException per constrained null")
	void testConstrainedNullThrowsException() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> new Spell(1, "Palla di Fuoco", 3, CardType.SPELL, CardRarity.RARE, HeroName.NONE, "Desc", null));

		assertTrue(ex.getMessage().contains("constrained"));
	}

	@Test
	@DisplayName("Costruttore Creature: IllegalArgumentException per subtype null")
	void testInvalidSubtypeThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> new Creature(2, "Guerriero", 2, CardType.CREATURE,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, null, 2, 2));
	}

	@Test
	@DisplayName("Costruttore Creature: IllegalArgumentException per difesa <= 0")
	void testInvalidDefenseThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> new Creature(2, "Guerriero", 2, CardType.CREATURE,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, CreatureSubtype.HUMAN, 2, 0));
	}

	@Test
	@DisplayName("Costruttore Creature: IllegalArgumentException per attacco < 0")
	void testNegativeAttackThrowsException() {
		assertThrows(IllegalArgumentException.class, () -> new Creature(3, "Bestia", 2, CardType.CREATURE,
				CardRarity.COMMON, HeroName.NONE, "Desc", true, CreatureSubtype.BEAST, -1, 3));
	}

}
