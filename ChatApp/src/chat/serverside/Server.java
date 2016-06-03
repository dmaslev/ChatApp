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
	private Vector<User> connectedUsers;
	private Map<String, Socket> clients;
	private MessageCenter messageCenter;

	public void run() {
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		reader = new Scanner(System.in);
		connectedUsers = new Vector<User>();
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

	public Vector<User> getAllUsers() {
		return this.connectedUsers;
	}

	public Map<String, Socket> getClients() {
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
		for (User user : this.connectedUsers) {
			System.out.println(user.toString());
		}

		System.out.println("__________________________");
	}

	public void addUser(String name) {
		clients.put(name, socket);
		User connectedUser = new User(socket, name);
		connectedUsers.add(connectedUser);
	}
}