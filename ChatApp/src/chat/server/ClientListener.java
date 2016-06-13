package chat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

	/**
	 * Listens for messages from client and sends them to message center.
	 */
	public void run() {
		try {
			this.input = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException ioException) {
			keepRunning = false;
		}
		
		ClientSender clientSender = new ClientSender(client, messageCenter, messageServer);
		clientSender.start();

		while (keepRunning) {
			try {
				String messageReceived = input.readLine();
				String recipient = input.readLine();
				if (messageReceived.equalsIgnoreCase("logout") && recipient.equalsIgnoreCase(usernameAttched)) {
					keepRunning = false;
					clientSender.disconnect(true, usernameAttched);
				} else if (messageReceived.equalsIgnoreCase("admin-register")) {
					messageServer.addUser(recipient, client, clientSender, this);
				} else if (messageReceived.equalsIgnoreCase("shutdown")) {
					// Client sender already closed. The client asked to
					// close the clientListener
					messageServer.removeUser(usernameAttched, client);
					keepRunning = false;
				} else {
					Message message = new Message(messageReceived, recipient, usernameAttched);
					messageCenter.addMessageToQueue(message);
				}
			} catch (IOException ioException) {
				// Connection lost
				clientSender.stopSender();
				messageServer.removeUser(usernameAttched, client);
				keepRunning = false;
			}
		}
	}

	public Socket getSocket() {
		return this.client;
	}

	public void setUsername(String name) {
		this.usernameAttched = name;
	}
}