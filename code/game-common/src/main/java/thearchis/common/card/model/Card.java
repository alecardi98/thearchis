package thearchis.common.card.model;

import thearchis.hero.HeroName;

public class Card {

	private final int id;
	private final String name;
	private final int cost; // costo in mana
	private final CardType tipo;
	private final Rarity rarità;
	private final HeroName eroe; // classe di appartenenza

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
			this.tipo = tipo;
		}

		if (rarità != Rarity.Comune && rarità != Rarity.Rara && rarità != Rarity.Epica
				&& rarità != Rarity.Leggendaria) {
			throw new InvalidCardDataException(id, "rarity");
		} else {
			this.rarità = rarità;
		}

		this.eroe = eroe;
	}

}
