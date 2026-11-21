package thearchis.server.handler;

import thearchis.common.card.MasterSet;

public class ServerMain {

	public static void main(String[] args) {
		MasterSet.getInstance();
		Server.getInstance().run();
	}

}