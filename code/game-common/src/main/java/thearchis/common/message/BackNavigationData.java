package thearchis.common.message;

import java.io.Serializable;

import thearchis.common.game.ClientState;

public class BackNavigationData implements Serializable {

	private ClientState destination;

	public BackNavigationData(ClientState destination) {
		this.destination = destination;
	}

	public ClientState getDestination() {
		return destination;
	}

}
