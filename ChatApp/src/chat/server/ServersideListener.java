package chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Map;

import chat.util.Logger;
import chat.util.SystemCode;

public class ServersideListener extends Thread {

	private Socket clientSocket;
	private BufferedWriter output;
	private OutputStreamWriter outputInner;
	private BufferedReader input;
	private InputStreamReader inputInner;
	private boolean keepRunning;

	private MessageDispatcher messageDispatcher;
	private Server messageServer;

	private String username;
	private Date connectedDate;

	public ServersideListener(Socket clientSocket, MessageDispatcher messageDispatcher, Server messageServer) {
		this.clientSocket = clientSocket;
		this.messageDispatcher = messageDispatcher;
		this.messageServer = messageServer;
		this.connectedDate = new Date();
	}

	/**
	 * Listens for messages from client and sends them to message dispatcher.
	 */
	@Override
	public void run() {
		this.keepRunning = true;

		try {
			try {
				this.inputInner = new InputStreamReader(clientSocket.getInputStream());
				this.input = new BufferedReader(inputInner);
			} catch (IOException e) {
				// Failed to open input stream.
				closeRecourses();
				throw new IOException("Opening input stream failed.", e);
			}

			try {
				this.outputInner = new OutputStreamWriter(this.clientSocket.getOutputStream());
				this.output = new BufferedWriter(outputInner);
			} catch (IOException e) {
				closeRecourses();
				throw new IOException("Opening output stream failed.", e);
			}

			while (keepRunning) {
				String messageType = input.readLine();
				String textReceived = input.readLine();
				if (messageType == null || textReceived == null) {
					// Client socket was closed.
					closeRecourses();
					messageServer.removeListener(this);
					return;
				}

				if (messageType.equals(SystemCode.REGISTER)) {
					// First check if the given username is available.
					String resultCode = messageServer.validateUsername(textReceived);
					// Second check to ensure that no other user logged in with
					// the same username between last check and the time current
					// user is logged in.
					resultCode = messageServer.addUser(textReceived, this);

					sendMessageToClient(resultCode);

					if (!resultCode.equals(SystemCode.SUCCESSFUL_LOGIN)) {
						closeRecourses();
						messageServer.removeListener(this);
						keepRunning = false;
						return;
					}

					setUsername(textReceived);
				} else if (messageType.equals(SystemCode.LOGOUT)) {
					keepRunning = false;
					messageServer.disconnectUser(username);
				} else if (messageType.equals(SystemCode.REGULAR_MESSAGE)) {
					String recipient = input.readLine();

					if (recipient.equals("/all")) {
						sendMessageToAllUsers(textReceived, recipient);
						continue;
					}

					sendMessageToOneClientClient(textReceived, recipient, username);
				}
			}
		} catch (IOException ioException) {
			// Connection lost
			messageServer.removeListener(this);
			keepRunning = false;
			closeRecourses();
			System.err.println("Error occured in ServersideListener." + Logger.printError(ioException));
		}
	}

	public String getUsername() {
		return this.username;
	}

	public BufferedWriter getOutputStream() {
		return this.output;
	}

	/**
	 * Returns formatted string with information about the user.
	 */
	@Override
	public String toString() {
		String address = clientSocket.getInetAddress().toString();
		String info = "User: " + username + "(" + address + "), connected: " + connectedDate;
		return info;
	}

	void closeRecourses() {
		this.keepRunning = false;
		String address = clientSocket.getLocalAddress().toString();
		int port = clientSocket.getLocalPort();

		try {
			input.close();
		} catch (IOException e) {
			System.err.println("Unable to close outer input stream for user: " + username + ", address: " + address
					+ ", port: " + port + Logger.printError(e));
			try {
				inputInner.close();
			} catch (IOException e1) {
				System.err.println("Unable to close inner input stream for user: " + username + ", address: " + address
						+ ", port: " + port + Logger.printError(e1));
			}
		}

		try {
			output.close();
		} catch (IOException e) {
			System.err.println("Unable to close outer output stream for user: " + username + ", address: " + address
					+ ", port: " + port + Logger.printError(e));

			try {
				outputInner.close();
			} catch (IOException e1) {
				System.err.println("Unable to close inner output stream for user: " + username + ", address: " + address
						+ ", port: " + port + Logger.printError(e1));
			}
		}

		try {
			clientSocket.close();
		} catch (IOException e) {
			System.err.println(
					"Unable to close client socket (address: " + address + ", port: " + port + Logger.printError(e));
		}
	}

	void shutdown() throws IOException {
		sendMessageToClient("disconnect");
		this.clientSocket.close();
	}

	private void setUsername(String name) {
		this.username = name;
	}

	private void sendMessageToClient(String text) throws IOException {
		try {
			output.write(text);
			output.newLine();
			output.flush();
		} catch (IOException ioException) {
			throw new IOException("Can not send message to " + this.clientSocket.getInetAddress().toString(),
					ioException);
		}
	}

	private void sendMessageToAllUsers(String textReceived, String recipient) throws IOException {
		Map<String, ServersideListener> copyOfAllClients = messageServer.getCopyOfClients();
		for (String client : copyOfAllClients.keySet()) {
			if (client.equals(username)) {
				// Skip sending the message to the sender.
				continue;
			}

			Message message = new Message(textReceived, client, username);
			boolean messageSent = messageDispatcher.addMessageToQueue(message);
			if (!messageSent) {
				// MessageDispatcher has been shut down. Unable to send
				// the message.
				String text = "Failed to send your message: \"" + textReceived + "\" to: " + recipient;
				sendMessageToOneClientClient(text, username, "admin");
			}
		}
	}

	private void sendMessageToOneClientClient(String textReceived, String recipient, String sender) {
		Message message = new Message(textReceived, recipient, sender);
		boolean messageSent = messageDispatcher.addMessageToQueue(message);
		if (!messageSent) {
			// MessageDispatcher has been shut down. Unable to send
			// the message.
			String text = "Failed to send your message: \"" + textReceived + "\" to: " + recipient;
			sendMessageToOneClientClient(text, username, "admin");
		}
	}
}