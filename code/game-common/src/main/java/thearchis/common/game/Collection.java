package thearchis.common.game;

import java.io.Serializable;
import java.util.Map;

public class Collection implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Map<Integer, Integer> cards; // cardId, quantity
	private int dust;

	public Collection(Map<Integer, Integer> cards, int dust) {
		if (cards == null) {
			throw new IllegalArgumentException("Collection : cards non può essere null.");
		}
		this.cards = cards;

		if (dust < 0) {
			throw new IllegalArgumentException("Collection : dust non può essere < 0.");
		}
		this.dust = dust;
	}

	public void addCard(int cardId) {
		this.cards.put(cardId, cards.getOrDefault(cardId, 0) + 1);
	}

	public boolean removeCard(int cardId) {
		int currentCount = cards.getOrDefault(cardId, 0);

		if (currentCount <= 0) {
			return false;
		}

		if (currentCount == 1) {
			cards.remove(cardId);
		} else {
			cards.put(cardId, currentCount - 1);
		}

		return true;
	}

	public int getCardQuantity(int cardId) {
		return cards.getOrDefault(cardId, 0);
	}

	public int getDust() {
		return dust;
	}
}