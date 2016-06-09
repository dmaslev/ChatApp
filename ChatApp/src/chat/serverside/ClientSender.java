package chat.serverside;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;

public class ClientSender extends Thread {
	private LinkedList<Message> messages;
	private MessageCenter messageCenter;
	private Socket client;
	private boolean keepRunning;
	private Server messageServer;

	public ClientSender(Socket client, MessageCenter messageCenter, Server messageServer) {
		this.client = client;
		this.messageCenter = messageCenter;
		this.messages = new LinkedList<>();
		this.keepRunning = true;
		this.messageServer = messageServer;
	}

	/**
	 * Adds a message to the queue.
	 * 
	 * @param message
	 *            A message to be added.
	 */
	public synchronized void addMessage(Message message) {
		messages.add(message);
		notify();
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
	 * Waits for messages from server and sends them to the client. Stops when a
	 * system message is received.
	 */
	public void run() {
		while (keepRunning) {
			try {
				Message message = getNextMessageFromQueue();
				if (message == null) {
					keepRunning = false;
					continue;
				}

				if (message.getIsSystemMessage()) {
					if (message.getMessageText() == "logout") {
						// Message sent from client to logout
						messageServer.removeUser(message.getRecipient(), client);
					}

					keepRunning = false;
					sendMessagetoOneUser(message.getSender(), message.getMessageText());
				} else {
					sendMessage(message);
				}
			} catch (InterruptedException e) {
				keepRunning = false;
				e.printStackTrace();
			}
		}
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

		if (recipient.equalsIgnoreCase("all")) {
			sendMessageToAllUsers(sender, messageText);
		} else {
			boolean isUserConnected = messageServer.isUserConnected(recipient);
			if (isUserConnected) {
				sendMessagetoOneUser(recipient, messageText, sender);
			} else {
				String errorMessage = recipient + " is not connected.";
				sendMessagetoOneUser(sender, errorMessage);
			}
		}
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
	private void sendMessagetoOneUser(String sender, String errorMessage) {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
			out.write(errorMessage);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
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
		Map<String, User> clients = messageCenter.getClients();
		User user = clients.get(recipient);

		if (user == null) {
			return;
		}

		try {
			Socket ct = user.getSocket();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			if (!sender.equalsIgnoreCase("admin")) {
				messageText = sender + ": " + messageText;
			}

			out.write(messageText);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
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
		Map<String, User> clients = messageCenter.getClients();
		for (String client : clients.keySet()) {
			if (client == sender) {
				continue;
			}

			User user = clients.get(client);
			Socket ct = user.getSocket();

			try {
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
				messageText = sender + ": " + messageText;
				out.write(messageText);
				out.newLine();
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
	public void disconnect(boolean isClientListenerClosed, String name) {
		Message systemMessage;
		if (isClientListenerClosed) {
			// Generating empty message to close the client sender
			systemMessage = new Message("logout", name, "admin");
		} else {
			// Generating system message to close the client sender and the
			// client listener
			systemMessage = new Message("shutdown", name, "admin");
		}

		addMessage(systemMessage);
	}

	/**
	 * Sends a system message to terminate the client sender.
	 */
	public void stopSender() {
		Message systemMessage = null;
		addMessage(systemMessage);
	}
}
