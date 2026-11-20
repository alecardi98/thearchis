package thearchis.common.message;

import java.io.Serializable;

import thearchis.common.game.ClientState;

public class BackNavigationData implements Serializable {

	private static final long serialVersionUID = 1L;

	private ClientState destination;

	public BackNavigationData(ClientState destination) {
		if (destination == null) {
			throw new IllegalArgumentException("BackNavigationData : destination non può essere null.");
		}
		this.destination = destination;
	}

	public ClientState getDestination() {
		return destination;
	}

}
