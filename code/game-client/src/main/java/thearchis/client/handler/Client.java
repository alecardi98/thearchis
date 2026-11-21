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
import thearchis.common.message.BackNavigationData;
import thearchis.common.game.state.ClientState;
import thearchis.common.game.state.Player;
import thearchis.common.message.AuthenticationData;
import thearchis.common.message.CollectionData;

public class Client {
	private static final Logger logger = LogManager.getLogger("[Client]");
	private ObjectOutputStream out;
	private ObjectInputStream in;
	private Socket socket;
	private ServerListener serverListener;
	private final AtomicBoolean isDisconnecting = new AtomicBoolean(false);

	private Player player;

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
			logger.info("serverConnection : Client connesso al server.");

			out = new ObjectOutputStream(socket.getOutputStream());
			out.flush();
			in = new ObjectInputStream(socket.getInputStream());

			serverListener = new ServerListener(in, this);
			new Thread(serverListener, "ServerListener-Thread").start();
		} catch (IOException e) {
			logger.error("serverConnection : Impossibile connettersi al server, chiusura risorse ({}).",
					e.getMessage());
			// Chiusura di sicurezza se il socket si era aperto ma gli stream sono falliti
			if (socket != null && !socket.isClosed()) {
				try {
					socket.close();
				} catch (IOException ex) {
					logger.debug("serverConnection : Errore durante la chiusura del socket post-fallimento ({}).",
							ex.getMessage());
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

			logger.info("serverDisconnection : Client disconnesso dal server con successo.");
		} catch (IOException e) {
			logger.error("serverDisconnection : Errore durante la chiusura delle risorse di rete ({}).",
					e.getMessage());
		}
	}

	public synchronized void sendMessage(Object msg) {
		if (msg == null) {
			logger.warn("sendMessage : Impossibile inviare il messaggio (messaggio null).");
			return;
		}

		if (out == null || socket == null || socket.isClosed() || !socket.isConnected()) {
			logger.warn("sendMessage : Impossibile inviare il messaggio al server (socket chiuso o non connesso).");
			return;
		}

		try {
			out.writeObject(msg);
			out.flush();
			logger.debug("sendMessage : Messaggio inviato con successo.");
		} catch (IOException e) {
			logger.error("sendMessage : Errore di I/O durante l'invio del messaggio ({}).", e.getMessage());
		}
	}

	public synchronized void sendRegisterRequest(String nickname, String password) {
		if (nickname == null || password == null || nickname.isBlank() || password.isBlank()) {
			logger.error(
					"sendRegisterRequest : Errore nell'invio della richiesta di registrazione (credenziali non valide).");
			return;
		}
		AuthenticationData authenticationData = new AuthenticationData(nickname, password);
		Message msg = new Message(MessageType.REGISTER_REQUEST, authenticationData);
		sendMessage(msg);
	}

	public synchronized void sendLoginRequest(String nickname, String password) {
		if (nickname == null || password == null || nickname.isBlank() || password.isBlank()) {
			logger.error("sendLoginRequest : Errore nell'invio della richiesta di login (credenziali non valide).");
			return;
		}
		AuthenticationData authenticationData = new AuthenticationData(nickname, password);
		Message msg = new Message(MessageType.LOGIN_REQUEST, authenticationData);
		sendMessage(msg);
	}

	public synchronized void sendCollectionRequest() {
		CollectionData collectionData = new CollectionData();
		Message msg = new Message(MessageType.COLLECTION_REQUEST, collectionData);
		sendMessage(msg);
	}

	public synchronized void sendCraftRequest(int cardId) {
		if (cardId <= 0) {
			logger.error("sendCraftRequest : Errore nell'invio della richiesta di craft (cardId non valido).");
			return;
		}
		CollectionData collectionData = new CollectionData(cardId);
		Message msg = new Message(MessageType.CRAFT_REQUEST, collectionData);
		sendMessage(msg);
	}

	public synchronized void sendDisenchantRequest(int cardId) {
		if (cardId <= 0) {
			logger.error(
					"sendDisenchantRequest : Errore nell'invio della richiesta di disenchant (cardId non valido).");
			return;
		}
		CollectionData collectionData = new CollectionData(cardId);
		Message msg = new Message(MessageType.DISENCHANT_REQUEST, collectionData);
		sendMessage(msg);
	}

	public synchronized void sendBackNavigationRequest(ClientState destination) {
		if (destination == null) {
			logger.error(
					"sendBackNavigatioRequest : Errore nell'invio della richiesta di backnavigation (destination null).");
			return;
		}
		BackNavigationData backNavigationData = new BackNavigationData(destination);
		Message msg = new Message(MessageType.BACKNAVIGATION_REQUEST, backNavigationData);
		sendMessage(msg);
	}

	/*
	 * Getter & Setter
	 */
	public void setPlayer(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return this.player;
	}
}