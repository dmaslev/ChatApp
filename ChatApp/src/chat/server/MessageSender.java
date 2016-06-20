package chat.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import chat.constants.SystemCode;

public class MessageSender implements Runnable {
	
	private Message message;
	private Server server;

	public MessageSender(Message message, Server server) {
		this.message = message;
		this.server = server;
	}

	@Override
	public void run() {
		if (message.getIsSystemMessage()) {
			sendSystemMessage(message.getSystemCode(), message);
			server.removeUser(message.getSender());

		} else {
			sendMessage(message);
		}
	}
	

	private void sendSystemMessage(int systemCode, Message message) {
		String text = new String();
		if (systemCode == SystemCode.SHUTDOWN) {
			text = "shutdown";
		} else if(systemCode == SystemCode.LOGOUT) {
			text = "logout";
		}
		
		sendMessagetoOneUser(text, message.getSender());
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
	private void sendMessagetoOneUser(String errorMessage, String sender) {
		try {
			Socket client = server.getClients().get(sender).getSocket();
			DataOutputStream out = new DataOutputStream(client.getOutputStream());
			out.writeUTF(errorMessage);
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
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

		if (recipient.equalsIgnoreCase("/all")) {
			sendMessageToAllUsers(sender, messageText);
		} else {
			boolean isUserConnected = server.isUserConnected(recipient);
			if (isUserConnected) {
				sendMessagetoOneUser(recipient, messageText, sender);
			} else {
				String errorMessage = recipient + " is not connected.";
				sendMessagetoOneUser(errorMessage, sender);
			}
		}
		
		sendMessagetoOneUser("Enter your message: ", sender);
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
		Map<String, User> clients = server.getClients();
		User user = clients.get(recipient);

		// The user is not logged in.
		if (user == null) {
			String errorMessage = recipient + " is not connected.";
			sendMessagetoOneUser(errorMessage, recipient);
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

		for (String client : server.getClients().keySet()) {
			if (client.equals(sender)) {
				// Skip sending the message to the sender.
				continue;
			}
			
			User user = server.getClients().get(client);

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
