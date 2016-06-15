package chat.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientMessageSender implements Runnable {
	private BufferedReader inputReader;
	private DataOutputStream output;
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
		try {
			System.out.print("Enter your message: ");
			while (keepRunning) {
				String message = inputReader.readLine();
				if (message.equalsIgnoreCase(UserCommands.logoutCommand)) {
					stopClient();
					keepRunning = false;
				} else if (message.equalsIgnoreCase(UserCommands.exitCommand)) {
					keepRunning = false;
				} else {
					System.out.print("Enter a username or \"all\" to send to all connected users: ");
					String receiver = inputReader.readLine();
					if (receiver.equals("all")) {
						System.out.print("Enter your message: ");
					}
					
					sendMessage(400, message, receiver);
				}
			}
		} catch (IOException ioException) {
			keepRunning = false;
		} finally {
			try {
				inputReader.close();
			} catch (IOException ioException) {
				ioException.printStackTrace();
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
	protected void sendMessage(Integer systemCode, String message, String recipient) throws IOException {
		output.writeInt(systemCode);
		output.writeUTF(message);
		output.writeUTF(recipient);
		output.flush();
	}
	

	private void sendMessage(int messageCode, String username) throws IOException {
		output.writeInt(messageCode);
		output.writeUTF(username);
		output.flush();
	}

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
			throw new IOException(ioException);
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
			// Code 100 is system code for register message.
			sendMessage(100, username);
		} catch (IOException ioException) {
			keepRunning = false;
			throw new IOException(ioException);
		}
	}

	protected String getUsername() {
		return this.username;
	}

	protected void setUsername(String name) {
		this.username = name;
	}

	protected void shutdown() {
		try {
			System.out.println("Enter /exit to stop the program.");
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
			// Code 200 is system code, used for logging out.
			output.writeInt(200);
			output.writeUTF(username);
			output.flush();
			inputReader.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		keepRunning = false;
	}

	/**
	 * Accepts a username, checks if it is a valid username and prints a error
	 * message. Valid username is at least 3 characters long, starts with
	 * English letter and differ from special names /admin, administrator, all/
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
		} else if (name.equalsIgnoreCase("all") || name.equalsIgnoreCase("admin")
				|| name.equalsIgnoreCase("administrator")) {
			System.out.println(name + " can not be used. Please select a new one.");
			return false;
		}

		return true;
	}
}
