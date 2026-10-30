package thearchis.client.handler;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		Client client = new Client();
		client.startClient();

		client.sendRegister("alecardi98", "12345"); // registrazione errata
		Thread.sleep(3000);

		client.sendLogin("alecardi98", "122222"); // login errato
		Thread.sleep(3000);

		client.sendLogin("alecardi98", "12345"); // login
		Thread.sleep(20000);

		client.sendLogin("alecardi98", "12345"); // tentativo doppio login
		Thread.sleep(3000);

		client.serverDisconnection();
	}

}
