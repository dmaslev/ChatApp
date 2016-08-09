package chat.server;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import chat.util.Logger;
import chat.util.SystemCode;

public class Server {

	// Default port number is used if user does not provide a port number.
	private final int DEFAULT_PORT = 2222;
	private ServerSocket serverSocket;
	private boolean isRunning;

	// Collection for all connected in users.
	private ArrayList<ServersideListener> serverSideListeners;
	// Hash map with all logged in user user for faster searching when sending
	// message to single user.
	private Map<String, ServersideListener> clients;
	private Map<String, ServersideListener> copyClients;

	private MessageDispatcher messageDispatcher;
	private ServerCommandDispatcher serverCommandDispatcher;
	private DBConnector dbConnector;

	public Server() {
		clients = new HashMap<>();
		copyClients = new HashMap<>();
		serverSideListeners = new ArrayList<>();
	}

	/**
	 * Prints information about all connected users.
	 */
	void printConnectedUsers() {
		synchronized (clients) {
			Set<String> connectedUsernames = clients.keySet();
			if (connectedUsernames.isEmpty()) {
				System.out.println("There are no connected users at the moment.");
				return;
			}

			System.out.println("Connected Users:");
			for (String username : connectedUsernames) {
				ServersideListener client = clients.get(username);
				System.out.println(client.toString());
			}
		}
	}

	/**
	 * Validates the the name of the user. If the validation is passed creates a
	 * new User and adds it in the collection of all connected user.
	 * 
	 * @param name
	 *            The name of the user.
	 * @param messageListener
	 *            Message listener of the user.
	 * @throws SQLException 
	 */
	synchronized String addUser(String name, ServersideListener listener) throws SQLException {
		if (clients.containsKey(name)) {
			// The given name is already in user
			return "1";
		}

		clients.put(name, listener);
		return "0";
	}

	/**
	 * Returns copy of the original collection with all connected users.
	 * 
	 * @return Copy of the collection with all currently connected users.
	 */
	Map<String, ServersideListener> getCopyOfClients() {
		return copyClients;
	}

	/**
	 * Stops waiting for new connections. Calls disconnect method on all
	 * connected users and closes the server socket.
	 * 
	 * @throws IOException
	 */
	void stopServer() throws IOException {
		try {
			serverSocket.close();
		} catch (IOException ioException) {
			// Expected exception. Any thread currently blocked in accept() will
			// throw a SocketException.
			String address = serverSocket.getInetAddress().toString();
			int port = serverSocket.getLocalPort();
			System.err.println("Server socket on address: " + address + ", port: " + port + " was closed."
					+ Logger.printError(ioException));
		}
		
		isRunning = false;

		synchronized (serverSideListeners) {
			Iterator<ServersideListener> iterator = serverSideListeners.iterator();

			while (iterator.hasNext()) {
				ServersideListener serversideListener = iterator.next();

				String username = serversideListener.getUsername();
				if (username != null) {
					// The user is logged in. The username can be used to send
					// disconnect message.
					disconnectUser(username);
					clients.remove(username);
					continue;
				}

				// The user is not logged in. Disconnect message can be sent
				// using the output stream stored in the listener.
				serversideListener.shutdown();
				iterator.remove();
			}
		}

		serverCommandDispatcher.shutdown();
		messageDispatcher.shutdown();
		System.out.println("Server successfully disconnected.");
	}

	/**
	 * Disconnects a user. Terminates the client listener used by the user.
	 * Prints an error message if there is no user with such name.
	 * 
	 * @param name
	 *            The name of the user to be disconnected.
	 */
	synchronized void disconnectUser(String name) {
		Message shutDownMessage = new Message("disconnect", name, "admin", SystemCode.DISCONNECT);
		messageDispatcher.addMessageToQueue(shutDownMessage);
	}

	/**
	 * Removes a use from the collection with all connected users.
	 * 
	 * @param username
	 *            Name of the user to be removed.
	 */
	synchronized void stopListener(String username) {
		// Shut down the listener
		ServersideListener listener = this.clients.get(username);
		if (listener == null) {
			return;
		}

		listener.closeRecourses();
	}

	synchronized void removeListener(ServersideListener listener) {
		this.clients.remove(listener.getUsername());
		this.serverSideListeners.remove(listener);
	}

