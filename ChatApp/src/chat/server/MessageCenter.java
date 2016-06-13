package chat.server;

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

	/**
	 * Adds a message to the queue.
	 * @param message The message to be added.
	 */
	public synchronized void addMessageToQueue(Message message) {
		messagesQueue.add(message);
		notify();
	}

	public Map<String, User> getClients() {
		return this.clients;
	}
	

	public void disconnect() {
		Message systemMessage = new Message("shutdown", "admin", "admin");
		addMessageToQueue(systemMessage);
	}
	
	/**
	 * 
	 * @return First message in the queue.
	 */
	private synchronized Message getNextMessageFromQueue() {
		while (messagesQueue.size() == 0) {
			try {
				wait();
			} catch (InterruptedException interruptedException) {
				interruptedException.printStackTrace();
			}
		}
		
		Message message = messagesQueue.poll();
		return message;
	}

	synchronized private void sendMessage(Message message) {
		clients.get(message.getSender()).getClientSender().addMessage(message);
	}
}