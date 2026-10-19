package thearchis.common.card;

import thearchis.common.hero.HeroName;

public class Creature extends Card {

	private final CreatureSubtype sottotipo; // sottotipo della creatura
	private boolean summoningSickness; // handicap di evocazione
	private int atk; // valore di attacco della creatura
	private int def; // valore di difesa della creatura

	public Creature(int id, String name, int cost, CardType tipo, CreatureSubtype sottotipo, Rarity rarità, int atk,
			int def, HeroName eroe) {
		super(id, name, cost, tipo, rarità, eroe);

		summoningSickness = true;

		if (sottotipo != CreatureSubtype.Bestia && sottotipo != CreatureSubtype.Demone
				&& sottotipo != CreatureSubtype.Divino && sottotipo != CreatureSubtype.Umano
				&& sottotipo != CreatureSubtype.Nessuno) {
			throw new InvalidCardDataException(id, "subtype");
		} else {
			this.sottotipo = sottotipo;
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
