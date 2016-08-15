package chat.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import chat.util.Logger;

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
			try {
				String text = message.getMessageText();
				String recipient = message.getRecipient();
				sendSystemMessage(text, recipient);
			} catch (IOException e) {
				System.err.println("Sending message to the client failed. Posible reasons - "
						+ " client has been disconnected or output stream was closed." + Logger.printError(e));
			}
		} else {
			try {
				sendMessage(message);
			} catch (IOException e) {
				System.err.println("Sending message to the client failed. Posible reasons - "
						+ " client has been disconnected or output stream was closed." + Logger.printError(e));
			} catch (SQLException e) {
				System.err.println("Unable to connect to the database." + Logger.printError(e));
			}
		}
	}

	/**
	 * Sends a message to the client. Depending on the message type calls proper
	 * method to send the message.
	 * 
	 * @param message
	 *            A message to be sent.
	 * @throws IOException
	 * @throws SQLException 
	 */
	private void sendMessage(Message message) throws IOException, SQLException {
		String sender = message.getSender();
		String messageText = message.getMessageText();
		String recipient = message.getRecipient();

		try {
			sendMessageToOneUser(recipient, messageText, sender);
		} catch (IOException e) {
			throw new IOException("Error occured while sending message to client.", e);
		}

	}

	/**
	 * A method used to send system message to only client.
	 * 
	 * @param recipient
	 *            The name of the recipient.
	 * @param textMessage
	 *            Information message for the client.
	 * @throws IOException
	 */
	private void sendSystemMessage(String textMessage, String recipient) throws IOException {
		try {
			ServersideListener client = server.getServersideListener(recipient);
			if (client == null) {
				return;
			}

			BufferedWriter out = client.getOutputStream();
			if (out == null) {
				return;
			}

			out.write(textMessage);
			out.newLine();
			out.flush();

			// Inform the user that he has been disconnected. Shut down the
			// listener and remove it from the collection with all listeners.
			if (textMessage.equals("disconnect")) {
				server.stopListener(recipient);
				ServersideListener listener = server.getServersideListener(recipient);
				server.removeListener(listener);
				listener.closeRecourses();
			}
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
	 * @throws SQLException 
	 */
	private void sendMessageToOneUser(String recipient, String messageText, String sender) throws IOException, SQLException {
		ServersideListener client = server.getServersideListener(recipient);

		if (client == null) {
			// Recipient was disconnected after last check. Unable to send the
			// message. Send error message to the sender to inform him what
			// happened.
			String errorMessage = recipient + " is not connected.";
			sendSystemMessage(errorMessage, sender);
			return;
		}

		try {
			BufferedWriter out = client.getOutputStream();
			if (!sender.equalsIgnoreCase("admin")) {
				messageText = sender + ": " + messageText;
				insertMessage(sender, recipient, messageText);
			}

			out.write(messageText);
			out.newLine();
			out.flush();

		} catch (IOException ioException) {
			// Send message back to the sender to inform that the original message was
			// not sent.
			String text = "Failed to send message to: " + recipient;
			sendMessageToOneUser(sender, text, "admin");
			throw new IOException("Unable to send the message to: " + recipient, ioException);
		}
	}

	private void insertMessage(String sender, String recipient, String text) throws SQLException {
		String selectUser = "SELECT id_users FROM users WHERE username=?";
		Object[] params = new Object[] { sender };
		int userOne = 0;
		int userTwo = 0;
		String sql = new String();
		try {
			ResultSet resultSet = server.getDbConnector().select(selectUser, params);
			if (resultSet.next()) {
				userOne = resultSet.getInt("id_users");
			}
			
			if (sender.equals(recipient)) {
				userTwo = userOne;
			} else {
				params = new Object[] { recipient };
				resultSet = server.getDbConnector().select(selectUser, params);
				
				if (resultSet.next()) {
					userTwo = resultSet.getInt("id_users");
				}
			}
			
			sql = "INSERT INTO messages (`text`, `date`, `sender`, `recipient`) VALUES (?, ?, ?, ?)";
			Date currentDate = new Date();
			params = new Object[] { text, currentDate, userOne, userTwo };
			server.getDbConnector().insert(sql, params);
		} catch (SQLException e) {
			throw new SQLException("Unable to execute sql query: " + sql, e);
		}
	}
}
