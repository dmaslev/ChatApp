package chat.serverside;

import java.util.Scanner;

public class ServerInputManager extends Thread {
	private Server server;
	private Scanner reader;
	private boolean isServerInputManagerOn;

	public ServerInputManager(Server server, Scanner reader) {
		this.setServer(server);
		this.setReader(reader);
	}

	private void setReader(Scanner reader) {
		this.reader = reader;
	}

	private void setServer(Server server) {
		this.server = server;
	}

	/**
	 * Listens for input commands and executes them.
	 */
	public void run() {
		isServerInputManagerOn = true;
		while (isServerInputManagerOn) {
			String line = reader.nextLine();
			if (line.equalsIgnoreCase("disconnect")) {
				server.stopServer();
			} else if (line.startsWith("remove: ")) {
				String name = line.substring(8).trim();
				server.disconnectUser(name);
			} else if (line.equalsIgnoreCase("listall")) {
				server.listConnectedUsers();
			} else {
				System.out.println("Invalid command.");
			}
		}
	}

	/**
	 * Stops waiting for user input.
	 */
	public void disconnect() {
		isServerInputManagerOn = false;
	}
}
