package chat.server;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Date;

public class User {
	private Socket socket;
	private Date connectedDate;
	private String username;
	private String adress;
	private ClientSender messageSender;

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
	
	public BufferedWriter getOutputStream() throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
		return writer;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	@Override
	public String toString() {
		String info = "User: " + username + "(" + adress + "), connected: " + connectedDate; 
//		String.format("User: %s (%s), connected: %s", username, address, connectedDate);
		return info;
	}
}