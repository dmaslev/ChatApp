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
			if (message.getSystemCode() == SystemCode.REGISTER) {
				try {
					sendRegisterMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			} 
			
			try {
				String text = message.getMessageText();
				String recipient = message.getRecipient();
				sendSystemMessageToOneUser(text, recipient);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (message.getSystemCode() == SystemCode.LOGOUT) {
				server.removeUser(message.getRecipient());
			}
		} else {
			try {
				sendMessage(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendRegisterMessage(Message message) throws IOException {
		Socket ct = message.getSocket();
		int resultCode = Integer.parseInt(message.getMessageText());
		DataOutputStream out;
		try {
			out = new DataOutputStream(ct.getOutputStream());
			out.writeInt(resultCode);
			out.flush();
		} catch (IOException ioException) {
			throw new IOException(ioException);
		}
		
	}

	/**
	 * Sends a message to the client. Depending on the message type calls 
	 * proper method to send the message.
	 * 
	 * @param message A message to be sent.
	 * @throws IOException 
	 */
	private void sendMessage(Message message) throws IOException {
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
				sendSystemMessageToOneUser(errorMessage, sender);
			}
		}
		
		sendSystemMessageToOneUser("Enter your message: ", sender);
	}

	/**
	 * A method used to send system message to only client.
	 * 
	 * @param recipient The username of recipient.
	 * @param textMessage Information message for the client.
	 * @throws IOException 
	 */
	private void sendSystemMessageToOneUser(String textMessage, String recipient) throws IOException {
		try {
			Socket client = server.getClients().get(recipient).getSocket();
			DataOutputStream out = new DataOutputStream(client.getOutputStream());
			System.out.println(textMessage);
			out.writeUTF(textMessage);
			out.flush();
		} catch (IOException ioException) {
			throw new IOException(ioException);
		}
	}

	/**
	 * Sends message to one user.
	 * 
	 * @param recipient The username of recipient.
	 * @param messageText The text of the message.
	 * @param sender Username of sender of the message.
	 * @throws IOException 
	 */
	private void sendMessagetoOneUser(String recipient, String messageText, String sender) throws IOException {
		Map<String, User> clients = server.getClients();
		User user = clients.get(recipient);

		// The user is not logged in.
		if (user == null) {
			String errorMessage = recipient + " is not connected.";
			sendSystemMessageToOneUser(errorMessage, recipient);
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
	 * Sends message to all connected users. The message is not being sent to 
	 * the author.
	 * 
	 * @param sender The author of the message.
	 * @param messageText The text of the message.
	 * @throws IOException 
	 */
	private void sendMessageToAllUsers(String sender, String messageText) throws IOException {
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
				out.writeUTF("Enter your message: ");

				out.flush();
			} catch (IOException ioException) {
				throw new IOException("Unable to send the message to " + client, ioException);
			}
		}
	}
}
