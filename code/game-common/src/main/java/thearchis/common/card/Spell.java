package thearchis.common.card;

import thearchis.common.hero.HeroName;

public class Spell extends Card {

	private static final long serialVersionUID = 1L;

	public Spell(int id, String name, int manaCost, CardType type, CardRarity rarity, HeroName hero, String description,
			Boolean constrained) {
		super(id, name, manaCost, type, rarity, hero, description, constrained);

		if (type != CardType.SPELL) {
			throw new IllegalArgumentException(
					"Card : type atteso " + CardType.SPELL + " per un'istanza di " + this.getClass() + ".");
		}
	}

}
