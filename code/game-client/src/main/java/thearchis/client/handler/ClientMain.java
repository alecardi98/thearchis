package thearchis.client.handler;

import thearchis.common.card.MasterSet;
import thearchis.common.game.ClientState;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		MasterSet.getInstance();

		Client client = new Client();
		client.startClient();

		client.sendRegisterRequest("alecardi98", "12345"); // registrazione già esistente
		Thread.sleep(500);
		client.sendBackNavigationRequest(ClientState.MAIN_MENU); // navigazione errata NOT_AUTHENTICATED -> MAIN_MENU
		Thread.sleep(500);
		client.sendLoginRequest("alecardi98", "122222"); // login errato
		Thread.sleep(500);
		client.sendLoginRequest("alecardi98", "12345"); // login
		Thread.sleep(500);
		client.sendLoginRequest("alecardi98", "12345"); // tentativo doppio login
		Thread.sleep(500);
		client.sendBackNavigationRequest(ClientState.MAIN_MENU); // navigazione errata MAIN_MENU -> MAIN_MENU
		Thread.sleep(500);
		client.sendCollectionRequest(); // accesso collezione
		Thread.sleep(500);
		client.sendLoginRequest("alecardi98", "12345"); // login fuori stato
		Thread.sleep(500);
		client.sendRegisterRequest("alecardi98", "12345"); // registrazione fuori stato
		Thread.sleep(500);
		client.sendBackNavigationRequest(ClientState.MAIN_MENU); // navigazione corretta IN_COLLECTION -> MAIN_MENU
		Thread.sleep(500);
		client.sendBackNavigationRequest(ClientState.MAIN_MENU); // navigazione errata MAIN_MENU -> MAIN_MENU
		Thread.sleep(500);
		client.sendCollectionRequest(); // accesso collezione
		Thread.sleep(500);
		client.sendCollectionRequest(); // accesso errato collezione
		Thread.sleep(500);

		client.serverDisconnection();

	}

}
