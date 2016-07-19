package chat.server;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Date;

/**
 * Instance of user class is created for every client connected. It keeps
 * information about the client.
 * 
 */
public class User {

	private Socket socket;
	private DataOutputStream writer;

	private Date connectedDate;
	private String username;
	private String adress;
	
	private boolean isLoggedIn;

	public User(Socket socket, String name) {
		this.socket = socket;
		this.adress = socket.getLocalAddress().toString();
		this.username = name;
		this.connectedDate = new Date();
		this.isLoggedIn = false;
	}

	public Socket getSocket() {
		return this.socket;
	}

	public DataOutputStream getOutputStream() {
		return this.writer;
	}

	public String getUsername() {
		return this.username;
	}
	
	public void setUsername(String name) {
		this.username = name;
		this.isLoggedIn = true;
	}
	
	public boolean isLoggedIn() {
		return this.isLoggedIn;
	}

	/**
	 * Returns formated string with information about the user.
	 */
	@Override
	public String toString() {
		String info = "User: " + username + "(" + adress + "), connected: " + connectedDate;
		return info;
	}
}