package chat.client;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

import chat.util.Logger;
import chat.util.SystemCode;

public class ClientMessageSender implements Runnable {

	private Socket socket;
	private BufferedWriter output;
	private Scanner inputReader;
	private String username;

	// Boolean variable used to stop the run method.
	private boolean isRunning;
	private OutputStreamWriter innerStream;

	public ClientMessageSender(Socket socket, Scanner inputReader) {
		this.socket = socket;
		this.inputReader = inputReader;
	}

	public void run() {
		this.isRunning = true;

		setUsername(username);
		try {
			System.out.println("Enter your message: ");

			while (isRunning) {
				String message = inputReader.nextLine();

				if (message.equalsIgnoreCase(UserCommands.LOGOUT)) {
					// User asked to logout.
					logout();
					isRunning = false;
				} else if (message.equalsIgnoreCase(UserCommands.EXIT)) {
					// Exit command is entered. Stop the run method.
					isRunning = false;
				} else {
					System.out.print("Enter a username or \"/all\" to send to all connected users: ");
					String receiver = inputReader.nextLine();

					sendMessage(SystemCode.REGULAR_MESSAGE, message, receiver);
					if (!receiver.equals(username)) {
						System.out.println("Enter your message: ");
					}
				}
			}
		} catch (IOException ioException) {
			isRunning = false;
			System.err.println("Lost connection with server. " + Logger.printError(ioException));
		} finally {
			inputReader.close();
		}
	}

	public String getUsername() {
		return this.username;
	}

	/**
	 * Open data stream used by ClientMessageSender
	 * 
	 * @throws IOException
	 *             If the data stream cannot be opened.
	 */
	public void init() throws IOException {
		try {
			// There is no need to store OutputSream returned by
			// socket.getOutputStream() because closing the socket will closed
			// it.
			innerStream = new OutputStreamWriter(socket.getOutputStream());
			output = new BufferedWriter(innerStream);
		} catch (IOException ioException) {
			throw new IOException("Unable to open the output stream.", ioException);
		}
	}

	/**
	 * Sends a message to server listeners.
	 * 
	 * @param message
	 *            Text message to be sent.
	 * @param recipient
	 *            Username of the recipient in String format.
	 * @throws IOException
	 *             If connection error occurs during sending the message.
	 */
	void sendMessage(String systemCode, String message, String recipient) throws IOException {
		output.write(systemCode);
		output.newLine();
		output.write(message);
		output.newLine();
		output.write(recipient);
		output.newLine();
		output.flush();
	}

	/**
	 * Opens new socket, input stream and returns the socket.
	 * 
	 * @return The new opened socket
	 * @throws IOException
	 *             Opening the socket or opening the data streams fails.
	 */
	Socket reconnect() throws IOException {
		System.out.println(
				"You have been disconnected. If you want to reconnect please enter host address/ip adress of server/: ");
		try {
			String serverAddress = inputReader.nextLine();
			socket = new Socket(serverAddress, socket.getPort());
		} catch (IOException e) {
			throw new IOException("Error occured while reading from console.", e);
		}

		init();
		return this.socket;
	}

	/**
	 * Reads a username in String format and validates it. If the validation
	 * fails recursively calls the method until the validation is passed.
	 * 
	 * @param input
	 *            BufferedReader for reading the input
	 * @throws IOException
	 */
	void sendUsernameForValidation() throws IOException {
		System.out.print("Enter your username: ");
		try {
			username = inputReader.nextLine();
			while (!validateUsername(username)) {
				System.out.print("Enter your username: ");
				username = inputReader.nextLine();
			}

			sendRegisterMessage(SystemCode.REGISTER, username);

		} catch (IOException ioException) {
			isRunning = false;
			throw new IOException("Unable to send message to server.", ioException);
		}
	}

	void setUsername(String name) {
		this.username = name;
	}

	/**
	 * System message for shutting down the client was received. The message
	 * sender terminates.
	 * 
	 * @throws IOException
	 */
	void shutdown() {
		this.isRunning = false;

		try {
			this.output.close();
		} catch (IOException ioException) {
			System.err.println(
					"Closing the output stream failed. Close the inner stream." + Logger.printError(ioException));

			try {
				innerStream.close();
			} catch (IOException e) {
				System.err.println("Closing the inner output stream failed. " + Logger.printError(ioException));
			}
		}

		try {
			this.socket.close();
		} catch (IOException ioException) {
			System.err.println("Closing the socket failed. " + Logger.printError(ioException));
		}

		System.exit(0);
	}

	/**
	 * Send a register message to server.
	 * 
	 * @param messageCode
	 *            The system code of the message is used to define the type of
	 *            the message.
	 * @param username
	 *            The username of the new client.
	 * @throws IOException
	 */
	private void sendRegisterMessage(String messageCode, String username) throws IOException {
		output.write(messageCode);
		output.newLine();
		output.write(username);
		output.newLine();
		output.flush();
	}

	/**
	 * Stops reading input and sends a system message to stop the client message
	 * listener.
	 * 
	 * @throws IOException
	 */
	private void logout() throws IOException {
		isRunning = false;

		try {
			output.write(SystemCode.LOGOUT);
			output.newLine();
			output.write(username);
			output.newLine();
			output.flush();
		} catch (IOException ioException) {
			throw new IOException(ioException);
		}
	}

	/**
	 * Accepts a username, checks if it is a valid username and prints a error
	 * message. Valid username is at least 3 characters long, starts with
	 * English letter and differ from special names /admin, administrator/
	 * 
	 * @param name
	 *            A username to be validated.
	 * @return Returns false if the validation fails and true otherwise.
	 */
	private boolean validateUsername(String name) {
		if (!Character.isAlphabetic(name.charAt(0))) {
			System.out.println("Username must start with english letter.");
			return false;
		}

		if (name.length() < 3) {
			System.out.println("Username must be at least 3 characters long.");
			return false;
		} else if (name.equalsIgnoreCase("admin") || name.equalsIgnoreCase("administrator")) {
			System.out.println(name + " can not be used. Please select a new one.");
			return false;
		}

		return true;
	}
}
