package chat.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

	// Default port number is used if user does not provide valid port number.
	private final int DEFAULT_PORT_NUMBER = 2222;

	private String serverAddress;
	private int port;

	private Scanner inputReader;

	/**
	 * Sets connection to the server. Opens data input and output streams.
	 * @throws IOException 
	 * @throws Exception 
	 */
	private void initializeClient(String[] args) throws RuntimeException, IOException {
		try {
			setSocketParameters(args);
			try {
				startClient();
			} catch (InterruptedException interruptedException) {
				// Main thread was interrupted before client message listener
				// was
				// started.
				throw new RuntimeException(interruptedException);
			}
		} catch (IllegalArgumentException illegalArgumentException) {
			illegalArgumentException.printStackTrace();
		}
	}

	/**
	 * Starts new thread for sending message. Invokes Thread.join() method to
	 * make sure the main thread will wail the sender thread to start.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void startClient() throws IOException, InterruptedException {
		ClientMessageListener listener  = null;
		try {
			Socket socket = new Socket(serverAddress, port);
			System.out.println("Successfully connected to: " + serverAddress + " on port: " + port);
			
			ClientMessageSender sender = new ClientMessageSender(socket, inputReader);
			sender.init();
			listener = new ClientMessageListener(socket, sender);
			listener.init();
			
			Thread listenerThread = new Thread(listener);
			listenerThread.start();
			listenerThread.join();
		} catch (UnknownHostException unknownHostException) {
			System.out.println(serverAddress + " can not be determinated.");
			unknownHostException.printStackTrace();
			throw new IOException(unknownHostException);
		} catch (IOException ioException) {
			System.out.println("Unable to connect to " + serverAddress + " on port " + port);
			throw new IOException(ioException);
		} 
	}

	/**
	 * Sets parameters to server address and server port variables. If args[],
	 * passed to the main method contains valid arguments they will be set. If
	 * args[] is empty the user will be asked to enter the parameters.
	 * 
	 * @param args Arguments containing server address and server port.
	 * @throws IllegalArgumentException when provided arguments are invalid.
	 */
	private void setSocketParameters(String[] args) throws IllegalArgumentException {
		if (args == null) {
			return;
		}

		if (args.length > 0) {
			serverAddress = args[0];
			if (args.length > 1) {
				try {
					port = Integer.parseInt(args[1]);
					// Check if the provided number is valid port number.
					if (port < 1 || port > 65535) {
						throw new IllegalArgumentException("Invalid port number");
					}
				} catch (NumberFormatException numberFormatException) {
					throw new IllegalArgumentException(args[1] + " is not valid number. ", numberFormatException);
				}
			} else {
				// Port number was not provided. Default one will be used.
				port = this.DEFAULT_PORT_NUMBER;
			}
		} else {
			// Server address and port number were not provided. The user will
			// be asked to enter server address. 
			System.out.print("Enter host address/ip adress of server/: ");
			inputReader = new Scanner(System.in);
			serverAddress = inputReader.nextLine();
			if (serverAddress.length() == 0) {
				throw new IllegalArgumentException("Server adress can not be empty string.");
			}
			
			//Default port value will be used for port number.
			port = this.DEFAULT_PORT_NUMBER;
		}
	}

	public static void main(String[] args) throws Exception {
		Client client = new Client();
		try {
			client.initializeClient(args);
		} catch (IOException ioException) {
			throw new IOException(ioException);
		} catch (RuntimeException runtimeException) {
			throw new Exception(runtimeException);
		}
	}
}