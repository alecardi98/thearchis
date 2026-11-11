package thearchis.client.handler;

import thearchis.common.card.MasterSet;
import thearchis.common.game.ClientState;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		MasterSet.getInstance();

		Client client = new Client();
		client.startClient();

		Thread.sleep(10000);
		client.serverDisconnection();

	}

}
