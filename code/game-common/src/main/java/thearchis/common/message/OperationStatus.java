package thearchis.common.message;

import java.io.Serializable;

public enum OperationStatus implements Serializable {
	REGISTER_OK, NICKNAME_ALREADY_EXISTS, 
	LOGIN_OK, INVALID_CREDENTIALS, ALREADY_LOGGED_IN,
	
	BACKNAVIGATION_OK,
	
	COLLECTION_OK,
	CRAFT_OK, NOT_ENOUGH_DUST,
	DISENCHANT_OK, CARD_NOT_OWNED,

	BAD_REQUEST,
	DB_ERROR;
}