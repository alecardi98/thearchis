package thearchis.common.game.state;

import java.io.Serializable;
import java.util.Map;

import thearchis.common.game.hero.HeroName;

public class Deck implements Serializable {

	private static final long serialVersionUID = 1L;

	private final HeroName hero;
	private boolean battleDeck; // TODO scelta del mazzo da battaglia
	private final Map<Integer, Integer> cards; // cardId,quantity

	public Deck(HeroName hero, Map<Integer, Integer> cards) {
		if (hero == null) {
			throw new IllegalArgumentException("Deck : hero non può essere null.");
		}
		this.hero = hero;

		if (cards == null) {
			throw new IllegalArgumentException("Deck : cards non può essere null.");
		}
		this.cards = cards;
	}

	// TODO creazione vera di un mazzo e testare
	public void addCard(int cardId, int quantity) {
		if (quantity > 0) {
			cards.put(cardId, cards.getOrDefault(cardId, 0) + quantity);
		}
	}

}