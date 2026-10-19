package thearchis.common.db;

import java.io.Serializable;

public enum DBStatus implements Serializable{
	REGISTER_OK, NICKNAME_ALREADY_EXISTS, INVALID_CREDENTIALS, LOGIN_OK, DB_ERROR;
}