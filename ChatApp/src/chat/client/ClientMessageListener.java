package chat.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import chat.constants.SystemCode;

public class ClientMessageListener implements Runnable {

	private Socket client;
	private DataInputStream listener;
	private ClientMessageSender messageSender;

	// Boolean variable used to stop the run method.
	private boolean isRunning;

	public ClientMessageListener(Socket client, ClientMessageSender messageSender) {
		this.client = client;
		this.messageSender = messageSender;
	}

	public void run() {
		this.isRunning = true;

		try {
			if (!initializeMessageListener()) {
				// Error was occurred during initializing message listener.
				return;
			}

			Thread senderThread = new Thread(messageSender);
			senderThread.start();

			while (isRunning) {
				String message = listener.readUTF();
				if (message.equalsIgnoreCase("logout")) {
					// User asked to logout.
					System.out.println("Successfully logged out.");
					isRunning = false;
					listener.close();
					client.close();
				} else if (message.equalsIgnoreCase("disconnect")) {
					// Server was shutdown or removed the user.
					System.out.print("You have been disconnected from server. ");
					
					// Sending a system message to stop message sender.
					messageSender.sendMessage(SystemCode.DISCONNECT);
					isRunning = false;
					messageSender.shutdown();
				} else {
					display(message);
				}
			}
		} catch (IOException ioException) {
			// Unexpected connection lost.
			System.out.print("Lost connection with server. ");
			isRunning = false;
			messageSender.shutdown();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @return Returns false if error occurs while opening data input stream,
	 *         reading from input stream or reading result message from socket
	 *         input stream.
	 */
	private boolean initializeMessageListener() {
		try {
			listener = new DataInputStream(client.getInputStream());
		} catch (IOException ioException) {
			// Unable to open input stream.
			ioException.printStackTrace();
			return false;
		}

		if (isRunning) {
			try {
				Integer result = listener.readInt();
				displayConvertResultCodeToMessage(result);

				// Looping until a message for successful login is received.
				while (result != SystemCode.SUCCESSFUL_LOGIN) {
					messageSender.initializeUsername();
					result = listener.readInt();
					displayConvertResultCodeToMessage(result);
				}
			} catch (IOException ioException) {
				ioException.printStackTrace();
				return false;
			}
		}

		return true;
	}

	/**
	 * Accepts system code sent from server and prints a message depending on
	 * code value.
	 * 
	 * @param result Result code sent from server.
	 */
	private void displayConvertResultCodeToMessage(Integer result) {
		String message = new String();
		switch (result) {
		case 0:
			message = "Successfully logged in.";
			break;
		case 1:
			message = "Selected username is already in use. Please select a new one.";
			break;
		case 2:
			message = "Username must be at least 3 characters long.";
			break;
		case 3:
			message = "Selected username can not be used. Please select a new one.";
			break;
		case 4:
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
	 * @param message A message to be displayed
	 */
	private void display(String message) {
		System.out.println(message);
	}
}
