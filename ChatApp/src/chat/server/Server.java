package chat.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import chat.constants.SystemCode;

public class Server {

	// Default port number is used if user does not provide a port number.
	private final int DEFAULT_PORT = 2222;
	private ServerSocket serverSocket;
	private boolean keepRunning;

	// Collection for all logged in users.
	private Map<String, User> clients;

	// Collection for connected users, before they log in.
	private HashSet<Socket> connectedClients;

	private MessageCenter messageCenter;
	private ServerInputManager serverInput;

	/**
	 * Checks in collection with all connected users if there is a user with
	 * provided username.
	 * 
	 * @param username
	 * @return Returns true if the user is found in the collection with all 
	 * connected users and false otherwise.
	 */
	protected synchronized boolean isUserConnected(String username) {
		User client = clients.get(username);
		if (client == null) {
			return false;
		}

		return true;
	}

	/**
	 * Prints information about all connected users.
	 */
	protected void listConnectedUsers() {
		if (this.clients.keySet().isEmpty()) {
			System.out.println("There are no connected users at the moment.");
		} else {
			System.out.println("Connected Users:");
			for (String username : this.clients.keySet()) {
				User client = clients.get(username);
				System.out.println(client.toString());
			}
		}
	}

	/**
	 * Validates the username. If the validation is passed creates a new User
	 * and adds it in the collection of all connected user.
	 * 
	 * @param name Username of the user.
	 * @param client Socket of the user.
	 * @param messageSender Message sender of the user.
	 * @param messageListener Message listener of the user.
	 */
	protected synchronized void addUser(String name, Socket client, ServersideListener messageListener) {
		int resultCode = validateUsername(name);
		try {
			sendMessageToClient(client, resultCode);

			if (resultCode == SystemCode.SUCCESSFUL_LOGIN) {
				User connectedUser = new User(client, name);
				connectedUser.setOutputStream();
				
				messageListener.setUsername(name);
				clients.put(name, connectedUser);
				connectedClients.remove(client);
			}
		} catch (IOException ioException) {
			System.out.println("User has been disconnected. Unable to send the message.");
			ioException.printStackTrace();
		}
	}

	/**
	 * Removes a use from the collection with all connected users.
	 * 
	 * @param username Name of the user to be removed.
	 * @param client Used if the user is connected, but not logged in with
	 *            username yet.
	 */
	protected synchronized void removeUser(String username) {
		System.out.println(username + " disconnected.");
		this.clients.remove(username);
	}

	/**
	 * 
	 * @return Returns the collection of all connected users.
	 */
	protected Map<String, User> getClients() {
		return this.clients;
	}

	/**
	 * Stops waiting for new connections. Calls disconnect method on all
	 * connected users and closes the server socket.
	 */
	protected void stopServer() {
		keepRunning = false;
		for (String username : clients.keySet()) {
			User user = clients.get(username);
			if (user != null) {
				Message shutDownMessage = new Message("disconnect", username, SystemCode.DISCONNECT);
				messageCenter.addMessageToQueue(shutDownMessage);
			}
		}

		for (Socket socket : connectedClients) {
			try {
				socket.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		serverInput.disconnect();
		messageCenter.shutdown();
		try {
			serverSocket.close();
		} catch (IOException ioException) {
			// The socket is already closed.
			ioException.printStackTrace();
		}

		System.out.println("Server successfully disconnected.");
	}

	/**
	 * Disconnects a user. Terminates the client listener and client sender used
	 * by the user. Prints an error message if there is no user with such
	 * username.
	 * 
	 * @param name The name of the user to be disconnected.
	 */
	protected void disconnectUser(String name) {
		User user = clients.get(name);
		if (user == null) {
			System.out.println(name + " is not connected.");
		} else {
			Message shutDownMessage = new Message("disconnect", name, SystemCode.DISCONNECT);
			messageCenter.addMessageToQueue(shutDownMessage);
		}
	}

	/**
	 * Initializes the server, opens the server socket and waits for user
	 * connections.
	 * 
	 * @param args Server port. If null, default value is used.
	 */
	private void startServer(String[] args) {
		keepRunning = true;
		try {
			initializeServer(args);

			if (keepRunning) {
				messageCenter = new MessageCenter(this);
				messageCenter.start();
				serverInput = new ServerInputManager(this);
				serverInput.start();

				waitForConnections();
			}
		} catch (BindException bindException) {
			bindException.printStackTrace();
		} catch (IOException | IllegalArgumentException exception) {
			keepRunning = false;
			exception.printStackTrace();
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
	private int validateUsername(String name) {
		int resultCode;
		if (!Character.isAlphabetic(name.charAt(0))) {
			resultCode = 4;
		} else if (this.clients.containsKey(name)) {
			resultCode = 1;
		} else if (name.length() < 3) {
			resultCode = 2;
		} else if (name.equalsIgnoreCase("admin") || name.equalsIgnoreCase("administrator")) {
			resultCode = 3;
		} else {
			resultCode = 0;
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
			unknownHostException.printStackTrace();
		}
	}

	private void waitForConnections() {
		while (keepRunning) {
			try {
				Socket socket = serverSocket.accept();
				connectedClients.add(socket);
				System.out.println(socket.getInetAddress() + " connected");

				ServersideListener client = new ServersideListener(socket, messageCenter, this);
				client.start();
			} catch (IOException ioException) {
				// Server socket was closed while waiting for connections.
				keepRunning = false;
			}
		}
	}

	private synchronized void sendMessageToClient(Socket ct, int resultCode) {
		String text = "" + resultCode;
		Message message = new Message(text, ct, SystemCode.REGISTER);
		messageCenter.addMessageToQueue(message);
	}

	private void initializeServer(String[] args) throws IOException, NumberFormatException, BindException {
		int port = DEFAULT_PORT;
		try {
			if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				if (port < 1 || port > 65535) {
					// Invalid port number
					throw new IllegalArgumentException("Invalud port number");
				}
			} else if (args.length > 1) {
				System.out.println("Unknow number of arguments. Start the program with only one "
						+ "argument for port number or without any arguments to use the default one.");
				keepRunning = false;
			}
		} catch (NumberFormatException numberFormatException) {
			System.out.println(args[0] + " is not a valid port.");
			numberFormatException.printStackTrace();
			throw new IllegalArgumentException(numberFormatException);
		}

		try {
			serverSocket = new ServerSocket(port);
		} catch (BindException bindException) {
			System.out.println("Port " + port + " is already in use.");
			keepRunning = false;
			throw new IOException(bindException);
		}

		if (keepRunning) {
			// All logged in clients.
			clients = new HashMap<>();

			// Collection with connected, but not logged in users.
			connectedClients = new HashSet<>();
			printWelcomeMessage();
		}
	}

	public static void main(String[] args) {
		Server server = new Server();
		server.startServer(args);
	}
}