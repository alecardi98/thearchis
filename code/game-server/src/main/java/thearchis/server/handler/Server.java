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

	private static final Logger logger = LogManager.getLogger(Server.class);
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
			logger.info("Server in ascolto sulla porta {}: in attesa di connessioni...", port);

			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					logger.info("Nuova connessione da {} accettata.", clientSocket.getInetAddress());

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

	private void dbConnection() {
		dbManager = new DBManager();
		dbManager.connect();
	}

	private void initializeClient(Socket socket) {
		int clientId = idGenerator.getAndIncrement(); 
		ClientHandler clientHandler = new ClientHandler(socket, clientId);

		try {
			clientHandler.init();
			activeClients.put(clientId, clientHandler); 
			logger.info("Client #{} : collegato con successo.", clientId);

			clientThreadPool.submit(clientHandler); 

		} catch (IOException e) {
			logger.error("Client #{} : errore durante l'handshake ({})", clientId, e.getMessage());
			removeClient(clientId);
			clientHandler.cleanUp();
		}
	}

	public void removeClient(int clientId) {
		ClientHandler clientHandler = activeClients.remove(clientId);

		if (clientHandler != null) {
			logger.info("Client #{} : rimosso dai client attivi.", clientId);

			String nickname = clientHandler.getPlayer().getNickname();
			if (nickname != null) { // client autenticato
				authenticatedUsers.remove(nickname);
				logger.info("Client #{} : autenticazione utente '{}' rimossa.", clientId, nickname);
			}
		} else {
			logger.warn("Client #{} : non trovato tra i client attivi al momento della rimozione.", clientId);
		}
	}

	private synchronized void stopServer() {
		if (!running)
			return;

		running = false;
		logger.info("Arresto del server in corso...");

		activeClients.values().forEach(ClientHandler::cleanUp);
		activeClients.clear();

		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				logger.error("Errore durante la chiusura del ServerSocket: {}", e.getMessage());
			}
		}

		clientThreadPool.shutdown();
		dbManager.close();

		logger.info("Server arrestato e risorse liberate.");
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
	public DBManager getDbManager() {
		return dbManager;
	}

}