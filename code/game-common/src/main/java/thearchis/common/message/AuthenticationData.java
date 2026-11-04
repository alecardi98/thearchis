package thearchis.common.message;

import java.io.Serializable;

public class AuthenticationData implements Serializable{

	private String nickname;
	private String password;

	public AuthenticationData(String nickname, String password) {
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
