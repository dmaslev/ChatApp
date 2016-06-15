package chat.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientMessageListener implements Runnable {
	private DataInputStream listener;
	private boolean isConnected;
	private ClientMessageSender messageSender;
	private Socket client;

	public ClientMessageListener(Socket client, ClientMessageSender messageSender) {
		this.client = client;
		this.messageSender = messageSender;
		this.isConnected = true;
	}

	public void run() {
		Thread senderThread = new Thread(messageSender);

		try {
			initializeMessageListener();
			senderThread.start();

			String message;
			while (isConnected) {
				message = listener.readUTF();
				if (message == null) {
					// Lost connection
					isConnected = false;
				} else if (message.equalsIgnoreCase("logout")) {
					System.out.println("Disconnected from server.");
					isConnected = false;
				} else if (message.equalsIgnoreCase("shutdown")) {
					System.out.print("\nYou have been disconnected from server. ");
					// Code 300 is system code for shutdown the client listener.
					messageSender.sendMessage(300);
					isConnected = false;
					messageSender.shutdown();
				} else {
					display(message);
				}
			}
		} catch (IOException ioException) {
			System.out.print("\nLost connection with server. ");
			isConnected = false;
			messageSender.shutdown();
		} finally {
			try {
				listener.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void initializeMessageListener() {
		try {
			listener = new DataInputStream(client.getInputStream());
		} catch (IOException ioException) {
			// Socket is closed
			isConnected = false;
			ioException.printStackTrace();
		}

		if (isConnected) {
			try {
				Integer result = listener.readInt();
				convertResultCodeToMessage(result);
				
				//Code 0 is for successful login.
				while (result != 0) {
					messageSender.initializeUsername();
					result = listener.readInt();
					convertResultCodeToMessage(result);
				}
			} catch (IOException ioException) {
				isConnected = false;
				ioException.printStackTrace();
			}
		}
	}

	private void convertResultCodeToMessage(Integer result) {
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
		default:
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
		System.out.print("Enter your message: ");
	}
}
