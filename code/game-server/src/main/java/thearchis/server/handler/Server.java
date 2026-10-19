package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static Server server;
	private final int port = 443;

	private final ConcurrentMap<Integer, ClientHandler> activeClients = new ConcurrentHashMap<>();
	private final AtomicInteger idGenerator = new AtomicInteger(1);
	private final ExecutorService clientThreadPool = Executors.newCachedThreadPool();

	private ServerSocket serverSocket;
	private volatile boolean running = true;

	private Server() {
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			logger.info("Server in ascolto sulla porta {}", port);

			while (running) {
				try {
					Socket clientSocket = serverSocket.accept();
					logger.info("Nuova connessione accettata da: {}", clientSocket.getInetAddress());

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

	private void initializeClient(Socket socket) {
		int clientId = idGenerator.getAndIncrement();
		ClientHandler handler = new ClientHandler(socket, clientId);

		try {
			handler.init();
			activeClients.put(clientId, handler);
			logger.info("Client #{} registrato con successo.", clientId);

			handler.run(); // Avvia il ciclo di ascolto messaggi di questo handler

		} catch (IOException e) {
			logger.error("Errore durante l'handshake con il client #{}: {}", clientId, e.getMessage());
		} finally {
			removeClient(clientId);
			handler.cleanUp();
		}
	}

	public void removeClient(int clientId) {
		if (activeClients.remove(clientId) != null) {
			logger.info("Client #{} rimosso dal server.", clientId);
		}
	}

	public synchronized void stopServer() {
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
		logger.info("Server arrestato e risorse liberate.");
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