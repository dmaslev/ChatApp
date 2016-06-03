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

	public ClientThread(Socket socket, MessageCenter messageCenter) {
		this.client = socket;
		this.messageCenter = messageCenter;

		try {
			this.input = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Socket getSocket() {
		return this.client;
	}

	public void run() {
		try {
			String username = input.readLine();
			messageCenter.sendUsername(username);
			usernameAttched = username;
		} catch (IOException e1) {
			return;
		}

		while (true) {
			// if (client.isClosed()) {
			// break;
			// }

			try {
				String messageReceived = input.readLine();
				String recipient = input.readLine();

				if (!messageReceived.equals(null)) {
					if (recipient.equalsIgnoreCase("all")) {
						messageCenter.sendMessageToAllUsers(messageReceived);
					} else {
						boolean isUserConnected = messageCenter.isUserConnected(recipient);
						if (isUserConnected) {
							messageCenter.sendMessagetoOneUser(recipient, messageReceived);
						} else {
							String message = recipient + " is not connected.";
							messageCenter.sendMessagetoOneUser(usernameAttched, message);
						}
					}
				}
			} catch (IOException e) {
				break;
			}
		}
	}
}
