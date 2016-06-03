package chat.serverside;
import java.net.Socket;
import java.util.Date;

public class User {
	private Socket socket;
	private Date connectedDate;
	private String username;
	private String adress;
	private ClientThread messageListener;

	public User(Socket socket, String username, ClientThread messageListener) {
		this.socket = socket;
		this.adress = socket.getLocalAddress().toString();
		this.username = username;
		this.connectedDate = new Date();
		this.messageListener = messageListener;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public ClientThread getClientThread() {
		return this.messageListener;
	}
	
	public String getUsername(){
		return this.username;
	}
	
	public String toString() {
		String info = String.format("User: %s (%s), connected: %s", username, adress, connectedDate);
		return info;
	}
}