package thearchis.common.message;

import java.io.Serializable;

public class Message implements Serializable {

	private MessageType type;
	private Object data;

	public Message(MessageType type, Object data) {
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