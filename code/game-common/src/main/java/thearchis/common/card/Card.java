package thearchis.common.card;

import java.io.Serializable;

import thearchis.common.hero.HeroName;

public class Card implements Serializable {

	private final int id;
	private final String name;
	private final int cost;
	private final CardType type;
	private final Rarity rarity;
	private final HeroName hero;

	public Card(int id, String name, int cost, CardType tipo, Rarity rarità, HeroName eroe) {
		this.id = id;
		this.name = name;

		if (cost < 0 || cost > 10) {
			throw new InvalidCardDataException(id, "cost");
		} else {
			this.cost = cost;
		}

		if (tipo != CardType.Creatura && tipo != CardType.Magia) {
			throw new InvalidCardDataException(id, "type");
		} else {
			this.type = tipo;
		}

		if (rarità != Rarity.Comune && rarità != Rarity.Rara && rarità != Rarity.Epica
				&& rarità != Rarity.Leggendaria) {
			throw new InvalidCardDataException(id, "rarity");
		} else {
			this.rarity = rarità;
		}

		this.hero = eroe;
	}

}
