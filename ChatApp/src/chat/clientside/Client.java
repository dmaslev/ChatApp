package chat.clientside;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
	private BufferedReader input;
	private BufferedWriter output;
	private Socket socket;
	private Scanner scanner;
	private String serverAdress;
	private String username;

	public Client() throws UnknownHostException, IOException {
		scanner = new Scanner(System.in);
		System.out.print("Host adress: ");
		serverAdress = scanner.nextLine();

		// System.out.print("Server port: ");
		int port = 2222;// Integer.parseInt(scanner.nextLine());
		socket = new Socket(serverAdress, port);

		output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	public void run() {
		initializeUsername();
		ClientMessageListener listener = new ClientMessageListener(input);
		listener.start();

		while (true) {
			String message = scanner.nextLine();
			System.out.print("Enter a username or \"all\" to send to all connected users: ");
			String receiver = scanner.nextLine();
			if (socket.isClosed()) {
				System.out.println("Server disconnected");
				System.exit(0);
			}

			sendMessage(message, receiver);
		}
	}

	private void initializeUsername() {
		System.out.println("Successfully connected to: " + serverAdress);
		System.out.print("Enter your username: ");
		username = scanner.nextLine();
		try {
			output.write(username);
			output.newLine();
			output.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws UnknownHostException, IOException {
		Client client = new Client();
		client.run();
	}

	private void sendMessage(String message, String receiver) {
		try {
			output.write(message);
			output.newLine();
			output.write(receiver);
			output.newLine();
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getUsername() {
		return this.username;
	}
}