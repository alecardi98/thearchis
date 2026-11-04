package thearchis.common.card;

import thearchis.common.hero.HeroName;

public class Spell extends Card {

	public Spell(int id, String name, int manaCost, CardType type, CardRarity rarity, HeroName hero, String description,
			boolean constrained) {
		super(id, name, manaCost, type, rarity, hero, description, constrained);
	}

}
