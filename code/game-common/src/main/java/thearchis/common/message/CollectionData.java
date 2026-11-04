package thearchis.common.message;

import java.io.Serializable;

public class CollectionData implements Serializable {

	private int cardId;

	public CollectionData(int cardId) {
		this.cardId = cardId;
	}

	public CollectionData() {
		this.cardId = 0;
	}

	public int getCardId() {
		return cardId;
	}
}
