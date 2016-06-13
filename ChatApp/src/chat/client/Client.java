package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	private final int DEFAULT_PORT_NUMBER = 2222;
	private Socket socket;
	private int port;
	private String serverAdress;
	private ClientMessageListener listener;
	private BufferedReader inputReader;

	public Client() {
	}

	public static void main(String[] args) {
		Client client = new Client();
		client.initializeClient(args);
	}

	/**
	 * Sets connection to the server. Opens input and ouput streams.
	 */
	private void initializeClient(String[] args) {
		inputReader = new BufferedReader(new InputStreamReader(System.in));
		openSocket(args);

		try {
			socket = new Socket(serverAdress, port);
			System.out.println("Successfully connected to: " + serverAdress);
			ClientMessageSender sender = new ClientMessageSender(socket);
			sender.initializeUsername();
			listener = new ClientMessageListener(socket, sender);
			Thread listenerThread = new Thread(listener);
			listenerThread.start();
		} catch (UnknownHostException unknownHostException) {
			System.out.println("Unable to connect to " + serverAdress);
			unknownHostException.printStackTrace();
		} catch (IOException ioException) {
			System.out.println("Unable to connect to " + serverAdress);
			ioException.printStackTrace();
		}
	}

	private void openSocket(String[] args) {
		if (args.length > 0) {
			serverAdress = args[0];
		} else {
			System.out.print("Enter host adress/ip adress of server/: ");
			try {
				serverAdress = inputReader.readLine();
			} catch (IOException ioException) {
				ioException.printStackTrace();
			}
		}

		port = DEFAULT_PORT_NUMBER;
		if (args.length > 1) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException numberFormatException) {
				System.out.println(args[1] + " is not valid port number. Default port number will be used.");
				port = DEFAULT_PORT_NUMBER;
			}
		}
	}
}