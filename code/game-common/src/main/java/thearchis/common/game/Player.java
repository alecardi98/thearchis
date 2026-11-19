package thearchis.common.game;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Player implements Serializable {

	private String nickname;
	private int coins;
	private Collection collection;
	private List<Deck> decks;
	private int wins;
	private int losses;

	public Player(String nickname, int coins, Collection collection, List<Deck> decks, int wins, int losses) {
		this.nickname = nickname;
		this.coins = coins;
		this.collection = collection;
		this.decks = (decks != null) ? decks : new ArrayList<>();
		this.wins = wins;
		this.losses = losses;
	}

	/*
	 * Getter & Setter
	 */

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public void setCollection(Collection collection) {
		this.collection = collection;
	}

	public int getCoins() {
		return coins;
	}

	public Collection getCollection() {
		return collection;
	}
}
