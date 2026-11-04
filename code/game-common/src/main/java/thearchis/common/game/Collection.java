package thearchis.common.game;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Collection implements Serializable {

	private final Map<Integer, Integer> cards = new HashMap<>(); // cardId, quantity

	public Collection() {
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

}