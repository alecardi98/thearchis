package thearchis.common.game;

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

	public void addCard(int cardId, int quantity) {
	    if (quantity > 0) {
	        cards.put(cardId, cards.getOrDefault(cardId, 0) + quantity);
	    }
	}
}