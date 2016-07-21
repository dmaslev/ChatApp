package chat.server;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import chat.constants.SystemCode;

public class MessageDispatcher extends Thread {

	private Server server;
	private boolean keepRunning;
	private boolean shutDownImmediately;
	private ExecutorService executorService;

	private LinkedList<Message> messagesQueue;

	public MessageDispatcher(Server server) {
		this.server = server;
		initializeExecutor();
	}

	@Override
	public void run() {
		this.keepRunning = true;
		this.shutDownImmediately = true;
		this.messagesQueue = new LinkedList<Message>();

		// If the boolean variable keepRunning is set to false all messages
		// currently in the queue will be sent and after that the run method
		// will stop.
		while (keepRunning || messagesQueue.size() > 0) {
			if (shutDownImmediately) {
				// Server asked to shut down the messageDispatcher immediately.
				break;
			}
			
			if (!interrupted()) {
				Message message = null;;
				try {
					message = getNextMessageFromQueue();
				} catch (InterruptedException e) {
					// getNexMessageFromQueue was interrupted. Server asked to shutdown
					e.printStackTrace();
					continue;
				}

				MessageSender messageSender = new MessageSender(message, server);
				
				// What will happen if all threads are busy.
				// Creates a thread pool that reuses a fixed number of threads
				// operating off a shared unbounded queue. At any point, at most
				// nThreads threads will be active processing tasks. If
				// additional tasks are submitted when all threads are active,
				// they will wait in the queue until a thread is available.
				// https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/Executors.html#newFixedThreadPool(int)
				executorService.execute(messageSender);
			}
		}
		
		System.out.println("2");
		// Wait to send all message then shutdown the thread pool.
		executorService.shutdown();
	}

	/**
	 * Adds a message to the queue.
	 * 
	 * @param message
	 *            The message to be added.
	 */
	public synchronized void addMessageToQueue(Message message) {
		// If the server force the MessageCenter to stop all messages in the
		// queue will be sent. Adding new messages in the queue is not allowed.
		if (keepRunning) {
			messagesQueue.add(message);
		}

		notify();
	}

	void shutdown(boolean shutDownImmediately) {
		this.shutDownImmediately = shutDownImmediately;
		this.keepRunning = false;
		if (this.messagesQueue.isEmpty()) {
			interrupt();
		} 
	}

	/**
	 * Adds a system message in the queue to terminate the client sender. If the
	 * client listener is not terminated the system message terminates it.
	 * 
	 * @param name
	 *            The username of the client.
	 */
	void disconnectUser(String name) {
		// Generating systems message to close the client sender
		Message systemMessage = new Message("logout", name, "admin", SystemCode.LOGOUT);

		addMessageToQueue(systemMessage);
	}

	/**
	 * 
	 * @return First message in the queue.
	 * @throws InterruptedException 
	 */
	private synchronized Message getNextMessageFromQueue() throws InterruptedException {
		while (messagesQueue.size() == 0) {
			wait();
		}

		Message message = messagesQueue.poll();
		return message;
	}

	private void initializeExecutor() {
		int numberOfThread = 10;
		this.executorService = Executors.newFixedThreadPool(numberOfThread);
	}
}