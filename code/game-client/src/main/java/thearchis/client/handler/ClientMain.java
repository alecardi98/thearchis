package thearchis.client.handler;

import thearchis.common.game.card.MasterSet;
import thearchis.common.game.state.ClientState;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		MasterSet.getInstance();

		Client client = new Client();
		client.startClient();

		Thread.sleep(10000);
		client.serverDisconnection();

	}

}
