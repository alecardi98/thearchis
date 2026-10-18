package thearchis.hero;

public class InvalidHeroDataException extends RuntimeException {
	public InvalidHeroDataException(HeroName eroe, String message) {
		super("Dati eroe non validi [nome: " + eroe + "]: " + message);
	}
}
