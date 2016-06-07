package chat.serverside;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;

public class ClientSender extends Thread {
	private LinkedList<Message> messages;
	private MessageCenter messageCenter;
	private Socket client;
	private BufferedWriter output;
	private boolean keepRunning;

	public ClientSender(Socket client, MessageCenter messageCenter) {
		this.client = client;
		this.messageCenter = messageCenter;
		this.messages = new LinkedList<>();
		this.keepRunning = true;
		try {
			this.output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public synchronized void addMessage(Message message) {
		messages.add(message);
		notify();
	}
	
	private synchronized Message getNextMessageFromQueue() throws InterruptedException {
		while (messages.size() == 0) {
			wait();
		}
		
		Message message = messages.getFirst();
		messages.removeFirst();
		return message;
	}
	
	public void run() {
		while (keepRunning) {
			try {
				Message message = getNextMessageFromQueue();
				sendMessage(message);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendMessage(Message message) {
		String sender = message.getSender();
		String messageText = message.getMessageText();
		String recipient = message.getRecipient();
		
		if (recipient.equalsIgnoreCase("all")) {
			sendMessageToAllUsers(sender, messageText);
		} else {
			boolean isUserConnected = messageCenter.isUserConnected(recipient);
			if (isUserConnected) {
				sendMessagetoOneUser(recipient, messageText, sender);
			} else {
				String errorMessage = recipient + " is not connected.";
				sendMessagetoOneUser(sender, errorMessage);
			}
		}
	}

	private void sendMessagetoOneUser(String sender, String errorMessage) {
		Map<String, User> clients = messageCenter.getClients();
		User user = clients.get(sender);
		
		if (user == null) {
			return;
		}

		try {
			Socket ct = user.getSocket();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			out.write(errorMessage);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessagetoOneUser(String recipient, String messageText, String sender) {
		Map<String, User> clients = messageCenter.getClients();
		User user = clients.get(recipient);
		
		if (user == null) {
			return;
		}

		try {
			Socket ct = user.getSocket();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			messageText = sender + ": " + messageText;
			out.write(messageText);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessageToAllUsers(String sender, String messageText) {
		Map<String, User> clients = messageCenter.getClients();
		for (String client : clients.keySet()) {
			if (client == sender) {
				continue;
			}
			
			User user = clients.get(client);
			Socket ct = user.getSocket();

			try {
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
				messageText = sender + ": " + messageText;
				out.write(messageText);
				out.newLine();
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void disconnect() throws IOException {
		keepRunning = false;
		client.close();
	}
}
