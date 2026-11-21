package thearchis.common.game.state;

import java.io.Serializable;
import java.util.List;

public class Player implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nickname;
	private int coins;
	private Collection collection;
	private List<Deck> decks;
	private int wins;
	private int losses;

	public Player(String nickname, int coins, Collection collection, List<Deck> decks, int wins, int losses) {
		if (nickname == null || nickname.isBlank()) {
			throw new IllegalArgumentException("Player : nickname non può essere null o vuoto.");
		}
		this.nickname = nickname;

		if (coins < 0) {
			throw new IllegalArgumentException("Player : coins non può essere < 0.");
		}
		this.coins = coins;

		if (collection == null) {
			throw new IllegalArgumentException("Player : collection non può essere null.");
		}
		this.collection = collection;

		if (decks == null) {
			throw new IllegalArgumentException("Player : decks non può essere null.");
		}
		this.decks = decks;

		if (wins < 0) {
			throw new IllegalArgumentException("Player : wins non può essere < 0.");
		}
		this.wins = wins;

		if (losses < 0) {
			throw new IllegalArgumentException("Player : losses non può essere < 0.");
		}
		this.losses = losses;
	}

	/*
	 * Getter & Setter
	 */

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			throw new IllegalArgumentException("setNickname : non può settare nickname null o vuoto.");
		}
		this.nickname = nickname;
	}

	public void setCollection(Collection collection) {
		if (collection == null) {
			throw new IllegalArgumentException("setCollection : non può settare collection null.");
		}
		this.collection = collection;
	}

	public int getCoins() {
		return coins;
	}

	public Collection getCollection() {
		return collection;
	}

	public List<Deck> getDecks() {
		return decks;
	}

	public int getWins() {
		return wins;
	}

	public int getLosses() {
		return losses;
	}
}
