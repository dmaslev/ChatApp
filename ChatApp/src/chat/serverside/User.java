package chat.serverside;
import java.net.Socket;
import java.util.Date;

public class User {
	private Socket socket;
	private Date connectedDate;
	private String username;
	private String adress;
	private ClientListener messageListener;
	private ClientSender messageSender;

	public User(Socket socket, String username, ClientSender messageSender, ClientListener messageListener) {
		this.socket = socket;
		this.adress = socket.getLocalAddress().toString();
		this.username = username;
		this.connectedDate = new Date();
		this.messageListener = messageListener;
		this.messageSender = messageSender;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public ClientListener getClientListener() {
		return this.messageListener;
	}

	public ClientSender getClientSender() {
		return this.messageSender;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public String toString() {
		String info = String.format("User: %s (%s), connected: %s", username, adress, connectedDate);
		return info;
	}
}