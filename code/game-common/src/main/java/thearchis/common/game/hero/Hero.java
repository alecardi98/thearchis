package thearchis.common.game.hero;

public class Hero {

	private final HeroName name;
	private final HeroPower power;

	public Hero(HeroName name, HeroPower power) {
		if (name == null) {
			throw new IllegalArgumentException("Hero : name non può essere null.");
		}
		this.name = name;

		if (power == null) {
			throw new IllegalArgumentException("Hero : power non può essere null.");
		}
		this.power = power;
	}

}
