package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import chat.util.Logger;
import chat.util.SystemCode;

public class ClientMessageListener implements Runnable {

	private Socket socket;
	private BufferedReader listener;
	private InputStreamReader innerStream;
	private ClientMessageSender messageSender;

	// Boolean variable used to stop the run method.
	private boolean isRunning;

	public ClientMessageListener(Socket client, ClientMessageSender messageSender) {
		this.socket = client;
		this.messageSender = messageSender;
	}

	public void run() {
		this.isRunning = true;

		try {
			if (!initUsername()) {
				// Error was occurred during registration the user.
				return;
			}

			// The client successfully logged in and now can send messages.
			Thread senderThread = new Thread(messageSender);
			senderThread.start();

			while (isRunning) {
				String message = listener.readLine();
				if (message.equalsIgnoreCase("logout")) {
					// User asked to logout.
					System.out.println("Successfully logged out.");
					isRunning = false;
				} else if (message.equalsIgnoreCase("disconnect")) {
					// Server was shutdown or removed the user.
					System.out.print("You have been disconnected from server. ");
					isRunning = false;
					messageSender.shutdown();

				} else {
					display(message);
				}
			}
		} catch (IOException ioException) {
			// Unexpected connection lost.
			isRunning = false;
			System.err.print("Lost connection with server. " + Logger.printError(ioException));
		} finally {
			try {
				closeResources();
			} catch (IOException e) {
				System.err.print("Error occured while closing resources." + Logger.printError(e));
			}
		}
	}

	/**
	 * Open data stream used by ClientMessageListener
	 * 
	 * @throws IOException
	 *             If the data stream cannot be opened.
	 */
	public void init() throws IOException {
		try {
			innerStream = new InputStreamReader(socket.getInputStream());
			listener = new BufferedReader(innerStream);
		} catch (IOException ioException) {
			// Unable to open input stream.
			throw new IOException("Unable to open the input stream.", ioException);
		}
	}

	void closeResources() throws IOException {
		try {
			listener.close();
		} catch (IOException ioException) {
			System.err.println(
					"Closing the input stream failed. Close the inner stream." + Logger.printError(ioException));

			try {
				innerStream.close();
			} catch (IOException e) {
				System.err.println("Closing the innter input stream failed. " + Logger.printError(e));
			}
		}

		try {
			socket.close();
		} catch (IOException ioException) {
			System.err.println("Closing the socket failed. " + Logger.printError(ioException));
		}

		messageSender.shutdown();
	}

	private boolean initUsername() throws IOException {
		messageSender.readUsername();
		
		try {
			// Reads and integer code from the server. The value depends on
			// server side validation for the username.
			String result = listener.readLine();
			displayConvertResultCodeToMessage(result);

			while (result.equals(SystemCode.FAILED_LOGIN)) {
				messageSender.readUsername();
				result = listener.readLine();
				displayConvertResultCodeToMessage(result);
			}
			
			// Looping until a message for successful login is received.
			while (!result.equals(SystemCode.SUCCESSFUL_LOGIN)) {
				// Previous try to log in failed. The server has closed the socket so new one must be opened.
				this.socket = messageSender.reconnect();
				init();
				
				messageSender.readUsername();
				result = listener.readLine();
				displayConvertResultCodeToMessage(result);
			}
		} catch (IOException ioException) {
			throw new IOException(
					"Error occured while registering the user. Possible reasons - "
					+ "sending the message to server, opening the socket or data input/ouput stream failed.",
					ioException);
		}
		
		return true;
	}

	/**
	 * Accepts system code sent from server and prints a message depending on
	 * code value.
	 * 
	 * @param result
	 *            Result code sent from server.
	 */
	private void displayConvertResultCodeToMessage(String result) {
		String message = new String();
		switch (result) {
		case "0":
			message = "Successfully logged in.";
			break;
		case "1":
			message = "Selected username is already in use. Please select a new one.";
			break;
		case "2":
			message = "Username must be at least 3 characters long.";
			break;
		case "3":
			message = "Selected username can not be used. Please select a new one.";
			break;
		case "4":
			message = "Username must start with english letter.";
			break;
		case "5":
			message = "Failed to log in. Incorrect username or password.";
			break;
		case "6":
			message = "This user is already logged in.";
			break;
		default:
			message = "Unknown system code.";
			break;
		}

		System.out.println(message);
	}

	/**
	 * Accepts a string and displays it to the client.
	 * 
	 * @param message
	 *            A message to be displayed
	 */
	private void display(String message) {
		System.out.println(message);
		System.out.println("Enter your message: ");
	}
}
