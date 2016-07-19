package chat.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import chat.constants.SystemCode;

public class ClientMessageListener implements Runnable {

	private Socket socket;
	private DataInputStream listener;
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
				String message = listener.readUTF();
				if (message.equalsIgnoreCase("logout")) {
					// User asked to logout.
					System.out.println("Successfully logged out.");
					isRunning = false;
				} else if (message.equalsIgnoreCase("disconnect")) {
					// Sending a system message to stop message sender.
					messageSender.sendMessage(SystemCode.DISCONNECT);
					
					// Server was shutdown or removed the user.
					System.out.print("You have been disconnected from server. ");
					System.out.println("Enter \"/exit\" to stop the program.");
					
					isRunning = false;
				} else {
					display(message);
				}
			}
		} catch (IOException ioException) {
			// Unexpected connection lost.
			System.out.print("Lost connection with server. ");
			ioException.printStackTrace();
			isRunning = false;
		} finally {
			try {
				closeResources();
			} catch (IOException e) {
				e.printStackTrace();
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
			listener = new DataInputStream(socket.getInputStream());
		} catch (IOException ioException) {
			// Unable to open input stream.
			throw new IOException("Unable to open the input stream. ", ioException);
		}
	}
	
	protected void closeResources() throws IOException {
		try {
			listener.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		
		try {
			socket.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		
		try {
			messageSender.shutdown();
		} catch (IOException e) {
			// There is problem with closing ClientMessageSender's resources.
			throw new IOException(e);
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
			messageSender.initializeUsername();
		} catch (IOException ex) {
			throw new IOException(ex);
		}

		try {
			// Reads and integer code from the server. The value depends on
			// server side validation for the username.
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

		return true;
	}

	/**
	 * Accepts system code sent from server and prints a message depending on
	 * code value.
	 * 
	 * @param result
	 *            Result code sent from server.
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
	 * @param message
	 *            A message to be displayed
	 */
	private void display(String message) {
		System.out.println(message);
	}
}
