package chat.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;

import chat.constants.SystemCode;

public class ServersideSender extends Thread {
	
	private Socket client;
	private Server messageServer;
	private boolean keepRunning;

	private LinkedList<Message> messages;
	private Map<String, User> clients;

	public ServersideSender(Socket client, MessageCenter messageCenter, Server messageServer) {
		this.client = client;
		this.messageServer = messageServer;
		this.clients =  messageServer.getClients();
	}

	/**
	 * Waits for messages from server and sends them to the client. Stops when a
	 * system message is received.
	 */
	@Override
	public void run() {
		this.messages = new LinkedList<>();
		this.keepRunning = true;

		while (keepRunning) {
			try {
				Message message = getNextMessageFromQueue();
				if (message == null) {
					// Server sent empty message to stop the run method.
					keepRunning = false;
				} else {
					if (message.getIsSystemMessage()) {
						if (message.getSystemCode() == SystemCode.LOGOUT) {
							// User asked to log out. Remove it from collection with connected users.
							messageServer.removeUser(message.getRecipient());
						}

						keepRunning = false;
						sendSystemMessage(message.getSystemCode());
					} else {
						sendMessage(message);
					}
				}
			} catch (InterruptedException interruptedException) {
				keepRunning = false;
				interruptedException.printStackTrace();
			}
		}
	}

	/**
	 * Adds a message to the queue.
	 * 
	 * @param message
	 *            A message to be added.
	 */
	protected synchronized void addMessage(Message message) {
		messages.add(message);
		notify();
	}

	/**
	 * Adds a system message in the queue to terminate the client sender. If the
	 * client listener is not terminated the system message terminates it.
	 * 
	 * @param isClientListenerClosed
	 *            Indicates if the client listener has been already closed.
	 * @param name
	 *            The username of the client.
	 */
	protected void disconnect(boolean isClientListenerClosed, String name) {
		Message systemMessage;
		if (isClientListenerClosed) {
			// Generating systems message to close the client sender
			systemMessage = new Message(SystemCode.LOGOUT, name);
		} else {
			// Generating system message to close the client sender and the
			// client listener
			systemMessage = new Message(SystemCode.SHUTDOWN, name);
		}

		addMessage(systemMessage);
	}

	/**
	 * Sends a system message to terminate the client sender.
	 */
	protected void stopSender() {
		Message systemMessage = null;
		addMessage(systemMessage);
	}
	
	/**
	 * 
	 * @return Returns the first message in the queue.
	 * @throws InterruptedException
	 */
	private synchronized Message getNextMessageFromQueue() throws InterruptedException {
		while (messages.size() == 0) {
			wait();
		}

		Message message = messages.pop();
		return message;
	}

	/**
	 * Sends a message to the client.
	 * 
	 * @param message
	 *            A message to be sent.
	 */
	private void sendMessage(Message message) {
		String sender = message.getSender();
		String messageText = message.getMessageText();
		String recipient = message.getRecipient();

		if (recipient.equalsIgnoreCase("/all")) {
			sendMessageToAllUsers(sender, messageText);
		} else {
			boolean isUserConnected = messageServer.isUserConnected(recipient);
			if (isUserConnected) {
				sendMessagetoOneUser(recipient, messageText, sender);
			} else {
				String errorMessage = recipient + " is not connected.";
				sendMessagetoOneUser(errorMessage);
			}
		}
		
		sendMessagetoOneUser("Enter your message: ");
	}

	private void sendSystemMessage(int systemCode) {
		String message = new String();
		if (systemCode == SystemCode.SHUTDOWN) {
			message = "shutdown";
		} else if(systemCode == SystemCode.LOGOUT) {
			message = "logout";
		}
		
		sendMessagetoOneUser(message);
	}

	/**
	 * A method used to inform the client when he sends message to user
	 * currently not logged in.
	 * 
	 * @param sender
	 *            The author of the message.
	 * @param errorMessage
	 *            Information message for the client.
	 */
	private void sendMessagetoOneUser(String errorMessage) {
		try {
			DataOutputStream out = new DataOutputStream(client.getOutputStream());
			out.writeUTF(errorMessage);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	/**
	 * Sends message to one user.
	 * 
	 * @param recipient
	 *            The recipient.
	 * @param messageText
	 *            The text of the message.
	 * @param sender
	 *            The author of the message.
	 */
	private void sendMessagetoOneUser(String recipient, String messageText, String sender) {
		Map<String, User> clients = messageServer.getClients();
		User user = clients.get(recipient);

		// The user is not logged in.
		if (user == null) {
			return;
		}

		try {
			DataOutputStream out = user.getOutputStream();
			if (!sender.equalsIgnoreCase("admin")) {
				messageText = sender + ": " + messageText;
			}

			out.writeUTF(messageText);
			out.flush();

			if (!sender.equals(recipient)) {
				out.writeUTF("Enter your message: ");
				out.flush();
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	/**
	 * Sends message to all connected users.
	 * 
	 * @param sender
	 *            The author of the message.
	 * @param messageText
	 *            The text of the message.
	 */
	private void sendMessageToAllUsers(String sender, String messageText) {
		messageText = sender + ": " + messageText;

		for (String client : clients.keySet()) {
			if (client.equals(sender)) {
				// Skip sending the message to the sender.
				continue;
			}
			
			User user = clients.get(client);

			try {
				DataOutputStream out = user.getOutputStream();

				out.writeUTF(messageText);
				out.flush();
			} catch (IOException ioException) {
				System.out.println("Unable to send the message to " + client);
				ioException.printStackTrace();
			}
		}
	}
}