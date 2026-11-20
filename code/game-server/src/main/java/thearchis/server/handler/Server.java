package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

	private static final Logger logger = LogManager.getLogger("[Server]");
	private static Server instance;
	private final int port = 443;
	private ServerSocket serverSocket;
	private volatile boolean running = true;

	private DBManager dbManager;
	private final ConcurrentMap<Integer, ClientHandler> activeClients = new ConcurrentHashMap<>(); // idclient,clienthandler
	private final AtomicInteger idGenerator = new AtomicInteger(1);
	private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
	private final ConcurrentMap<String, Integer> authenticatedUsers = new ConcurrentHashMap<>(); // nickname,idclient

	private Server() {
	}

	public static synchronized Server getInstance() {
		if (instance == null) {
			instance = new Server();
		}
		return instance;
	}

	@Override
	public void run() {
		dbConnection();
		try {
			serverSocket = new ServerSocket(port);
			logger.info("run : Server in attesa di connessioni sulla porta {} ...", port);

			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					logger.info("run : Nuova connessione accettata (IP {}).", clientSocket.getInetAddress());

					clientThreadPool.submit(() -> initializeClient(clientSocket));

				} catch (SocketException e) {
					if (!running) {
						logger.info("run : ServerSocket chiuso per arresto del server.");
						break;
					}
					logger.error("run : Errore di socket durante l'accept ({}).", e.getMessage());
				} catch (IOException e) {
					logger.error("run : Errore I/O durante l'accept ({}).", e.getMessage());
				}
			}
		} catch (IOException e) {
			logger.fatal("run : Impossibile aprire il ServerSocket sulla porta {} ({}).", port, e.getMessage());
		} finally {
			stopServer();
		}
	}

	private void dbConnection() {
		dbManager = new DBManager();
		dbManager.dbConnect();
	}

	private void initializeClient(Socket socket) {
		int clientId = idGenerator.getAndIncrement();
		ClientHandler clientHandler = new ClientHandler(socket, clientId);

		try {
			clientHandler.init();
			activeClients.put(clientId, clientHandler);
			logger.info("Client #{} : initializeClient : Client inizializzato con successo.", clientId);

			clientThreadPool.submit(clientHandler);

		} catch (IOException e) {
			logger.error("Client #{} : initializeClient : Errore durante l'handshake ({}).", clientId, e.getMessage());
			removeClient(clientId);
			clientHandler.cleanUp();
		}
	}

	public void removeClient(int clientId) {
		ClientHandler clientHandler = activeClients.remove(clientId);

		if (clientHandler != null) {
			logger.info("Client #{} : removeClient : Client rimosso dai client attivi.", clientId);

			if (clientHandler.getPlayer() != null && clientHandler.getPlayer().getNickname() != null) {
				String nickname = clientHandler.getPlayer().getNickname();
				authenticatedUsers.remove(nickname);
				logger.info("Client #{} : removeClient : Autenticazione utente {} rimossa.", clientId, nickname);
			}
		} else {
			logger.warn(
					"Client #{} : removeClient : Client non trovato tra i client attivi al momento della rimozione.",
					clientId);
		}
	}

	private synchronized void stopServer() {
		if (!running)
			return;

		running = false;
		logger.info("stopServer : Arresto del server in corso...");

		activeClients.values().forEach(ClientHandler::cleanUp);
		activeClients.clear();

		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.error("stopServer : Errore durante la chiusura del ServerSocket ({}).", e.getMessage());
			}
		}

		clientThreadPool.shutdown();
		dbManager.dbClose();

		logger.info("stopServer : Server arrestato con successo.");
	}

	public boolean isUserAuthenticated(String nickname) {
		return authenticatedUsers.containsKey(nickname);
	}

	public void registerLoggedUser(String nickname, int clientId) {
		authenticatedUsers.put(nickname, clientId);
	}

	/*
	 * Getter & Setter
	 */
	public DBManager getDBManager() {
		return dbManager;
	}

	public ConcurrentMap<String, Integer> getAuthenticatedUsers() {
		return authenticatedUsers;
	}
}