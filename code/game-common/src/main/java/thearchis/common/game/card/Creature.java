package thearchis.common.game.card;

import thearchis.common.game.hero.HeroName;

public class Creature extends Card {

	private static final long serialVersionUID = 1L;

	private static final int MINIMUM_ATK = 0;
	private static final int MINIMUM_DEF = 1;

	private final CreatureSubtype subtype;
	private boolean summoningSickness;
	private int atk;
	private int def;

	public Creature(int id, String name, int manaCost, CardType type, CardRarity rarity, HeroName hero,
			String description, boolean constrained, CreatureSubtype subtype, int atk, int def) {
		super(id, name, manaCost, type, rarity, hero, description, constrained);

		if (type != CardType.CREATURE) {
			throw new IllegalArgumentException(
					"Card : type atteso " + CardType.CREATURE + " per un'istanza di " + this.getClass() + ".");
		}

		if (subtype == null) {
			throw new IllegalArgumentException("Card : subtype non può essere null.");
		}
		this.subtype = subtype;

		summoningSickness = true;

		if (atk < MINIMUM_ATK) {
			throw new IllegalArgumentException("Card : atk non può essere < " + MINIMUM_ATK + ".");
		}
		this.atk = atk;

		if (def < MINIMUM_DEF) {
			throw new IllegalArgumentException("Card : def non può essere < " + MINIMUM_DEF + ".");
		}
		this.def = def;
	}

}
