package thearchis.common.card;

import java.io.Serializable;

import thearchis.common.hero.HeroName;

public class Card implements Serializable {

	private final int id;
	private final String name;
	private final int manaCost;
	private final CardType type;
	private final CardRarity rarity;
	private final HeroName hero;
	private final String description;
	private final boolean constrained;

	public Card(int id, String name, int cost, CardType type, CardRarity rarity, HeroName hero, String description, boolean constrained) {
		this.id = id;
		this.name = name;

		if (cost < 0 || cost > 10) {
			throw new InvalidCardDataException(id, "cost");
		} else {
			this.manaCost = cost;
		}

		if (type != CardType.CREATURE && type != CardType.SPELL) {
			throw new InvalidCardDataException(id, "type");
		} else {
			this.type = type;
		}

		if (rarity != CardRarity.COMMON && rarity != CardRarity.RARE && rarity != CardRarity.EPIC
				&& rarity != CardRarity.LEGENDARY) {
			throw new InvalidCardDataException(id, "rarity");
		} else {
			this.rarity = rarity;
		}

		this.hero = hero;
		this.description = description;
		this.constrained = constrained;
	}

	/*
	 * Getter & Setter
	 */
	public int getCraftCost() {
		return rarity != null ? rarity.getCraftCost() : 0;
	}

	public int getDisenchantValue() {
		return rarity != null ? rarity.getDisenchantValue() : 0;
	}

	public int getId() {
		return id;
	}
	
	public boolean isConstrained() {
		return constrained;
	}

}
