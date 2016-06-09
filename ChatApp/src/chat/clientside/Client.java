package chat.clientside;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	private BufferedReader input;
	private BufferedWriter output;
	private Socket socket;
	private BufferedReader inputReader;
	private String serverAdress;
	private String username;
	private boolean keepRunning;
	private String stopCommand;
	private ClientInputListener listener;
	
	public Client() {
		inputReader = new BufferedReader(new InputStreamReader(System.in));
		this.keepRunning = true;
		this.stopCommand = "/stop";
	}

	public void run() {
		System.out.print("Host adress: ");
		try {
			serverAdress = inputReader.readLine();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		// System.out.print("Server port: ");
		int port = 2222;// Integer.parseInt(scanner.nextLine());
		try {
			socket = new Socket(serverAdress, port);
		} catch (UnknownHostException e1) {
			System.out.println("Unable to connect to" + serverAdress);
			e1.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Unable to connect to" + serverAdress);
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
		listener.initializeUsername(inputReader);
		listener.start();

		while (keepRunning) {
			String message;
			try {
				message = inputReader.readLine();
				if (message.equalsIgnoreCase(stopCommand)) {
					stopClient();
				} else {
					System.out.print("Enter a username or \"all\" to send to all connected users: ");
					String receiver = inputReader.readLine();
					while (receiver.equalsIgnoreCase("admin")) {
						System.out.println("You can not send messages to admin.");
						System.out.print("Enter a username or \"all\" to send to all connected users: ");
						receiver = inputReader.readLine();
					}

					sendMessage(message, receiver);
				}
			} catch (IOException e) {
				keepRunning = false;
				System.out.println("Disconnected from server.");
			}
		}
	}

	public static void main(String[] args) throws UnknownHostException, IOException {
		Client client = new Client();
		client.run();
	}
	
	/**
	 * Sends a message to server listeners.
	 * @param message - Text message to be sent.
	 * @param recipient - Username of the recipient in String format.
	 */
	public void sendMessage(String message, String recipient) {
		try {
			output.write(message);
			output.newLine();
			output.write(recipient);
			output.newLine();
			output.flush();
		} catch (IOException e) {
			System.out.println("Unable to connect to server.");
			keepRunning = false;
			e.printStackTrace();
		}
	}

	/**
	 * Stops reading input and sends a system message to stop the client input listener.
	 */
	private void stopClient() {
		try {
			output.write("admin-logout");
			output.newLine();
			output.write(username);
			output.newLine();
			output.flush();
			inputReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		keepRunning = false;
	}

	/**
	 * Closes the input reader and prints a information message.
	 */
	public void stopScanner() {
		keepRunning = false;
		try {
			this.inputReader.close();
			socket.close();
		} catch (IOException e) {
			System.out.println("Input reader has been already closed.");
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