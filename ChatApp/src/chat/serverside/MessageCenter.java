package chat.serverside;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;

public class MessageCenter {

	private Server server;
	private Map<String, User> clients;

	public MessageCenter(Server server) {
		this.server = server;
		this.clients = this.server.getClients();
	}

	synchronized public void sendMessagetoOneUser(String client, String message, String sender) {
		User user = clients.get(client);
		if (user == null) {
			return;
		}

		try {
			Socket ct = user.getSocket();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			message = sender + ": " + message;
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	synchronized public void sendMessagetoOneUser(Socket ct, String message) {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	synchronized public void sendMessageToAllUsers(String sender, String message) {
		for (String client : clients.keySet()) {
			if (client == sender) {
				continue;
			}
			
			User user = clients.get(client);
			Socket ct = user.getSocket();

			try {
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
				message = sender + ": " + message;
				out.write(message);
				out.newLine();
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	synchronized public void registerUser(String name, Socket client, ClientThread messageListener) {
		server.addUser(name, client, messageListener);
	}

	synchronized public boolean isUserConnected(String username) {
		User client = clients.get(username);
		if (client == null) {
			return false;
		}

		return true;
	}
}