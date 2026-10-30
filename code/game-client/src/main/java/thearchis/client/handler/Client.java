package thearchis.client.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private final AtomicBoolean isDisconnecting = new AtomicBoolean(false);

	private String pendingNickname;
	private String nickname;

	private static final String HOST = "localhost";
//	private static final String HOST = "204.216.208.188";
	private static final int PORT = 443;

	public Client() {
	}

	public void startClient() {
		serverConnection();
	}

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
			logger.error("Impossibile connettersi al server, chiusura risorse ({}).", e.getMessage());
			// Chiusura di sicurezza se il socket si era aperto ma gli stream sono falliti
			if (socket != null && !socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException ex) {
					logger.debug("Errore durante la chiusura del socket post-fallimento: {}", ex.getMessage());
				}
			}
			System.exit(1);
		}
	}

	public void serverDisconnection() {
		if (!isDisconnecting.compareAndSet(false, true)) {
			return;
		}

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
		if (out == null || socket == null || socket.isClosed() || !socket.isConnected()) {
			logger.warn("Impossibile inviare il messaggio '{}': socket chiuso o non connesso.",
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

	public synchronized void sendRegister(String nickname, String password) {
		Message msg = new Message(MessageType.REGISTER_REQUEST, new UserData(nickname, password));
		sendMessage(msg);
	}

	public synchronized void sendLogin(String nickname, String password) {
		pendingNickname = nickname;
		Message msg = new Message(MessageType.LOGIN_REQUEST, new UserData(nickname, password));
		sendMessage(msg);
	}

	public synchronized void confirmLogin() {
		if (pendingNickname != null) {
			nickname = pendingNickname;
			pendingNickname = null;
			logger.info("Sessione salvata con successo per '{}'.", nickname);
		}
	}

	public synchronized void clearPendingLogin() {
		pendingNickname = null;
	}

	/*
	 * Getter & Setter
	 */

	public String getPendingNickname() {
		return pendingNickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}