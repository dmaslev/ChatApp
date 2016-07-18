package chat.server;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chat.constants.SystemCode;

public class MessageCenter extends Thread {

	private Server server;
	private boolean isServerOn;
	private ExecutorService executorService;

	private LinkedList<Message> messagesQueue;

	public MessageCenter(Server server) {
		this.server = server;
		initializeExecutor();
	}

	@Override
	public void run() {
		this.isServerOn = true;
		this.messagesQueue = new LinkedList<Message>();

		while (isServerOn || messagesQueue.size() > 0) {
			Message message = getNextMessageFromQueue();

			if (message == null) {
				// Server is shut down. A system message is sent to shut
				// down the message center. Loop while there are messages
				// in the queue.
				this.isServerOn = false;
			} else  {
				MessageSender messageSender = new MessageSender(message, server);
				executorService.execute(messageSender);
			} 
		}
		
		// Wait to send all message then shutdown the thread pool.
		executorService.shutdown();
	}

	/**
	 * Adds a message to the queue.
	 * 
	 * @param message The message to be added.
	 */
	public synchronized void addMessageToQueue(Message message) {
		messagesQueue.add(message);
		notify();
	}

	protected void shutdown() {
		// Add null message to stop the run method in message center.
		Message systemMessage = null;
		addMessageToQueue(systemMessage);
	}

	/**
	 * Adds a system message in the queue to terminate the client sender. If the
	 * client listener is not terminated the system message terminates it.
	 * 
	 * @param name The username of the client.
	 */
	protected void disconnectUser(String name) {
		// Generating systems message to close the client sender
		Message systemMessage = new Message("logout", name, SystemCode.LOGOUT);

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

	private void initializeExecutor() {
		int numberOfThread = 10;
		this.executorService = Executors.newFixedThreadPool(numberOfThread);
	}
}