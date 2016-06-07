package chat.serverside;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;

public class MessageCenter extends Thread{

	private Server server;
	private Map<String, User> clients;
	private LinkedList<Message> messagesQueue;

	public MessageCenter(Server server) {
		this.server = server;
		this.clients = this.server.getClients();
		this.messagesQueue = new LinkedList<Message>();
	}
	
	synchronized public void sendMessagetoOneUser(Socket ct, String message) {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(ct.getOutputStream()));
			out.write(message);
			out.newLine();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	synchronized public void registerUser(String name, Socket client, ClientSender messageSender, ClientListener messageListener) {
		server.addUser(name, client, messageSender, messageListener);
	}
	
	public void removeUser(String usernameAttched) {
		server.removeUser(usernameAttched);
	}
	
	public Map<String, User> getClients() {
		return this.clients;
	}

	synchronized public boolean isUserConnected(String username) {
		User client = clients.get(username);
		if (client == null) {
			return false;
		}

		return true;
	}
	
	public synchronized void addMessageToQueue(Message message) {
		messagesQueue.add(message);
		notify();
	}

	private synchronized Message getNextMessageFromQueue() {
		while (messagesQueue.size() == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
			}
		}
		
		Message message = messagesQueue.poll();
		return message;
	}

	private void sendMessage(Message message) {
		clients.get(message.getSender()).getClientSender().addMessage(message);
	}
	
	public void run() {
		while (true) {
			Message message = getNextMessageFromQueue();
			sendMessage(message);
		}
	}
}