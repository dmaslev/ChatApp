package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientMessageListener implements Runnable {
	private BufferedReader listener;
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
				message = listener.readLine();
				if (message == null) {
					// Lost connection
					isConnected = false;
				} else if (message.equalsIgnoreCase("logout")) {
					System.out.println("Disconnected from server.");
					isConnected = false;
				} else if (message.equalsIgnoreCase("shutdown")) {
					System.out.println(
							"You have been disconnected from server. Enter /reconnect to try to reconnect or /exit to stop the program.");
					messageSender.sendMessage("shutdown");
					isConnected = false;
					messageSender.shutdown();
				} else {
					display(message);
				}
			}
		} catch (IOException ioException) {
			System.out.println("Lost connection with server.");
			isConnected = false;
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
			listener = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException ioException) {
			// Socket is closed
			isConnected = false;
			ioException.printStackTrace();
		}

		if (isConnected) {
			try {
				String result = listener.readLine();
				System.out.println(result);
				while (!result.equals("Successfully logged in.")) {
					messageSender.initializeUsername();
					result = listener.readLine();
					System.out.println(result);
				}
			} catch (IOException ioException) {
				isConnected = false;
				ioException.printStackTrace();
			}
		}
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
