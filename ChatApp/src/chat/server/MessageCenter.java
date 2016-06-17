package chat.server;

import java.util.LinkedList;
import java.util.Map;

import chat.constants.SystemCode;

public class MessageCenter extends Thread {

	private Server server;
	private boolean isServerOn;

	private Map<String, User> clients;
	private LinkedList<Message> messagesQueue;

	public MessageCenter(Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		this.isServerOn = true;
		this.messagesQueue = new LinkedList<Message>();
		this.clients = this.server.getClients();

		while (isServerOn) {
			Message message = getNextMessageFromQueue();
			if (message.getIsSystemMessage()) {
				if (message.getSystemCode() == SystemCode.SHUTDOWN) {
					// Server is shut down. A system message is sent to shut
					// down the message center.
					this.isServerOn = false;
				}
			} else {
				sendMessage(message);
			}
		}
	}

	/**
	 * Adds a message to the queue.
	 * 
	 * @param message
	 *            The message to be added.
	 */
	public synchronized void addMessageToQueue(Message message) {
		messagesQueue.add(message);
		notify();
	}

	public Map<String, User> getClients() {
		return this.clients;
	}

	public void disconnect() {
		// Add system message to stop the run method in message center.
		Message systemMessage = new Message(SystemCode.SHUTDOWN);
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

	private synchronized void sendMessage(Message message) {
		clients.get(message.getSender()).getClientSender().addMessage(message);
	}
}