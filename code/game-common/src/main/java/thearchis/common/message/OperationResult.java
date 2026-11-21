package thearchis.common.message;

import java.io.Serializable;

public class OperationResult<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	private final OperationStatus status;
	private final T data;

	public OperationResult(OperationStatus status, T data) {
		if (status == null) {
			throw new IllegalArgumentException("OperationResult : status non può essere null.");
		}

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