package chat.serverside;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Map;

public class MessageCenter {

	private Server server;
	private Map<String, Socket> clients;

	public MessageCenter(Server server) {
		this.server = server;
		this.clients = this.server.getClients();
	}

	synchronized public void sendMessagetoOneUser(String client, String message) {
		Socket ct = clients.get(client);
		if (ct == null) {
			return;
		}

		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessagetoOneUser(Socket ct, String message) {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	synchronized public void sendMessageToAllUsers(String message) {
		for (String client : clients.keySet()) {
			Socket ct = clients.get(client);

			try {
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
				out.write(message);
				out.newLine();
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	synchronized public void registerUser(String name, Socket client) {
		server.addUser(name, client);
	}

	synchronized public boolean isUserConnected(String username) {
		Socket ct = clients.get(username);
		if (ct == null) {
			return false;
		}

		return true;
	}
}