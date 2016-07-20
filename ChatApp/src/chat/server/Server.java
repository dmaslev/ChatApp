package chat.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import chat.constants.SystemCode;

public class Server {

	// Default port number is used if user does not provide a port number.
	private final int DEFAULT_PORT = 2222;
	private ServerSocket serverSocket;
	private boolean isRunning;

	// Collection for all logged in users.
	private Map<String, User> clients;

	private MessageDispatcher messageDispatcher;
	private ServerCommandDispatcher serverCommandDispatcher;

	/**
	 * Checks in collection with all connected users if there is a user with
	 * provided user name.
	 * 
	 * @param username
	 * @return Returns true if the user is found in the collection with all
	 *         connected users and false otherwise.
	 */
	synchronized boolean isUserConnected(String username) {
		User client = clients.get(username);
		if (client == null) {
			return false;
		}

		return true;
	}

	/**
	 * Prints information about all connected users.
	 */
	void printConnectedUsers() {
		Set<String> connectedUsernames = clients.keySet();
		if (connectedUsernames.isEmpty()) {
			System.out.println("There are no connected users at the moment.");
			return;
		}

		System.out.println("Connected Users:");
		for (String username : connectedUsernames) {
			User client = clients.get(username);
			System.out.println(client.toString());
		}
	}

	/**
	 * Validates the the name of the user. If the validation is passed creates a
	 * new User and adds it in the collection of all connected user.
	 * 
	 * @param name
	 *            The name of the user.
	 * @param client
	 *            Socket of the user.
	 * @param messageSender
	 *            Message sender of the user.
	 * @param messageListener
	 *            Message listener of the user.
	 * @throws IOException
	 */
	synchronized boolean addUser(String name, BufferedWriter output, ServersideListener messageListener)
			throws IOException {
		String resultCode = validateUsername(name);
		sendMessageToClient(output, resultCode);
		if (resultCode.equals(SystemCode.SUCCESSFUL_LOGIN)) {
			String address = messageListener.getSocket().getInetAddress().toString();
			User user = new User(name, output, address);
			clients.put(name, user);
			messageListener.setUsername(name);
			return true;
		}
		
		return false;
	}

	/**
	 * Removes a use from the collection with all connected users.
	 * 
	 * @param username
	 *            Name of the user to be removed.
	 * @param client
	 *            Used if the user is connected, but not logged in with username
	 *            yet.
	 */
	synchronized void removeUser(String username) {
		System.out.println(username + " disconnected.");
		this.clients.remove(username);
	}

	/**
	 * 
	 * @return Returns the collection of all connected users.
	 */
	Map<String, User> getClients() {
		return this.clients;
	}

	/**
	 * Stops waiting for new connections. Calls disconnect method on all
	 * connected users and closes the server socket.
	 * 
	 * @throws IOException
	 */
	void stopServer(boolean waitForMessagesInTheQueue) throws IOException {
		isRunning = false;
		for (String username : clients.keySet()) {
			User user = clients.get(username);
			if (user != null) {

				Message shutDownMessage = new Message("disconnect", username, "admin", SystemCode.DISCONNECT);
				messageDispatcher.addMessageToQueue(shutDownMessage);
			}
		}

		serverCommandDispatcher.shutdown();
		messageDispatcher.shutdown(waitForMessagesInTheQueue);
		try {
			serverSocket.close();
		} catch (IOException ioException) {
			// The socket is already closed.
			throw new IOException(ioException);
		}

		System.out.println("Server successfully disconnected.");
	}

	/**
	 * Disconnects a user. Terminates the client listener used by the user.
	 * Prints an error message if there is no user with such name.
	 * 
	 * @param name
	 *            The name of the user to be disconnected.
	 */
	void disconnectUser(String name) {
		User user = clients.get(name);
		if (user == null) {
			System.out.println(name + " is not connected.");
		} else {
			Message shutDownMessage = new Message("disconnect", name, "admin", SystemCode.DISCONNECT);
			messageDispatcher.addMessageToQueue(shutDownMessage);
		}
	}

	/**
	 * Initializes the server, opens the server socket and waits for user
	 * connections.
	 * 
	 * @param args
	 *            Server port. If null, default value is used.
	 * @throws IOException
	 */
	private void startServer(String[] args) throws IOException {
		isRunning = true;
		try {
			initializeServer(args);

			if (isRunning) {
				messageDispatcher = new MessageDispatcher(this);
				messageDispatcher.start();
				serverCommandDispatcher = new ServerCommandDispatcher(this);
				serverCommandDispatcher.start();

				waitForConnections();
			}
		} catch (IOException ioException) {
			isRunning = false;
			throw new IOException(ioException);
		}
	}

	/**
	 * Accepts a username, checks if it is a valid username and returns a error
	 * code. Valid username is at least 3 characters long, starts with English
	 * letter and differ from special names /admin, administrator, all/
	 * 
	 * @param name
	 * @return
	 */
	private String validateUsername(String name) {
		String resultCode;
		if (!Character.isAlphabetic(name.charAt(0))) {
			resultCode = "4";
		} else if (this.clients.containsKey(name)) {
			resultCode = "1";
		} else if (name.length() < 3) {
			resultCode = "2";
		} else if (name.equalsIgnoreCase("admin") || name.equalsIgnoreCase("administrator")) {
			resultCode = "3";
		} else {
			resultCode = "0";
		}

		return resultCode;
	}

	private void printWelcomeMessage() {
		try {
			InetAddress serverAdress = InetAddress.getLocalHost();
			int port = serverSocket.getLocalPort();

			System.out.println("Server adress: " + serverAdress + " on port: " + port);
			System.out.println("Server is ready to accept connections");
			System.out.println("Enter \"/help\" to see the help menu.");
		} catch (UnknownHostException unknownHostException) {
			// Unable to read local address.
			throw new UncheckedIOException(new IOException(unknownHostException));
		}
	}

	private void waitForConnections() throws IOException {
		while (isRunning) {
			try {
				Socket socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + " connected");

				ServersideListener client = new ServersideListener(socket, messageDispatcher, this);
				client.setOutputStream();
				client.start();
			} catch (IOException ioException) {
				// Server socket was closed while waiting for connections.
				isRunning = false;
				throw new IOException("Server socket was closed.", ioException);
			}
		}
	}

	private synchronized void sendMessageToClient(BufferedWriter output, String resultCode) {
		Message message = new Message(resultCode, output, SystemCode.REGISTER);
		messageDispatcher.addMessageToQueue(message);
	}

	private void initializeServer(String[] args) throws IOException, IllegalArgumentException {
		if (args == null) {
			return;
		}

		int port = DEFAULT_PORT;
		try {
			if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				if (port < 1 || port > 65535) {
					// Invalid port number
					throw new IllegalArgumentException("Invalid port number");
				}
			} else if (args.length > 1) {
				System.out.println("Unknow number of arguments. Start the program with only one "
						+ "argument for port number or without any arguments to use the default one.");
				isRunning = false;
				return;
			}
		} catch (NumberFormatException numberFormatException) {
			throw new IllegalArgumentException(args[0] + " is not a valid port.", numberFormatException);
		}

		try {
			serverSocket = new ServerSocket(port);
		} catch (BindException bindException) {
			isRunning = false;
			throw new IOException("Port " + port + " is already in use.", bindException);
		}

		if (isRunning) {
			// All logged in clients.
			clients = new ConcurrentHashMap<>();

			printWelcomeMessage();
		}
	}

	public static void main(String[] args) throws IOException {
		Server server = new Server();
		server.startServer(args);
	}
}