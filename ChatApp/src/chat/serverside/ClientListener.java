package chat.serverside;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ClientListener extends Thread {
	private Socket client;
	private MessageCenter messageCenter;
	private Server messageServer;
	private BufferedReader input;
	private String usernameAttched;
	private boolean keepRunning;

	public ClientListener(Socket socket, MessageCenter messageCenter, Server server) {
		this.client = socket;
		this.messageCenter = messageCenter;
		this.messageServer = server;
		this.keepRunning = true;
	}

	public Socket getSocket() {
		return this.client;
	}

	public void run() {
		try {
			this.input = new BufferedReader(new InputStreamReader(client.getInputStream()));

		} catch (IOException e1) {
			keepRunning = false;
		}
		ClientSender clientSender = new ClientSender(client, messageCenter, messageServer);
		clientSender.start();
		
		while (keepRunning) {
			try {
				String messageReceived = input.readLine();
				String recipient = input.readLine();

				if (messageReceived.equalsIgnoreCase("admin-logout")) {
					keepRunning = false;
					Message systemMessage = new Message("shutdown", "admin", "admin");
					clientSender.addMessage(systemMessage);
				} else if (messageReceived.equalsIgnoreCase("admin-register")) {
					messageServer.registerUser(recipient, client, clientSender, this);
				} else {
					Message message = new Message(messageReceived, recipient, usernameAttched);
					messageCenter.addMessageToQueue(message);
				}
			} catch (IOException e) {
				keepRunning = false;
			}
		}

		messageServer.removeUser(usernameAttched, client);
	}

	public void disconnect() throws IOException {
		OutputStream output = client.getOutputStream();
		output.close();
		input.close();
		client.close();
	}

	public void setUsername(String name) {
		this.usernameAttched = name;
	}
}