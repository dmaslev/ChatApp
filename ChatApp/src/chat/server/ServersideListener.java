package chat.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import chat.constants.SystemCode;

public class ServersideListener extends Thread {

	private Socket client;
	private MessageCenter messageCenter;
	private Server messageServer;
	private String usernameAttched;
	private boolean keepRunning;

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
		ServersideSender clientSender = new ServersideSender(client, messageCenter, messageServer);

		try {
			DataInputStream input = new DataInputStream(client.getInputStream());
			clientSender.start();

			while (keepRunning) {
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
				} else if (messageType == SystemCode.REGULAR_MESSAGE) {
					String recipient = input.readUTF();
					Message message = new Message(textReceived, recipient, usernameAttched);
					messageCenter.addMessageToQueue(message);
				}
			}
		} catch (IOException ioException) {
			// Connection lost
			clientSender.stopSender();
			messageServer.removeUser(usernameAttched, client);
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