package thearchis.common.db;

import java.io.Serializable;

public enum DBStatus implements Serializable {
	REGISTER_OK, NICKNAME_ALREADY_EXISTS, 
	LOGIN_OK, INVALID_CREDENTIALS, ALREADY_LOGGED_IN,
	DB_ERROR;
}