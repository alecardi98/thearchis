package thearchis.common.card;

import java.io.Serializable;

import thearchis.common.hero.HeroName;

public abstract class Card implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int MINIMUM_ID = 1;
	private static final int MINIMUM_MANA_COST = 0;
	private static final int MAXIMUM_MANA_COST = 10;

	private final int id;
	private final String name;
	private final int manaCost;
	private final CardType type;
	private final CardRarity rarity;
	private final HeroName hero;
	private final String description;
	private final boolean constrained;

	public Card(int id, String name, int manaCost, CardType type, CardRarity rarity, HeroName hero, String description,
			Boolean constrained) {
		if (id < MINIMUM_ID) {
			throw new IllegalArgumentException("Card : id non può essere < " + MINIMUM_ID + " (id : " + id + ").");
		}
		this.id = id;

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Card : name non può essere null o vuoto.");
		}
		this.name = name;

		if (manaCost < MINIMUM_MANA_COST || manaCost > MAXIMUM_MANA_COST) {
			throw new IllegalArgumentException("Card : manaCost non può essere < " + MINIMUM_MANA_COST + " o > "
					+ MAXIMUM_MANA_COST + " (manaCost : " + manaCost + ").");
		}
		this.manaCost = manaCost;

		if (type == null) {
			throw new IllegalArgumentException("Card : type non può essere null.");
		}
		this.type = type;

		if (rarity == null) {
			throw new IllegalArgumentException("Card : rarity non può essere null.");
		}
		this.rarity = rarity;

		if (hero == null) {
			throw new IllegalArgumentException("Card : hero non può essere null.");
		}
		this.hero = hero;

		if (description == null) {
			throw new IllegalArgumentException("Card : description non può essere null.");
		}
		this.description = description;

		if (constrained == null) {
			throw new IllegalArgumentException("Card : constrained non può essere null.");
		}
		this.constrained = constrained;
	}

	/*
	 * Getter & Setter
	 */
	public int getCraftCost() {
		return rarity.getCraftCost();
	}

	public int getDisenchantValue() {
		return rarity.getDisenchantValue();
	}

	public int getId() {
		return id;
	}

	public boolean isConstrained() {
		return constrained;
	}

}
