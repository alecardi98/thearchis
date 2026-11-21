package thearchis.common.message;

import java.io.Serializable;

public class CollectionData implements Serializable {

	private static final long serialVersionUID = 1L;

	private int cardId;

	public CollectionData(int cardId) {
		if (cardId <= 0) {
			throw new IllegalArgumentException("CollectionData : cardId non può essere <= 0.");
		}
		this.cardId = cardId;
	}

	public CollectionData() {
		this.cardId = 0;
	}

	public int getCardId() {
		return cardId;
	}
}
