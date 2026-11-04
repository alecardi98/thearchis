package thearchis.client.handler;

import thearchis.common.card.MasterSet;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		MasterSet.getInstance();
		
		Client client = new Client();
		client.startClient();

		client.sendRegisterRequest("alecardi98", "12345"); // registrazione errata
		Thread.sleep(500);

		client.sendLoginRequest("alecardi98", "122222"); // login errato
		Thread.sleep(500);

		client.sendLoginRequest("alecardi98", "12345"); // login
		Thread.sleep(500);

		client.sendLoginRequest("alecardi98", "12345"); // tentativo doppio login
		Thread.sleep(500);

		client.sendCollectionRequest(); // collezione
		Thread.sleep(500);

		client.sendLoginRequest("alecardi98", "12345"); // login fuori stato
		Thread.sleep(500);

		client.sendRegisterRequest("alecardi98", "12345"); // registrazione fuori stato
		Thread.sleep(500);
		
		client.sendDisenchantRequest(1);
		Thread.sleep(500);
		


		client.serverDisconnection();
	}

}
