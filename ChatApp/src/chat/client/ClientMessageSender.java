package chat.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientMessageSender implements Runnable {
	private final String STOP_COMMAND = "/logout";
	private BufferedReader inputReader;
	private BufferedWriter output;
	private boolean keepRunning;
	private String username;
	private Socket client;

	public ClientMessageSender(Socket socket) {
		this.client = socket;
		this.keepRunning = true;
		this.inputReader = new BufferedReader(new InputStreamReader(System.in));
	}

	public void run() {
		setUsername(username);
		while (keepRunning) {
			try {
				String message = inputReader.readLine();
				if (message.equalsIgnoreCase(STOP_COMMAND)) {
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
			} catch (IOException ioException) {
				keepRunning = false;
				System.out.println("Disconnected from server.");
			}
		}
	}

	/**
	 * Sends a message to server listeners.
	 * 
	 * @param message
	 *            - Text message to be sent.
	 * @param recipient
	 *            - Username of the recipient in String format.
	 * @throws IOException
	 */
	public void sendMessage(String message, String recipient) throws IOException {
		output.write(message);
		output.newLine();
		output.write(recipient);
		output.newLine();
		output.flush();
	}
	
	public void sendMessage(String message) throws IOException {
		output.write(message);
		output.newLine();
		output.write(username);
		output.newLine();
		output.flush();
	}

	/**
	 * Reads a username in String format and validates it. If the validation
	 * fails recursively calls the method until the validation is passed.
	 * 
	 * @param input
	 *            BufferedReader for reading the input
	 */
	public void initializeUsername() {
		try {
			output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		
		System.out.print("Enter your username: ");
		try {
			username = inputReader.readLine();
			while (!validateUsername(username)) {
				System.out.print("Enter your username: ");
				username = inputReader.readLine();
			}
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		try {
			sendMessage("admin-register", username);
		} catch (IOException ioException) {
			// Lost connection with the server. Unable to send message.
			System.out.println("Unable to connect to the server.");
			keepRunning = false;
			ioException.printStackTrace();
		}
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String name) {
		this.username = name;
	}
	
	public void shutdown() {
		try {
			output.close();
			client.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	/**
	 * Stops reading input and sends a system message to stop the client input
	 * listener.
	 */
	private void stopClient() {
		try {
			output.write("logout");
			output.newLine();
			output.write(username);
			output.newLine();
			output.flush();
			inputReader.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		keepRunning = false;
	}

	/**
	 * Accepts a username, checks if it is a valid username and prints a error
	 * message.
	 * 
	 * @param name
	 *            A username to be validated.
	 * @return Returns false if the validation fails and true otherwise.
	 */
	private boolean validateUsername(String name) {
		boolean validUsername = true;
		if (name.length() < 3) {
			System.out.println("Username must be at least 3 characters long.");
			validUsername = false;
		} else if (name.equalsIgnoreCase("all") || name.equalsIgnoreCase("admin")
				|| name.equalsIgnoreCase("administrator") || name.equalsIgnoreCase("/stop")) {
			System.out.println(name + " can not be used. Please select a new one.");
			validUsername = false;
		}

		return validUsername;
	}
}
