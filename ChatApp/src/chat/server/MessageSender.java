package chat.server;

import java.io.BufferedWriter;
import java.io.IOException;
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
			if (message.getSystemCode().equals(SystemCode.REGISTER)) {
				try {
					sendRegisterMessage(message);
				} catch (IOException e) {
					// Sending message to the client failed. Possible reasons -
					// client has been disconnected or output stream was closed.
					e.printStackTrace();
				}

				return;
			}

			try {
				String text = message.getMessageText();
				String recipient = message.getRecipient();
				sendSystemMessage(text, recipient);
			} catch (IOException e) {
				// Sending message to the client failed. Possible reasons -
				// client has been disconnected or output stream was closed.
				e.printStackTrace();
			}

		} else {
			try {
				sendMessage(message);
			} catch (IOException e) {
				// Sending message to the client failed. Possible reasons -
				// client has been disconnected or output stream was closed.
				e.printStackTrace();
			}
		}
	}

	private void sendRegisterMessage(Message message) throws IOException {
		String resultCode = message.getMessageText();
		BufferedWriter out;
		try {
			out = message.getOutput();
			out.write(resultCode);
			out.newLine();
			out.flush();
			if (!resultCode.equals(SystemCode.SUCCESSFUL_LOGIN)) {
				// Login failed. Shut down the server side listener and close all resources.
				ServersideListener listener = message.getListener();
				server.removeListener(listener);
				listener.shutdown();
			}
		} catch (IOException ioException) {
			throw new IOException("Unable to send the message to client. ", ioException);
		}
	}

	/**
	 * Sends a message to the client. Depending on the message type calls proper
	 * method to send the message.
	 * 
	 * @param message
	 *            A message to be sent.
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
				sendMessagtTOneUser(recipient, messageText, sender);
			} else {
				String errorMessage = recipient + " is not connected.";
				sendSystemMessage(errorMessage, sender);
			}
		}

		sendSystemMessage("Enter your message: ", sender);
	}

	/**
	 * A method used to send system message to only client.
	 * 
	 * @param recipient
	 *            The username of recipient.
	 * @param textMessage
	 *            Information message for the client.
	 * @throws IOException
	 */
	private void sendSystemMessage(String textMessage, String recipient) throws IOException {
		try {
			User user = server.getClients().get(recipient);
			if (user == null) {
				return;
			}

			BufferedWriter out = user.getOutputStream();
			if (out == null) {
				return;
			}

			out.write(textMessage);
			out.newLine();
			out.flush();

			// Inform the user that he has been disconnected. Shut down the
			// listener and remove it from the collection with all listeners.
			server.stopListener(recipient);
		} catch (IOException ioException) {
			throw new IOException("Unable to send the message to " + recipient, ioException);
		}
	}

	/**
	 * Sends message to one user.
	 * 
	 * @param recipient
	 *            The username of recipient.
	 * @param messageText
	 *            The text of the message.
	 * @param sender
	 *            Username of sender of the message.
	 * @throws IOException
	 */
	private void sendMessagtTOneUser(String recipient, String messageText, String sender) throws IOException {
		Map<String, User> clients = server.getClients();
		User user = clients.get(recipient);

		// The user is not logged in.
		if (user == null) {
			String errorMessage = recipient + " is not connected.";
			sendSystemMessage(errorMessage, recipient);
			return;
		}

		try {
			BufferedWriter out = user.getOutputStream();
			if (!sender.equalsIgnoreCase("admin")) {
				messageText = sender + ": " + messageText;
			}

			out.write(messageText);
			out.newLine();
			out.flush();

			if (!sender.equals(recipient)) {
				out.write("Enter your message: ");
				out.newLine();
				out.flush();
			}
		} catch (IOException ioException) {
			throw new IOException("Unable to send the message to " + recipient, ioException);
		}
	}

	/**
	 * Sends message to all connected users. The message is not being sent to
	 * the author.
	 * 
	 * @param sender
	 *            The author of the message.
	 * @param messageText
	 *            The text of the message.
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
				BufferedWriter out = user.getOutputStream();

				out.write(messageText);
				out.newLine();
				out.write("Enter your message: ");
				out.newLine();
				out.flush();
			} catch (IOException ioException) {
				throw new IOException("Unable to send the message to " + client, ioException);
			}
		}
	}
}
