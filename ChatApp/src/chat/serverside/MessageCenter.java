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
	private boolean isServerOn;

	public MessageCenter(Server server) {
		this.server = server;
		this.clients = this.server.getClients();
		this.messagesQueue = new LinkedList<Message>();
		this.isServerOn = true;
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
	
	public Map<String, User> getClients() {
		return this.clients;
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
				e.printStackTrace();
			}
		}
		
		Message message = messagesQueue.poll();
		return message;
	}

	private void sendMessage(Message message) {
		clients.get(message.getSender()).getClientSender().addMessage(message);
	}
	
	public void run() {
		while (isServerOn) {
			Message message = getNextMessageFromQueue();
			if (message.getIsSystemMessage()) {
				if (message.getMessageText().equalsIgnoreCase("shutdown")) {
					this.isServerOn = false;
				}
			} else {
				sendMessage(message);
			}
		}
	}

	public void disconnect() {
		Message systemMessage = new Message("shutdown", "admin", "admin");
		addMessageToQueue(systemMessage);
	}
}