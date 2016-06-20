package chat.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import chat.constants.SystemCode;

public class ServersideListener extends Thread {

	private Socket client;
	private DataInputStream input;
	private boolean keepRunning;

	private MessageCenter messageCenter;
	private Server messageServer;
	private String usernameAttched;
	
	public ServersideListener(Socket client, MessageCenter messageCenter, Server messageServer) {
		this.client = client;
		this.messageCenter = messageCenter;
		this.messageServer = messageServer;
	}

	/**
	 * Listens for messages from client and sends them to message center.
	 */
	@Override
	public void run() {
		this.keepRunning = true;

		try {
			input = new DataInputStream(client.getInputStream());

			while (keepRunning) {
				int messageType = input.readInt();
				String textReceived = input.readUTF();
				if (messageType == SystemCode.LOGOUT) {
					keepRunning = false;
					messageCenter.disconnectUser(usernameAttched);
				} else if (messageType == SystemCode.REGISTER) {
					messageServer.addUser(textReceived, client, this);
				} else if (messageType == SystemCode.SHUTDOWN) {
					// Client sender already closed. The client asked to
					// close the clientListener
					messageServer.removeUser(usernameAttched);
					keepRunning = false;
				} else if (messageType == SystemCode.REGULAR_MESSAGE) {
					String recipient = input.readUTF();
					Message message = new Message(textReceived, recipient, usernameAttched);
					messageCenter.addMessageToQueue(message);
				}
			}
		} catch (IOException ioException) {
			// Connection lost
			messageServer.removeUser(usernameAttched);
			keepRunning = false;
		} 
	}

	public Socket getSocket() {
		return this.client;
	}

	protected void setUsername(String name) {
		this.usernameAttched = name;
	}
}