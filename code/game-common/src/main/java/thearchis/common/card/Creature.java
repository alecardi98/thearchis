package thearchis.common.card;

import thearchis.common.hero.HeroName;

public class Creature extends Card {

	private final CreatureSubtype subtype; // sottotipo della creatura
	private boolean summoningSickness; // handicap di evocazione
	private int atk; // valore di attacco della creatura
	private int def; // valore di difesa della creatura

	public Creature(int id, String name, int manaCost, CardType type, CardRarity rarity, HeroName hero,
			String description, boolean constrained, CreatureSubtype subtype, int atk, int def) {
		super(id, name, manaCost, type, rarity, hero, description, constrained);

		if (type != CardType.CREATURE) {
			throw new InvalidCardDataException(id, "type mismatch: atteso CREATURE per un'istanza di Creature");
		}

		summoningSickness = true;

		if (subtype != CreatureSubtype.BEAST && subtype != CreatureSubtype.DEMON && subtype != CreatureSubtype.GOD
				&& subtype != CreatureSubtype.HUMAN && subtype != CreatureSubtype.NONE) {
			throw new InvalidCardDataException(id, "subtype");
		} else {
			this.subtype = subtype;
		}

		if (atk < 0) {
			throw new InvalidCardDataException(id, "attack");
		} else {
			this.atk = atk;
		}

		if (def <= 0) {
			throw new InvalidCardDataException(id, "defense");
		} else {
			this.def = def;
		}

	}

}
