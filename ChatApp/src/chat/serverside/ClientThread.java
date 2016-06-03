package chat.serverside;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientThread extends Thread {
	private Socket client;
	private MessageCenter messageCenter;
	private BufferedReader input;
	private String usernameAttched;
	private boolean keepRunning;

	public ClientThread(Socket socket, MessageCenter messageCenter) {
		this.client = socket;
		this.messageCenter = messageCenter;
		this.keepRunning = true;
		try {
			this.input = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Socket getSocket() {
		return this.client;
	}

	public void run() {
		try {
			String username = input.readLine();
			messageCenter.registerUser(username, client, this);
			usernameAttched = username;
		} catch (IOException e1) {
			return;
		}

		while (keepRunning) {
			try {
				String messageReceived = input.readLine();
				String recipient = input.readLine();

				if (!messageReceived.equals(null)) {
					if (recipient.equalsIgnoreCase("all")) {
						messageCenter.sendMessageToAllUsers(usernameAttched, messageReceived);
					} else {
						boolean isUserConnected = messageCenter.isUserConnected(recipient);
						if (isUserConnected) {
							messageCenter.sendMessagetoOneUser(recipient, messageReceived, usernameAttched);
						} else {
							String message = recipient + " is not connected.";
							messageCenter.sendMessagetoOneUser(usernameAttched, message, usernameAttched);
						}
					}
				}
			} catch (IOException e) {
				
			}
		}
	}
	
	public void disconnect() {
		keepRunning = false;
	}
}
