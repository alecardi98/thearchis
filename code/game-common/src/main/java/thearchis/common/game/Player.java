package thearchis.common.game;

import java.io.Serializable;

public class Player implements Serializable {

	private String nickname;
	private int coins;
	private int dust;
	private Collection collection;
	// private List<Deck> decks = new List<>();
	private int wins;
	private int losses;

	public Player(String nickname, int coins, int dust, Collection collection, int wins, int losses) {
		this.nickname = nickname;
		this.coins = coins;
		this.dust = dust;
		this.collection = collection;
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
	
	public int getDust() {
		return dust;
	}
}
