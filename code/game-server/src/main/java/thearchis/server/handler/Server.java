package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBStatus;
import thearchis.common.message.Message;
import thearchis.common.message.MessageType;
import thearchis.common.message.UserData;
import thearchis.server.db.DBManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server implements Runnable {

	private static final Logger logger = LogManager.getLogger(Server.class);
	private static Server server; // (Singleton)
	private final int port = 443;
	private ServerSocket serverSocket;
	private volatile boolean running = true;

	private DBManager dbManager;

	// mappa idclient,clienthandler
	private final ConcurrentMap<Integer, ClientHandler> activeClients = new ConcurrentHashMap<>();
	private final AtomicInteger idGenerator = new AtomicInteger(1);
	private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();

	private Server() {
	}

	@Override
	public void run() {
		dbConnection();
		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server in ascolto sulla porta {}: in attesa di connessioni...", port);

			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					logger.info("Nuova connessione da {} accettata.", clientSocket.getInetAddress());

					// libero il thread del server per gestire il client con un nuovo thread
					clientThreadPool.submit(() -> initializeClient(clientSocket));

				} catch (SocketException e) {
					if (!running) {
						logger.info("ServerSocket chiuso per arresto del server.");
						break;
					}
					logger.error("Errore di socket durante l'accept: {}", e.getMessage());
				} catch (IOException e) {
					logger.error("Errore I/O durante l'accept: {}", e.getMessage());
				}
			}
		} catch (IOException e) {
			logger.fatal("Impossibile aprire il ServerSocket sulla porta {}: {}", port, e.getMessage());
		} finally {
			stopServer();
		}
	}

	/*
	 * Connessione al database
	 */
	private void dbConnection() {
		dbManager = new DBManager();
		dbManager.connect();
	}

	private void initializeClient(Socket socket) {
		int clientId = idGenerator.getAndIncrement(); // assegnazione id con AtomicInteger
		ClientHandler handler = new ClientHandler(socket, clientId);

		try {
			handler.init();
			activeClients.put(clientId, handler); // mappo il nuovo clienthandler
			logger.info("Client #{} : collegato con successo.", clientId);

			clientThreadPool.submit(handler); // nuovo thread per il clientHandler

		} catch (IOException e) {
			logger.error("Client #{} : errore durante l'handshake ({})", clientId, e.getMessage());
			removeClient(clientId);
			handler.cleanUp();
		}
	}

	public void removeClient(int clientId) {
		if (activeClients.remove(clientId) != null) {
			logger.info("Client #{} : rimosso dal server.", clientId);
		}
		// non fermo il thread perchè è gestito da ExecutorService
	}

	private synchronized void stopServer() {
		if (!running)
			return;

		running = false;
		logger.info("Arresto del server in corso...");

		// libero la mappa
		activeClients.values().forEach(ClientHandler::cleanUp); // chiudo le connessioni dei client (in, out, socket)
		activeClients.clear(); // svuoto la mappa

		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.error("Errore durante la chiusura del ServerSocket: {}", e.getMessage());
			}
		}

		clientThreadPool.shutdown(); // chiuso la pool di thread
		dbManager.close();

		logger.info("Server arrestato e risorse liberate.");
	}

	public void handleRegister(int clientId, Message msg) {
		UserData userData = (UserData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		logger.info("Client #{} : ricevuta richiesta di registrazione (nickname '{}')", clientId, nickname);

		// Controllo nel database
		DBStatus status = dbManager.registerUser(nickname, password);

		ClientHandler clientHandler = activeClients.get(clientId);

		if (clientHandler != null) {
			Message responseMsg = new Message(MessageType.REGISTER_RESPONSE, status);
			clientHandler.sendMessage(responseMsg);
			logger.info("Client #{} : esito registrazione inviato (status '{}')", clientId, status);
		} else {
			logger.warn("Client #{} : non trovato tra i client attivi.", clientId);
		}
	}

	/*
	 * Getter & Setter
	 */
	public static synchronized Server getServer() {
		if (server == null) {
			server = new Server();
		}
		return server;
	}
}