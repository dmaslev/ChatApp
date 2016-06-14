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

	public ClientListener(Socket client, MessageCenter messageCenter, Server messageServer) {
		this.client = client;
		this.messageCenter = messageCenter;
		this.messageServer = messageServer;
	}

	/**
	 * Listens for messages from client and sends them to message center.
	 */
	public void run() {
		this.keepRunning = true;
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
				} else if (messageReceived.equalsIgnoreCase("admin-register") && usernameAttched == null) {
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