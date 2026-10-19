package thearchis.client.handler;

public class ClientMain {

	public static void main(String[] args) throws InterruptedException {
		Client client = new Client();
		client.startClient();
		
		client.sendRegister("alecardi98", "12345");

	}

}
