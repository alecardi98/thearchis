package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Client {
	private static final Logger logger = LogManager.getLogger(Client.class);
	private ObjectOutputStream out;
	private ServerListener serverListener;
	private Socket socket;

	private static final String HOST = "localhost";
//	private static final String HOST = "204.216.208.188";
	private static final int PORT = 443;

	public Client() {
	}

	/*
	 * Metodo per avviare il client
	 */
	public void startClient() {
		serverConnection();
	}

	/*
	 * Metodo con il quale il client si connette al server
	 */
	private void serverConnection() {
		ObjectInputStream in;
		try {
			socket = new Socket(HOST, PORT);
			logger.info("Client connesso al server");
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			serverListener = new ServerListener(in, this);
			new Thread(serverListener).start();
		} catch (IOException e) {
			logger.error("ERRORE! Impossibile connettersi al server. Disconnessione.");
			System.exit(1);
		}
	}

	/*
	 * Metodo con il quale il client si disconnette dal server
	 */
	public void serverDisconnection() {
		try {
			out.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Disconnessione riuscita.");
	}

	/*
	 * Metodo per inviare messaggi
	 */
	public void sendMessage(Object msg) {
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
