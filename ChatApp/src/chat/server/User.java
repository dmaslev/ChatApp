package chat.server;

import java.io.BufferedWriter;
import java.util.Date;

/**
 * Instance of user class is created for every client connected. It keeps
 * information about the client.
 * 
 */
public class User {

	private BufferedWriter output;

	private Date connectedDate;
	private String username;
	private String address;


	public User(String name, BufferedWriter output, String address) {
		this.address = address;
		this.username = name;
		this.output = output;
		this.connectedDate = new Date();
	}

	public BufferedWriter getOutputStream() {
		return this.output;
	}

	public String getUsername() {
		return this.username;
	}


	/**
	 * Returns formated string with information about the user.
	 */
	@Override
	public String toString() {
		String info = "User: " + username + "(" + address + "), connected: " + connectedDate;
		return info;
	}
}