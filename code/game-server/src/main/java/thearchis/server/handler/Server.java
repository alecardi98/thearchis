package thearchis.server.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server implements Runnable {

	private static final Logger logger = LogManager.getLogger(Server.class);
	private static Server server; // singleton
	private final int port = 443;

	private Server() {
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	/*
	 * Getter & Setter
	 */
	public static Server getServer() {
		if (server == null) {
			server = new Server();
		}
		return server;
	}

}