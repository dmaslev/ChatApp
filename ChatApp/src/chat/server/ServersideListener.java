package chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import chat.constants.SystemCode;

public class ServersideListener extends Thread {

	private Socket clientSocket;
	private BufferedWriter output;
	private BufferedReader input;
	private boolean keepRunning;

	private MessageDispatcher messageCenter;
	private Server messageServer;
	private String usernameAttached;

	public ServersideListener(Socket clientSocket, MessageDispatcher messageCenter, Server messageServer) {
		this.clientSocket = clientSocket;
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
			input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

			while (keepRunning) {
				String messageType = input.readLine();
				String textReceived = input.readLine();
				if (messageType.equals(SystemCode.LOGOUT)) {
					keepRunning = false;
					// TODO fix usernamerattached
					messageCenter.disconnectUser(usernameAttached);
				} else if (messageType.equals(SystemCode.REGISTER)) {
					// Add user in the collection with all connected users
					boolean successfulLogin = messageServer.addUser(textReceived, output, this);
					if (!successfulLogin) {
						// TODO User login failed. Stop the listener.
						keepRunning = false;
					}
				} else if (messageType.equals(SystemCode.DISCONNECT)) {
					// The client asked to close the clientListener.
					messageServer.removeUser(usernameAttached);
					keepRunning = false;
				} else if (messageType.equals(SystemCode.REGULAR_MESSAGE)) {
					String recipient = input.readLine();
					Message message = new Message(textReceived, recipient, usernameAttached);
					messageCenter.addMessageToQueue(message);
				}
			}
		} catch (IOException ioException) {
			// Connection lost
			usernameAttached = clientSocket.getInetAddress().toString();
			messageServer.removeUser(usernameAttached);
			keepRunning = false;
			ioException.printStackTrace();
		}
	}

	public Socket getSocket() {
		return this.clientSocket;
	}

	public void setOutputStream() throws IOException {
		this.output = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
	}

	protected void setUsername(String name) {
		this.usernameAttached = name;
	}
}