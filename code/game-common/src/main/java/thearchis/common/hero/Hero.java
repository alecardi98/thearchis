package thearchis.common.hero;

public class Hero {
	private final HeroName name; // indica la classe di appartenenza
	private final HeroPower power; // indica il potere eroe

	public Hero(HeroName name, HeroPower power) {

		if (name != HeroName.AETHELGARD && name != HeroName.BRAKKAR && name != HeroName.LYRA && name != HeroName.MALAKOR
				&& name != HeroName.NONE) {
			throw new InvalidHeroDataException(name, "name");
		} else {
			this.name = name;
		}

		this.power = power;
	}
}
