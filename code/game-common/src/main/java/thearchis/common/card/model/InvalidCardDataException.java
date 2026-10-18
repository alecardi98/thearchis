package thearchis.common.card.model;

public class InvalidCardDataException extends RuntimeException {

    public InvalidCardDataException(int cardId, String message) {
        super("Dati carta non validi [id: " + cardId + "]: " + message);
    }
}