	/**
	 * Accepts a username, checks if it is a valid username and returns a error
	 * code. Valid username is at least 3 characters long, starts with English
	 * letter and differ from special names /admin, administrator, all/
	 * 
	 * @param name
	 * @return
	 */
	String validateUsername(String name) {
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

	/**
	 * Initializes the server, opens the server socket and waits for user
	 * connections.
	 * 
	 * @param args
	 *            Server port. If null, default value is used.
	 * @throws IOException
	 * @throws SQLException 
	 */
	private void startServer(String[] args) throws IOException, SQLException {
		isRunning = true;

		// TaskCopyClients is responsible for coping the collection with all
		// connected users.
		// Coping the collection is needed for sending message to all users. The
		// iteration is performed on the copy so new users can be added/removed
		// to the original.
		TimerTask taskCopyClients = new TaskCopyClients(clients, copyClients);
		Timer timer = new Timer();
		// First copy is performed immediately and then every 300 000
		// milliseconds (5 minutes) a new copy is made.
		timer.schedule(taskCopyClients, 0, 300000);

		try {
			Scanner reader= new Scanner(System.in);
			isRunning = initializeServer(args, reader);

			if (isRunning) {
				serverCommandDispatcher = new ServerCommandDispatcher(this, reader);
				serverCommandDispatcher.start();
				messageDispatcher = new MessageDispatcher(this);

				waitForConnections();
			}
		} catch (SQLException e) {
			throw new SQLException("Unable to connect to database server.", e);
		} catch (IOException ioException) {
			isRunning = false;
			throw new IOException(ioException);
		} catch (IllegalArgumentException e) {
			isRunning = false;
			throw new IllegalArgumentException(e);
		} finally {
			timer.cancel();
		}
	}

	private void printWelcomeMessage() throws IOException {
		try {
			InetAddress serverAdress = InetAddress.getLocalHost();
			int port = serverSocket.getLocalPort();

			System.out.println("Server adress: " + serverAdress + " on port: " + port);
			System.out.println("Server is ready to accept connections");
			System.out.println("Enter \"/help\" to see the help menu.");
		} catch (UnknownHostException unknownHostException) {
			// Unable to read local address.
			throw new IOException("Local host name could not be resolved into an address.", unknownHostException);
		}
	}

	private void waitForConnections() throws IOException {
		while (isRunning) {
			try {
				Socket socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + " connected");

				ServersideListener clientListener = new ServersideListener(socket, messageDispatcher, dbConnector, this);
				clientListener.start();
				serverSideListeners.add(clientListener);
			} catch (IOException ioException) {
				// Server socket was closed while waiting for connections.
				String address = serverSocket.getLocalSocketAddress().toString();
				int port = serverSocket.getLocalPort();
				throw new IOException("Server socket" + "(address: " + address + ", port: " + port + ") was closed.",
						ioException);
			}
		}
	}

	private boolean initializeServer(String[] args, Scanner reader) throws IOException, IllegalArgumentException, SQLException {
		if (args == null) {
			return false;
		}

		int port = DEFAULT_PORT;
		try {
			if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				if (port < 1 || port > 65535) {
					// Invalid port number
					throw new IllegalArgumentException(args[0] + " is not valid port number.");
				}
			} else if (args.length > 1) {
				System.out.println("Unknow number of arguments. Start the program with only one "
						+ "argument for port number or without any arguments to use the default one.");
				return false;
			}
		} catch (NumberFormatException numberFormatException) {
			throw new IllegalArgumentException(args[0] + " is not a valid port number.", numberFormatException);
		}

		try {
			serverSocket = new ServerSocket(port);
		} catch (BindException bindException) {
			throw new IOException("Port " + port + " is already in use.", bindException);
		}
		
		System.out.println("Enter password for the database sever: ");
		String password = "abcd1234";
		dbConnector = new DBConnector(password);
		dbConnector.connect();

		printWelcomeMessage();
		return true;
	}

	public ServersideListener getServersideListener(String recipient) {
		ServersideListener listener = this.clients.get(recipient);
		return listener;
	}

	public static void main(String[] args) throws IOException, SQLException {
		Server server = new Server();
		server.startServer(args);
	}
}