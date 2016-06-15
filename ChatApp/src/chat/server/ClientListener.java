package chat.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientListener extends Thread {
	private Socket client;
	private MessageCenter messageCenter;
	private Server messageServer;
	private DataInputStream input;
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
			this.input = new DataInputStream(client.getInputStream());
		} catch (IOException ioException) {
			keepRunning = false;
		}

		ClientSender clientSender = new ClientSender(client, messageCenter, messageServer);
		clientSender.start();

		while (keepRunning) {
			try {
				int messageType = input.readInt();
				String textReceived = input.readUTF();
				if (messageType == 200) {
					keepRunning = false;
					clientSender.disconnect(true, usernameAttched);
				} else if (messageType == 100) {
					messageServer.addUser(textReceived, client, clientSender, this);
				} else if (messageType == 300) {
					// Client sender already closed. The client asked to
					// close the clientListener
					messageServer.removeUser(usernameAttched, client);
					keepRunning = false;
				} else if (messageType == 400){
					String recipient = input.readUTF();
					Message message = new Message(textReceived, recipient, usernameAttched);
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