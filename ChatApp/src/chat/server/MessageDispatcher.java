package chat.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageDispatcher {

	private Server server;
	private ExecutorService executorService;

	private boolean keepRunning;

	public MessageDispatcher(Server server) {
		this.server = server;
		this.keepRunning = true;
		initializeExecutor();
	}

	/**
	 * Adds a message to the queue.
	 * 
	 * @param message
	 *            The message to be added.
	 */
	public synchronized boolean addMessageToQueue(Message message) {
		if (keepRunning) {
			MessageSender messageSender = new MessageSender(message, server);
			executorService.execute(messageSender);
			return true;
		}

		return false;
	}

	/**
	 * 
	 * @param shutDownImmediately
	 *            If false all messages currently in the queue will be sent. If
	 *            true the message dispatcher will shut down immediately without
	 *            sending the messages in the queue.
	 */
	void shutdown() {
		// Set keepRunning to false so it is not possible to add mroe messages in the queue.
		this.keepRunning = false;
		this.executorService.shutdown();
	}

	/**
	 * Initialize the thread pool responsible for sending message to the
	 * clients.
	 */
	private void initializeExecutor() {
		int numberOfThread = 10;
		this.executorService = Executors.newFixedThreadPool(numberOfThread);
	}
}