package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import thearchis.common.db.DBStatus;
import thearchis.common.game.ClientState;
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

	private final ConcurrentMap<Integer, ClientHandler> activeClients = new ConcurrentHashMap<>(); // idclient,clienthandler
	private final AtomicInteger idGenerator = new AtomicInteger(1);
	private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();
	private final ConcurrentMap<String, Integer> loggedUsers = new ConcurrentHashMap<>(); // nickname,idclient

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
		ClientHandler clientHandler = new ClientHandler(socket, clientId);

		try {
			clientHandler.init();
			activeClients.put(clientId, clientHandler); // mappo il nuovo clienthandler
			logger.info("Client #{} : collegato con successo.", clientId);

			clientThreadPool.submit(clientHandler); // nuovo thread per il clientHandler

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

			String nickname = clientHandler.getNickname();
			if (nickname != null) { // client autenticato
				loggedUsers.remove(nickname);
				logger.info("Client #{} : autenticazione utente '{}' rimossa.", nickname, clientId);
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

	public void handleRegister(int clientId, Message msg) {
		UserData userData = (UserData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		logger.info("Client #{} : ricevuta richiesta di registrazione (nickname '{}')", clientId, nickname);

		ClientHandler clientHandler = activeClients.get(clientId);
		if (clientHandler == null) {
			logger.warn("Client #{} : non trovato tra i client attivi.", clientId);
			return;
		}

		if (clientHandler.getClientState() != ClientState.NOT_AUTHENTICATED) {
			logger.warn("Client #{} : tentativo di registrazione rifiutato (stato non valido: {}).", clientId,
					clientHandler.getClientState());
			clientHandler.sendMessage(new Message(MessageType.REGISTER_RESPONSE, DBStatus.BAD_REQUEST));
			return;
		}

		DBStatus status = dbManager.registerUser(nickname, password);
		Message responseMsg = new Message(MessageType.REGISTER_RESPONSE, status);
		clientHandler.sendMessage(responseMsg);

		logger.info("Client #{} : esito registrazione inviato (status '{}')", clientId, status);
	}

	public void handleLogin(int clientId, Message msg) {
		UserData userData = (UserData) msg.getData();
		String nickname = userData.getNickname();
		String password = userData.getPassword();

		logger.info("Client #{} : ricevuta richiesta di login (nickname '{}')", clientId, nickname);

		ClientHandler clientHandler = activeClients.get(clientId);
		if (clientHandler == null) {
			logger.warn("Client #{} : non trovato tra i client attivi.", clientId);
			return;
		}

		if (clientHandler.getClientState() != ClientState.NOT_AUTHENTICATED) {
			logger.warn("Client #{} : tentativo di login rifiutato (stato non valido: {}).", clientId,
					clientHandler.getClientState());
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, DBStatus.BAD_REQUEST));
			return;
		}

		if (loggedUsers.containsKey(nickname)) { // nickname già loggato altrove
			logger.info("Client #{} : nickname '{}' già autenticato altrove.", clientId, nickname);
			clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, DBStatus.ALREADY_LOGGED_IN));
			return;
		}

		DBStatus status = dbManager.loginUser(nickname, password);

		if (status == DBStatus.LOGIN_OK) {
			clientHandler.setNickname(nickname);
			clientHandler.setClientState(ClientState.MAIN_MENU);
			loggedUsers.put(nickname, clientId);
		}

		clientHandler.sendMessage(new Message(MessageType.LOGIN_RESPONSE, status));
		logger.info("Client #{} : esito login inviato (status '{}')", clientId, status);
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