package thearchis.common.message;

import java.io.Serializable;

public enum MessageType implements Serializable {
	REGISTER_REQUEST, REGISTER_RESPONSE,
	LOGIN_REQUEST, LOGIN_RESPONSE,
	COLLECTION_REQUEST, COLLECTION_RESPONSE;
}
