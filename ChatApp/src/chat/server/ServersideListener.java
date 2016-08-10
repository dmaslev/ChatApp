package chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
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
	private DBConnector dbConnector;

	private String username;
	private Date connectedDate;

	public ServersideListener(Socket clientSocket, MessageDispatcher messageDispatcher, DBConnector dbConnector,
			Server messageServer) {
		this.clientSocket = clientSocket;
		this.messageDispatcher = messageDispatcher;
		this.dbConnector = dbConnector;
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
			openResources();

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
					String password = input.readLine();
					// First check if the given username is available.
					String resultCode = messageServer.validateUsername(textReceived);
					// Second check to ensure that no other user logged in with
					// the same username between last check and the time current
					// user is logged in.
					resultCode = messageServer.addUser(textReceived, this);

					if (resultCode.equals("0")) {
						try {
							String sql = "INSERT INTO users (`username`, `password`) VALUES (?, ?)";
							Object[] params = new String[] { textReceived, password };
							this.dbConnector.insert(sql, params);
							loginUser(textReceived);
						} catch (SQLException e) {
							throw new SQLException("Error occured while inserting elemnt in the database", e);
						}
					}

					sendMessageToClient(resultCode);

					if (!resultCode.equals(SystemCode.SUCCESSFUL_LOGIN)) {
						closeRecourses();
						messageServer.removeListener(this);
						keepRunning = false;
						return;
					}

					setUsername(textReceived);
				} else if (messageType.equals(SystemCode.LOGIN)) {
					String password = input.readLine();
					String sql = "SELECT password FROM users WHERE username=?";
					String[] params = new String[] { textReceived };
					ResultSet resultSet = dbConnector.select(sql, params);
					if (resultSet.next()) {
						String userPassword = resultSet.getString("password");
						if (password.equals(userPassword)) {
							sendMessageToClient(SystemCode.SUCCESSFUL_LOGIN);
							messageServer.addUser(textReceived, this);
							loginUser(textReceived);
							setUsername(textReceived);
							continue;
						}
					}
					
					sendMessageToClient(SystemCode.FAILED_LOGIN);
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
			System.err.println("Error occured in ServersideListener. " + Logger.printError(ioException));
		} catch (SQLException e) {
			System.err.println("Connection with the database lost. " + Logger.printError(e));
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

	private void openResources() throws IOException {
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
	}

	private void loginUser(String username) throws SQLException {
		String selectUser= "SELECT * FROM users WHERE username=?";
		Object[] params = new String[] { username };
		
		try {
			ResultSet resultSet = dbConnector.select(selectUser, params);
			if (resultSet.next()) {
				int userID = resultSet.getInt("id_users");
				String ip = this.clientSocket.getInetAddress().toString();
				Date dateLoggedIn = new Date();
				params = new Object[] { userID, ip, dateLoggedIn };
				String sql = "INSERT INTO connections (`id_user`, `ip`, `date_logged_in`) VALUES (?, ?, ?)";
				dbConnector.insert(sql, params);
			}
		} catch (SQLException e) {
			throw new SQLException("Connection with the database lost.", e);
		}
		
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