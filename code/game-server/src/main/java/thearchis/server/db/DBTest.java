package thearchis.server.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;

public class DBTest {

	public static void main(String[] args) {

		// Inizializza il database
		MongoDatabase db = DBManager.getDatabase();
		MongoCollection<Document> players = db.getCollection("players");

		// Inserimento
		Document newPlayer = new Document("username", "Ardon").append("level", 1).append("rankPoints", 1000);

		players.insertOne(newPlayer);
		System.out.println("Nuovo giocatore salvato!");

		// Ricerca
		Document player = players.find(eq("username", "Ardon")).first();
		if (player != null) {
			System.out.println("Livello giocatore: " + player.getInteger("level"));
		}

		
		DBManager.close(); // Alla chiusura del server

	}

}