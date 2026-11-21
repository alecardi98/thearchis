package thearchis.common.message;

import java.io.Serializable;

public class AuthenticationData implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nickname;
	private String password;

	public AuthenticationData(String nickname, String password) {
		if (nickname == null || nickname.isBlank()) {
			throw new IllegalArgumentException("AuthenticationData : nickname non può essere null o vuoto.");
		}
		if (password == null || password.isBlank()) {
			throw new IllegalArgumentException("AuthenticationData : password non può essere null o vuota.");
		}

		this.nickname = nickname;
		this.password = password;
	}

	public String getNickname() {
		return nickname;
	}

	public String getPassword() {
		return password;
	}
}
