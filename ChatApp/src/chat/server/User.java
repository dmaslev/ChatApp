package chat.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

/**
 * Instance of user class is created for every client connected. It keeps
 * information about the client.
 * 
 * @author I331285
 *
 */
public class User {
	private Socket socket;
	private Date connectedDate;
	private String username;
	private String adress;
	private ClientSender messageSender;
	DataOutputStream writer;
	
	public User(Socket socket, String username, ClientSender messageSender) {
		this.socket = socket;
		this.adress = socket.getLocalAddress().toString();
		this.username = username;
		this.connectedDate = new Date();
		this.messageSender = messageSender;
	}

	public Socket getSocket() {
		return this.socket;
	}

	public ClientSender getClientSender() {
		return this.messageSender;
	}

	public DataOutputStream getOutputStream() throws IOException {
		if (this.writer == null) {
			this.writer = new DataOutputStream(this.socket.getOutputStream());
		}
		
		return this.writer;
	}

	public String getUsername() {
		return this.username;
	}

	@Override
	public String toString() {
		String info = "User: " + username + "(" + adress + "), connected: " + connectedDate;
		// String.format("User: %s (%s), connected: %s", username, address,
		// connectedDate);
		return info;
	}
}