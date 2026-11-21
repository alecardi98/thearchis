package thearchis.client.handler;

import thearchis.common.game.card.MasterSet;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		MasterSet.getInstance();

		Client client = new Client();
		client.startClient();

		Thread.sleep(10000);
		client.serverDisconnection();

	}

}
