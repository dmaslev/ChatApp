package chat.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	// Default port number is used if user does not provide valid port number.
	private final int DEFAULT_PORT_NUMBER = 2222;
	
	private String serverAdress;
	private int port;
	
	private Scanner inputReader;

	/**
	 * Sets connection to the server. Opens data input and output streams.
	 * 
	 * @throws IOException If error occurs when reading from data input stream.
	 */
	private void initializeClient(String[] args) throws IOException {
		try {
			setSocketParameters(args);
			try {
				startClient();
			} catch (InterruptedException interruptedException) {
				// Main thread was interrupted before client message listener was
				// started.
				interruptedException.printStackTrace();
			}
		} catch (IllegalArgumentException illegalArgumentException) {
			illegalArgumentException.printStackTrace();
		} 
	}

	private void startClient() throws IOException, InterruptedException {
		try {
			Socket socket = new Socket(serverAdress, port);
			System.out.println("Successfully connected to: " + serverAdress + " on port: " + port);
			ClientMessageSender sender = new ClientMessageSender(socket, inputReader);
			sender.initializeUsername();
			
			ClientMessageListener listener = new ClientMessageListener(socket, sender);
			Thread listenerThread = new Thread(listener);
			listenerThread.start();
			listenerThread.join();
		} catch (UnknownHostException unknownHostException) {
			System.out.println(serverAdress + " can not be determinated.");
			unknownHostException.printStackTrace();
			throw new IOException(unknownHostException);
		} catch (IOException ioException) {
			System.out.println("Unable to connect to " + serverAdress + " on port " + port);
			ioException.printStackTrace();
			throw new IOException(ioException);
		}
	}

	private void setSocketParameters(String[] args) throws IllegalArgumentException {
		// Args can be empty, but not null.
		if (args.length > 0) {
			serverAdress = args[0];
			if (args.length > 1) {
				try {
					port = Integer.parseInt(args[1]);
					if (port < 1 || port > 65535) {
						// Invalid port number
						throw new IllegalArgumentException("Invalud port number");
					}
				} catch (NumberFormatException numberFormatException) {
					System.out.println(args[1] + " is not valid port number. ");
					throw new IllegalArgumentException(numberFormatException);
				}
			} else {
				port = DEFAULT_PORT_NUMBER;
			}
		} else {
			System.out.print("Enter host adress/ip adress of server/: ");
			inputReader = new Scanner(System.in);
			serverAdress = inputReader.nextLine();
			if (serverAdress.length() == 0) {
				throw new IllegalArgumentException("Server adress can not be null.");
			}

			port = DEFAULT_PORT_NUMBER;
		}
	}

	public static void main(String[] args) {
		Client client = new Client();
		try {
			client.initializeClient(args);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
}