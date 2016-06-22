package chat.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * Instance of user class is created for every client connected. It keeps
 * information about the client.
 
 */
public class User {
	
	private Socket socket;
	private DataOutputStream writer;
	
	private Date connectedDate;
	private String username;
	private String adress;
	
	public User(Socket socket, String username) {
		this.socket = socket;
		this.adress = socket.getLocalAddress().toString();
		this.username = username;
		this.connectedDate = new Date();
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

	/**
	 * Returns formated string with information about the user.
	 */
	@Override
	public String toString() {
		String info = "User: " + username + "(" + adress + "), connected: " + connectedDate;
		return info;
	}

	/**
	 * Opens the output stream.
	 * @throws IOException If the socket is closed.
	 */
	public void setOutputStream()  throws IOException {
		this.writer = new DataOutputStream(this.socket.getOutputStream());
	}
}