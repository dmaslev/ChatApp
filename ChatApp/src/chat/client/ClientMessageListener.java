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

	@SuppressWarnings("deprecation")
	public void run() {
		try {
			listener = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}

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

		Thread senderThread = new Thread(messageSender);
		senderThread.start();

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
					messageSender.sendMessage("shutdown");
					isConnected = false;
					messageSender.shutdown();
					senderThread.stop();
				} else {
					display(message);
				}
			} catch (IOException ioException) {
				System.out.println("Lost connection with server.");
				isConnected = false;
			}
		}
		
		try {
			listener.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
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
