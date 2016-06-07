package chat.serverside;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientListener extends Thread {
	private Socket client;
	private MessageCenter messageCenter;
	private BufferedReader input;
	private String usernameAttched;
	private boolean keepRunning;

	public ClientListener(Socket socket, MessageCenter messageCenter) {
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
			ClientSender clientSender = new ClientSender(client, messageCenter);
			clientSender.start();

			messageCenter.registerUser(username, client, clientSender, this);
			usernameAttched = username;
		} catch (IOException e1) {
			return;
		}

		while (keepRunning) {
			try {
				String messageReceived = input.readLine();
				String recipient = input.readLine();
				if (messageReceived == null) {
					break;
				}
				
				Message message = new Message(messageReceived, recipient, usernameAttched);
				messageCenter.addMessageToQueue(message);
				
			} catch (IOException e) {
				//Connection problem
			}
		}
		
		System.out.println("test");
		messageCenter.removeUser(usernameAttched);
	}
	
	public void disconnect() throws IOException {
		keepRunning = false;
		client.close();
	}
}
