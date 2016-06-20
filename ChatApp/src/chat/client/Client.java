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
	 * @throws IOException
	 *             If error occurs when reading from data input.
	 */
	private void initializeClient(String[] args) throws IOException {
		setSocketParameters(args);

		try {
			startClient();
		} catch (InterruptedException interruptedException) {
			// Main thread was interrupted before client message listener was
			// started.
			interruptedException.printStackTrace();
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

	private void setSocketParameters(String[] args) {
		// Args can be empty, but not null.
		if (args.length > 0) {
			serverAdress = args[0];
			if (args.length > 1) {
				try {
					port = Integer.parseInt(args[1]);
				} catch (NumberFormatException numberFormatException) {
					System.out.println(args[1] + " is not valid port number. Default port number will be used.");
					port = DEFAULT_PORT_NUMBER;
					numberFormatException.printStackTrace();
				}
			}
		} else {
			System.out.print("Enter host adress/ip adress of server/: ");
			inputReader = new Scanner(System.in);
			serverAdress = inputReader.nextLine();
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