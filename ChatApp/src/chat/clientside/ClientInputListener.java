package chat.clientside;

import java.io.BufferedReader;
import java.io.IOException;

public class ClientInputListener extends Thread {
	private BufferedReader listener;
	private Client client;
	private boolean isConnected;
	private String username;

	public ClientInputListener(BufferedReader input, Client client) {
		this.client = client;
		this.listener = input;
		this.isConnected = true;
	}

	public void run() {
		String message;
		while (isConnected) {
			try {
				message = listener.readLine();
				if (message == null) {
					// Lost connection
					isConnected = false;
				} else if (message.equalsIgnoreCase("logout")) {
					System.out.println("Disconnected from server.");
					isConnected = false;
				} else if (message.equalsIgnoreCase("shutdown")) {
					client.sendMessage("shutdown", username);
					isConnected = false;
					client.stopScanner();
				} else {
					display(message);
				}
			} catch (IOException e) {
				System.out.println("Lost connection with server.");
				isConnected = false;
			}
		}
	}

	/**
	 * Reads a usernamer in String format and validates it. If the validation
	 * fails recursively calls the method until the validation is passed.
	 * 
	 * @param scanner
	 *            BufferedReader for reading the input
	 */
	public void initializeUsername(BufferedReader scanner) {
		System.out.print("Enter your username: ");
		try {
			username = scanner.readLine();
			while (!validateUsername(username)) {
				System.out.print("Enter your username: ");
				username = scanner.readLine();
			}
		} catch (IOException e1) {
			// Input reader has been closed
			e1.printStackTrace();
		}

		client.sendMessage("admin-register", username);
		String result;
		try {
			result = listener.readLine();
			if (result.equals("Successfully logged in.")) {
				System.out.println(result);
			} else {
				System.out.println(result);
				initializeUsername(scanner);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		client.setUsername(username);
	}

	/**
	 * Accepts a username, checks if it is a valid username and prints a error message.
	 * @param name A username to be validated.
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

	/**
	 * Accepts a string and displays it to the client.
	 * @param message A message to be displayed
	 */
	private void display(String message) {
		System.out.println(message);
	}
}
