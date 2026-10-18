package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class ServerListener implements Runnable {

	private static final Logger logger = LogManager.getLogger(ServerListener.class);
	private ObjectInputStream in;
	private Client client;

	public ServerListener(ObjectInputStream in, Client client) {
		this.in = in;
		this.client = client;
	}

	@Override
	public void run() {
		try {
			Object obj;
			while (true) {
				obj = in.readObject();
				//gestione del pacchetto inviato dal server
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.error("ERRORE! Comunicazione con il Server persa.");
			cleanUp();
		}
	}

	/*
	 * Metodo che chiude correttamente il ServerListener
	 */
	private void cleanUp() {
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		client.serverDisconnection();
	}


}
