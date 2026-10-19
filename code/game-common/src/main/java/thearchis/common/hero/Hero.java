package thearchis.common.hero;

public class Hero {
	private final HeroName name; // indica la classe di appartenenza
	private final HeroPower power; // indica il potere eroe

	public Hero(HeroName name, HeroPower power) {

		if (name != HeroName.Aethelgard && name != HeroName.Brakkar && name != HeroName.Lyra && name != HeroName.Malakor
				&& name != HeroName.Generico) {
			throw new InvalidHeroDataException(name, "name");
		} else {
			this.name = name;
		}

		this.power = power;
	}
}
