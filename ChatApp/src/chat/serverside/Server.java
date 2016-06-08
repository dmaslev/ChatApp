package chat.serverside;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Server {
	private ServerSocket serverSocket;
	private final int PORT = 2222;
	private Scanner reader;
	private boolean isServerOn;
	private Socket socket;
	private Map<String, User> clients;
	private MessageCenter messageCenter;
	ServerInputManager serverInput;

	public void run() throws IOException {
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		reader = new Scanner(System.in);
		clients = new HashMap<>();

		try {
			printWelcomeMessage();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		messageCenter = new MessageCenter(this);
		messageCenter.start();
		serverInput = new ServerInputManager(this, reader);
		serverInput.start();
		isServerOn = true;
		waitForConnections();
	}

	public Map<String, User> getClients() {
		return this.clients;
	}

	public static void main(String[] args) throws IOException {
		Server server = new Server();
		server.run();
	}

	private void printWelcomeMessage() throws UnknownHostException {
		InetAddress serverAdress = InetAddress.getLocalHost();

		System.out.println("Server adress: " + serverAdress + " on port: " + serverSocket.getLocalPort());
		System.out.println("Server is ready to accept conenctions");
		System.out.println("Enter \"listall\" to see all connected users or \"disconnect\" to stop the server.");
	}

	private void waitForConnections() throws IOException {
		while (isServerOn) {
			try {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + " connected");
				
				ClientListener client = new ClientListener(socket, messageCenter, this);
				client.start();
			} catch (IOException e) {
				System.out.println("Server successfully disconnected.");
			}
		}
	}

	public void stopServer() throws IOException {
		isServerOn = false;
		for (String user : clients.keySet()) {
			try {
				clients.get(user).getClientSender().disconnect();
				clients.get(user).getClientListener().disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		serverInput.disconnect();
		messageCenter.disconnect();
		serverSocket.close();
	}

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

	public void addUser(String name, Socket client, ClientSender messageSender, ClientListener messageListener) {
		int resultCode = validateUsername(name);
		sendMessageToClient(client, resultCode, name);

		if (resultCode == 0) {
			User connectedUser = new User(client, name, messageSender, messageListener);
			messageListener.setUsername(name);
			clients.put(name, connectedUser);
		}
	}
	
	public void removeUser(String username) {
		System.out.println(username + " disconnected.");
		this.clients.remove(username);
	}

	synchronized public void registerUser(String name, Socket client, ClientSender messageSender, ClientListener messageListener) {
		addUser(name, client, messageSender, messageListener);
	}
	
	synchronized public boolean isUserConnected(String username) {
		User client = clients.get(username);
		if (client == null) {
			return false;
		}

		return true;
	}

	private void sendMessageToClient(Socket ct, int successfullyLoggedIn, String name) {
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

		messageCenter.sendMessagetoOneUser(ct, message);
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
}