package thearchis.server.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import thearchis.common.message.Message;

public class ClientHandler implements Runnable {

	private static final Logger logger = LogManager.getLogger(ClientHandler.class);

	private final Socket socket; // socket con il server
	private final int clientId;
	private boolean authenticated = false; // flag per garantire autenticazione al server

	private ObjectOutputStream out;
	private ObjectInputStream in;

	public ClientHandler(Socket socket, int clientId) {
		this.socket = socket;
		this.clientId = clientId;
	}

	/*
	 * Creazione flusso in e out
	 */
	public void init() throws IOException {
		out = new ObjectOutputStream(socket.getOutputStream());
		out.flush();
		in = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void run() {
		try {
			Object obj;
			// gestione dei pacchetti in entrata delegata al server
			while ((obj = in.readObject()) != null) {
				Message msg = (Message) obj;

				switch (msg.getType()) {
				case REGISTER_REQUEST -> Server.getServer().handleRegister(clientId, msg);
				default -> logger.error("Tipo di messaggio sconosciuto: {}", msg.getType());
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.info("Client #{} : connessione interrotta con il client.", clientId);
		} finally {
			cleanUp();
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
			logger.debug("Client #{} : socket del client già chiuso o non raggiungibile.", clientId);
		} finally {
			Server.getServer().removeClient(clientId);
		}
	}

	public synchronized void sendMessage(Object msg) {
		if (out == null) {
			logger.error("Client #{} : impossibile inviare messaggio al client (ObjectOutputStream non inizializzato).",
					clientId);
			return;
		}
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			logger.error("Client #{} : errore I/O durante l'invio del pacchetto '{}' al client.",
					msg != null ? msg.getClass().getSimpleName() : "null", clientId, e.getMessage());
		}
	}

}