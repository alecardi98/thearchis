package thearchis.common.deck;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import thearchis.common.hero.HeroName;

public class Deck implements Serializable {

	private final HeroName hero;
	private boolean battleDeck;
	private final Map<Integer, Integer> cards = new HashMap<>(); // cardId,quantity

	public Deck(HeroName hero) {
		this.hero = hero;
		//TODO selezione del mazzo di battaglia
	}

}