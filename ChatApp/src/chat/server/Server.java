package chat.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Server {
	private ServerSocket serverSocket;
	private final int DEFAULT_PORT = 2222;
	private Scanner reader;
	private boolean isServerOn;
	private Socket socket;
	private Map<String, User> clients;
	private MessageCenter messageCenter;
	private ServerInputManager serverInput;

	public static void main(String[] args) {
		Server server = new Server();
		server.run(args);
	}

	/**
	 * 
	 * @param username
	 * @return Returns true if the user is connected and false otherwise.
	 */
	synchronized public boolean isUserConnected(String username) {
		User client = clients.get(username);
		if (client == null) {
			return false;
		}

		return true;
	}

	/**
	 * Initializes the server, opens the server socket and waits for user
	 * connections.
	 * 
	 * @param args
	 */
	public void run(String[] args) {
		initializeServer(args);

		messageCenter = new MessageCenter(this);
		messageCenter.start();
		serverInput = new ServerInputManager(this, reader);
		serverInput.start();
		isServerOn = true;
		waitForConnections();
	}

	/**
	 * Prints information about all connected users.
	 */
	public void listConnectedUsers() {
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
	 * and adds it in the collection of all conencted user.
	 * 
	 * @param name
	 *            Username of the user.
	 * @param client
	 *            Socket of the user.
	 * @param messageSender
	 *            Message sender of the user.
	 * @param messageListener
	 *            Message listener of the user.
	 */
	synchronized public void addUser(String name, Socket client, ClientSender messageSender,
			ClientListener messageListener) {
		int resultCode = validateUsername(name);
		sendMessageToClient(client, resultCode, name);

		if (resultCode == 0) {
			User connectedUser = new User(client, name, messageSender, messageListener);
			messageListener.setUsername(name);
			clients.put(name, connectedUser);
		}
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
	synchronized public void removeUser(String username, Socket client) {
		if (username == null) {
			username = client.getInetAddress().toString();
		}

		System.out.println(username + " disconnected.");
		this.clients.remove(username);
	}

	/**
	 * 
	 * @return Returns the hashmap of all connected users.
	 */
	public Map<String, User> getClients() {
		return this.clients;
	}

	/**
	 * Stops waiting for new connections. Calls disconnect method on all
	 * connected users and closes the server socket.
	 */
	public void stopServer() {
		isServerOn = false;
		for (String username : clients.keySet()) {
			User user = clients.get(username);
			if (user != null) {
				user.getClientSender().disconnect(false, username);
			}
		}

		serverInput.disconnect();
		messageCenter.disconnect();
		try {
			serverSocket.close();
		} catch (IOException ioException) {
			// The socket is already closed.
			ioException.printStackTrace();
		}
	}

	/**
	 * Disconnects a user. Terminates the client listener and client sender used
	 * by the user. Prints an error message if there is no user with such
	 * username.
	 * 
	 * @param name
	 *            The name of the user to be disconnected.
	 */
	public void disconnectUser(String name) {
		User user = clients.get(name);
		if (user == null) {
			System.out.println(name + " is not connected.");
		} else {
			user.getClientSender().disconnect(false, name);
		}
	}

	private int validateUsername(String name) {
		int resultCode;
		if (this.clients.containsKey(name)) {
			resultCode = 1;
		} else if (name.length() < 3) {
			resultCode = 2;
		} else if (name.equalsIgnoreCase("all") || name.equalsIgnoreCase("admin")
				|| name.equalsIgnoreCase("administrator") || name.equalsIgnoreCase("/stop")) {
			resultCode = 3;
		} else {
			resultCode = 0;
		}

		return resultCode;
	}

	private void printWelcomeMessage() throws UnknownHostException {
		InetAddress serverAdress = InetAddress.getLocalHost();
		int port = serverSocket.getLocalPort();

		System.out.println("Server adress: " + serverAdress + " on port: " + port);
		System.out.println("Server is ready to accept conenctions");
		System.out.println("Enter \"/help\" to see the help menu.");
	}

	private void waitForConnections() {
		while (isServerOn) {
			try {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + " connected");

				ClientListener client = new ClientListener(socket, messageCenter, this);
				client.start();
			} catch (IOException ioException) {
				System.out.println("Server successfully disconnected.");
			}
		}
	}

	synchronized private void sendMessageToClient(Socket ct, int successfullyLoggedIn, String name) {
		String message = new String();

		switch (successfullyLoggedIn) {
		case 0:
			message = "Successfully logged in.";
			break;
		case 1:
			message = name + " is already in use. Please select a new one.";
			break;
		case 2:
			message = "Username must be at least 3 characters long.";
			break;
		case 3:
			message = name + " can not be used. Please select a new one.";
			break;
		default:
			break;
		}

		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	private void initializeServer(String[] args) {
		int port = DEFAULT_PORT;
		try {
			if (args.length > 0) {
				port = Integer.parseInt(args[0]);
			}
		} catch (NumberFormatException numberFormatException) {
			System.out.println(args[0] + " is not a valid port. Default value will be used.");
		}

		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		reader = new Scanner(System.in);
		clients = new HashMap<>();

		try {
			printWelcomeMessage();
		} catch (UnknownHostException unknownHostException) {
			unknownHostException.printStackTrace();
		}
	}
}