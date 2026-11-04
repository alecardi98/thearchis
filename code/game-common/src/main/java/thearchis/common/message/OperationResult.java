package thearchis.common.message;

import java.io.Serializable;

public class OperationResult<T> implements Serializable {
	private final OperationStatus status;
	private final T data;

	public OperationResult(OperationStatus status, T data) {
		this.status = status;
		this.data = data;
	}

	/*
	 * Getter & Setter
	 */

	public OperationStatus getStatus() {
		return status;
	}

	public T getData() {
		return data;
	}
}