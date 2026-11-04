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

	private static final Logger logger = LogManager.getLogger(ClientHandler.class);

	private final Socket socket;
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private final int clientId;
	private ClientState clientState;
	private Player player;

	public ClientHandler(Socket socket, int clientId) {
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
					logger.warn("Client #{} : oggetto non riconosciuto ricevuto dal socket.", clientId);
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
			Server.getInstance().removeClient(clientId);
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
			logger.error("Client #{} : errore I/O durante l'invio del pacchetto a client #{}.", clientId,
					e.getMessage());
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
		this.player = player;
	}

	public ClientState getClientState() {
		return clientState;
	}

	public void setClientState(ClientState clientState) {
		this.clientState = clientState;
	}
}