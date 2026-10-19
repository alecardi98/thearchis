package thearchis.server.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientHandler implements Runnable {

	private static final Logger logger = LogManager.getLogger(ClientHandler.class);

	private final Socket socket; // socket dedicato al client
	private final int clientId; // L'id numerico passato dal Server

	private ObjectOutputStream out; // per mandare messaggi
	private ObjectInputStream in; // per ricevere messaggi

	public ClientHandler(Socket socket, int clientId) {
		this.socket = socket;
		this.clientId = clientId;
	}

	public void init() throws IOException {
		out = new ObjectOutputStream(socket.getOutputStream()); // apertura canale di uscita pacchetti
		out.flush(); // pulizia canale
		in = new ObjectInputStream(socket.getInputStream()); // apertura canale di entrata pacchetti
	}

	@Override
	public void run() {
		try {
			Object obj;
			// ciclo per ascoltare e gestire i messaggi
			while ((obj = in.readObject()) != null) {
				// TODO: Cast a Message e logica di smistamento (ActionType / ControlType)
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.info("Connessione interrotta con il Client #" + clientId);
		} finally {
			cleanUp(); // Eseguito sempre, sia per disconnessione volontaria che per errore
		}
	}

	public void cleanUp() {
		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException e) {
			logger.error("Errore durante la chiusura del socket del Client #" + clientId);
		}
		Server.getServer().removeClient(clientId);
	}

	public synchronized void sendMessage(Object msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			logger.error("Errore invio messaggio al Client #" + clientId);
		}
	}
}