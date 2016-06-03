package chat.serverside;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

public class Server {
	private ServerSocket serverSocket;
	private final int PORT = 2222;
	private Scanner reader;
	private Socket socket;
	private Map<String, User> clients;
	private MessageCenter messageCenter;

	public void run() {
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
		ServerInputManager serverInput = new ServerInputManager(this, reader);
		serverInput.start();
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

	private void waitForConnections() {
		while (true) {
			try {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress() + " connected");

				ClientThread client = new ClientThread(socket, messageCenter);
				client.start();

			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
		}
	}

	public void stopServer() throws IOException {
		serverSocket.close();
	}

	public void listConnectedUsers() {
		System.out.println("Connected Users:");
		for (String user : this.clients.keySet()) {
			System.out.println(clients.get(user).toString());
		}

		System.out.println("__________________________");
	}

	public void addUser(String name, Socket client) {
		int resultCode = validateUsername(name);
		sendMessageToClient(client, resultCode, name);
		
		if (resultCode == 0) {
			User connectedUser = new User(socket, name);
			clients.put(name, connectedUser);
		}
	}

	private void sendMessageToClient(Socket ct, int successfullyLoggedIn, String name) {
		String message = new String();

		switch (successfullyLoggedIn) {
		case 0: 
			message = "Successfully logged in as " + name;
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
				|| name.equalsIgnoreCase("administrator")) {
			resultCode = 3;
		} else {
			resultCode = 0;
		}

		return resultCode;
	}
}