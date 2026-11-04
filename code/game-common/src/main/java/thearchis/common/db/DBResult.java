package thearchis.common.db;

import java.io.Serializable;

public class DBResult<T> implements Serializable {
	private final DBStatus status;
	private final T data;

	public DBResult(DBStatus status, T data) {
		this.status = status;
		this.data = data;
	}

	/*
	 * Getter & Setter
	 */

	public DBStatus getStatus() {
		return status;
	}

	public T getData() {
		return data;
	}
}