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
	private boolean keepRunning;
	private String stopCommand;
	private ClientInputListener listener;

	public Client() {
		scanner = new Scanner(System.in);
		System.out.print("Host adress: ");
		serverAdress = scanner.nextLine();
		this.keepRunning = true;
		this.stopCommand = "/stop";
	}

	public void run() {
		// System.out.print("Server port: ");
		int port = 2222;// Integer.parseInt(scanner.nextLine());
		try {
			socket = new Socket(serverAdress, port);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Successfully connected to: " + serverAdress);
		listener = new ClientInputListener(input, this);
		listener.initializeUsername(scanner);
		listener.start();

		while (keepRunning) {
			String message = scanner.nextLine();
			if (message.equalsIgnoreCase(stopCommand)) {
				stopClient();
			} else {
				System.out.print("Enter a username or \"all\" to send to all connected users: ");
				String receiver = scanner.nextLine();
				while(receiver.equalsIgnoreCase("admin")) {
					System.out.println("You can not send messages to admin.");
					System.out.print("Enter a username or \"all\" to send to all connected users: ");
					receiver = scanner.nextLine();
				}
				
				sendMessage(message, receiver);
			}
		}
	}

	public static void main(String[] args) throws UnknownHostException, IOException {
		Client client = new Client();
		client.run();
	}

	public void sendMessage(String message, String receiver) {
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

	private void stopClient() {
		try {
			output.write("admin-logout");
			output.newLine();
			output.write(username);
			output.newLine();
			output.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		keepRunning = false;
		listener.shutDown();
		try {
			socket.getInputStream().close();
		} catch (IOException e) {
			System.out.println("already closed.");
			e.printStackTrace();
		}
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String name) {
		this.username = name;
	}
}