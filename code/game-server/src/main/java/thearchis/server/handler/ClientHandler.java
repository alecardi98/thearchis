package thearchis.server.handler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.game.ClientState;
import thearchis.common.game.Player;
import thearchis.common.message.Message;

public class ClientHandler implements Runnable {

	private static final Logger logger = LogManager.getLogger("[ClientHandler]");

	private final Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private final int clientId;
	private ClientState clientState;
	private Player player;

	public ClientHandler(Socket socket, int clientId) {
		if (socket == null) {
			throw new IllegalArgumentException("ClientHandler : socket non può essere null.");
		}
		this.socket = socket;

		this.clientId = clientId;
		clientState = ClientState.NOT_AUTHENTICATED;
	}

	public void init() throws IOException {
		out = new ObjectOutputStream(socket.getOutputStream());
		out.flush();
		in = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void run() {
		try {
			Object obj;
			while ((obj = in.readObject()) != null) {
				if (obj instanceof Message msg) {
					MessageDispatcher.getInstance().dispatch(this, msg);
				} else {
					logger.warn("Client #{} : run : Oggetto non riconosciuto ricevuto dal socket.", clientId);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			logger.info("Client #{} : run : Connessione interrotta con il client.", clientId);
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
			logger.debug("Client #{} : cleanUp : Socket del client già chiuso o non raggiungibile.", clientId);
		} finally {
			Server.getInstance().removeClient(clientId);
		}
	}

	public synchronized void sendMessage(Object msg) {
		if (msg == null) {
			logger.error("Client #{} : sendMessage : Il messaggio da inviare al client è null.", clientId);
			return;
		}
		if (out == null) {
			logger.error(
					"Client #{} : sendMessage : Impossibile inviare messaggio al client (ObjectOutputStream non inizializzato).",
					clientId);
			return;
		}
		try {
			out.writeObject(msg);
			out.flush();
		} catch (IOException e) {
			logger.error("Client #{} : sendMessage : Errore I/O durante l'invio del pacchetto al client ({}).",
					clientId, e.getMessage());
		}
	}

	/*
	 * Getter & Setter
	 */
	public int getClientId() {
		return clientId;
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		if (player == null) {
			logger.error("Client #{} : setPlayer : Non può settare player null.", clientId);
			return;
		} else {
			this.player = player;
		}
	}

	public ClientState getClientState() {
		return clientState;
	}

	public void setClientState(ClientState clientState) {
		if (clientState == null) {
			logger.error("Client #{} : setClientState : Non può settare clientState null.", clientId);
			return;
		} else {
			this.clientState = clientState;
		}
	}
}