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
			if (!registerUser()) {
				// Error was occurred during registration the user.
				isRunning = false;
				return;
			}

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

	protected void closeResources() throws IOException {
		try {
			listener.close();
		} catch (IOException ioException) {
			// Closing the input stream failed. Close the inner stream.
			innerStream.close();
			ioException.printStackTrace();
		}

		try {
			socket.close();
		} catch (IOException ioException) {
			// Closing the socket failed.
			ioException.printStackTrace();
		}

		try {
			messageSender.shutdown();
		} catch (IOException ioException) {
			// There is problem with closing ClientMessageSender's resources.
			ioException.printStackTrace();
		}
	}

	/**
	 * 
	 * @return Returns false if error occurs while opening data input stream,
	 *         reading from input stream or reading result message from socket
	 *         input stream.
	 * @throws IOException
	 */
	private boolean registerUser() throws IOException {
		try {
			// Reads username from the user and sends it for validation to the
			// server.
			messageSender.sendUsernameForValidation();
		} catch (IOException ex) {
			throw new IOException(ex);
		}

		try {
			// Reads and integer code from the server. The value depends on
			// server side validation for the username.
			String result = listener.readLine();
			displayConvertResultCodeToMessage(result);

			// Looping until a message for successful login is received.
			while (!result.equals(SystemCode.SUCCESSFUL_LOGIN)) {
				// TODO reconnect
				messageSender.sendUsernameForValidation();
				result = listener.readLine();
				displayConvertResultCodeToMessage(result);
			}
		} catch (IOException ioException) {
			throw new IOException(ioException);
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
