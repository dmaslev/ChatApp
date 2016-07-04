package chat.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import chat.constants.SystemCode;

public class ClientMessageSender implements Runnable {

	private Socket client;
	private DataOutputStream output;
	private Scanner inputReader;
	private String username;

	// Boolean variable used to stop the run method.
	private boolean isRunning;
	private boolean expextedExitMessage;

	public ClientMessageSender(Socket socket, Scanner inputReader) {
		this.client = socket;
		this.inputReader = inputReader;
	}

	public void run() {
		this.isRunning = true;
		this.expextedExitMessage = false;

		setUsername(username);
		try {
			System.out.println("Enter your message: ");
			while (isRunning) {
				String message = inputReader.nextLine();
				
				if (expextedExitMessage) {
					while (!message.equals(UserCommands.EXIT)) {
						System.out.println("You have been disconnected from server. Enter \"/exit\" to stop the program.");
						message = inputReader.nextLine();
					}
					
					// Exit message is entered. Stop the run method.
					continue;
				}
				
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
				}
			}
		} catch (IOException ioException) {
			isRunning = false;
		} finally {
			inputReader.close();
		}
	}

	public String getUsername() {
		return this.username;
	}

	/**
	 * Sends a message to server listeners.
	 * 
	 * @param message Text message to be sent.
	 * @param recipient Username of the recipient in String format.
	 * @throws IOException If connection error occurs during sending the message.
	 */
	protected void sendMessage(Integer systemCode, String message, String recipient) throws IOException {
		output.writeInt(systemCode);
		output.writeUTF(message);
		output.writeUTF(recipient);
		output.flush();
	}

	/**
	 * 
	 * @param systemCode
	 * @throws IOException
	 */
	protected void sendMessage(Integer systemCode) throws IOException {
		output.writeInt(systemCode);
		output.writeUTF(username);
		output.flush();
	}

	/**
	 * Reads a username in String format and validates it. If the validation
	 * fails recursively calls the method until the validation is passed.
	 * 
	 * @param input
	 *            BufferedReader for reading the input
	 * @throws IOException
	 */
	protected void initializeUsername() throws IOException {
		try {
			output = new DataOutputStream(client.getOutputStream());
		} catch (IOException ioException) {
			ioException.printStackTrace();
			throw new IOException(ioException);
		}

		System.out.print("Enter your username: ");
		try {
			username = inputReader.nextLine();
			while (!validateUsername(username)) {
				System.out.print("Enter your username: ");
				username = inputReader.nextLine();
			}

			sendRegisterMessage(SystemCode.REGISTER, username);

		} catch (IOException ioException) {
			ioException.printStackTrace();
			isRunning = false;
			throw new IOException(ioException);
		}
	}

	protected void setUsername(String name) {
		this.username = name;
	}

	/**
	 * System message for shutting down the client was received. The message sender terminates.
	 */
	protected void shutdown() {
		try {
			expextedExitMessage = true;
			isRunning = false;
			System.out.println("Enter \"/exit\" to stop the program.");
			//todo
			output.close();
			client.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	/**
	 * Send a register message to server.
	 * @param messageCode The system code of the message is used to define the type of the message.
	 * @param username The username of the new client.
	 * @throws IOException 
	 */
	private void sendRegisterMessage(int messageCode, String username) throws IOException {
		output.writeInt(messageCode);
		output.writeUTF(username);
		output.flush();
	}
	
	/**
	 * Stops reading input and sends a system message to stop the client input
	 * listener.
	 */
	private void logout() {
		try {
			output.writeInt(SystemCode.LOGOUT);
			output.writeUTF(username);
			output.flush();
			inputReader.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		isRunning = false;
	}

	/**
	 * Accepts a username, checks if it is a valid username and prints a error
	 * message. Valid username is at least 3 characters long, starts with
	 * English letter and differ from special names /admin, administrator/
	 * 
	 * @param name A username to be validated.
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
