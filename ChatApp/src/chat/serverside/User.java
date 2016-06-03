package chat.serverside;
import java.net.Socket;
import java.util.Date;

public class User {
	private Socket socket;
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
	
	public String getUsername(){
		return this.username;
	}
	
	public String toString() {
		String info = String.format("User: %s (%s), connected: %s", username, adress, connectedDate);
		return info;
	}
}