package thearchis.common.message;

import java.io.Serializable;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;

	private MessageType type;
	private Object data;

	public Message(MessageType type, Object data) {
		if (type == null) {
			throw new IllegalArgumentException("Message : type non può essere null.");
		}

		this.type = type;
		this.data = data;
	}

	/*
	 * Getter & Setter
	 */
	public MessageType getType() {
		return type;
	}

	public Object getData() {
		return data;
	}
}