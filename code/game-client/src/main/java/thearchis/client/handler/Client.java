package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.UserData;

public class Client {
	private static final Logger logger = LogManager.getLogger(Client.class);
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Socket socket;
	private ServerListener serverListener;
	private boolean isDisconnecting = false;

	private static final String HOST = "localhost";
//	private static final String HOST = "204.216.208.188";
	private static final int PORT = 443;

	public Client() {
	}

	/*
	 * Avvia il client
	 */
	public void startClient() {
		serverConnection();
	}

	/*
	 * Il client si connette al server (inizializza in, out, socket e serverlistener)
	 */
	private void serverConnection() {
		try {
			socket = new Socket(HOST, PORT);
			logger.info("Client connesso al server");

			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(socket.getInputStream());

			serverListener = new ServerListener(in, this);
			new Thread(serverListener, "ServerListener-Thread").start();
		} catch (IOException e) {
			logger.error("Impossibile connettersi al server. Disconnessione.");
			System.exit(1);
		}
	}

	/*
	 * Il client si disconnette dal server (chiude in, out e socket)
	 */
	public synchronized void serverDisconnection() {
		if (isDisconnecting) {
			return; // Evita esecuzioni doppie o ricorsive
		}
		isDisconnecting = true;

		if (serverListener != null) {
			serverListener.stopListener();
		}

		try {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null && !socket.isClosed())
				socket.close();

			logger.info("Disconnessione dal server completata con successo.");
		} catch (IOException e) {
			logger.error("Errore durante la chiusura delle risorse di rete: {}", e.getMessage());
		}
	}

	public synchronized void sendMessage(Object msg) {
		if (out == null) {
			logger.error(
					"Impossibile inviare il messaggio '{}': lo stream di output non è inizializzato (Client non connesso).",
					msg != null ? msg.getClass().getSimpleName() : "null");
			return;
		}
		try {
			out.writeObject(msg);
			out.flush();
			logger.debug("Messaggio inviato con successo: {}", msg != null ? msg.getClass().getSimpleName() : "null");
		} catch (IOException e) {
			logger.error("Errore di I/O durante l'invio del messaggio di tipo '{}': {}",
					msg != null ? msg.getClass().getSimpleName() : "null", e.getMessage());
		}
	}

	/*
	 * Invio della richiesta di registrazione account
	 */
	public synchronized void sendRegister(String nickname, String password) {
		Message msg = new Message(MessageType.REGISTER_REQUEST, new UserData(nickname, password));
		sendMessage(msg);
	}
}