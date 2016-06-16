package chat.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import chat.constants.SystemCode;

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
				if (messageType == SystemCode.LOGOUT) {
					keepRunning = false;
					clientSender.disconnect(true, usernameAttched);
				} else if (messageType == SystemCode.REGISTER) {
					messageServer.addUser(textReceived, client, clientSender, this);
				} else if (messageType == SystemCode.SHUTDOWN) {
					// Client sender already closed. The client asked to
					// close the clientListener
					messageServer.removeUser(usernameAttched, client);
					keepRunning = false;
				} else if (messageType == SystemCode.REGULAR_MESSAGE){
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