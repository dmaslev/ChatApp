package chat.server;

import java.util.Scanner;

public class ServerInputManager extends Thread {
	
	private Server server;
	private boolean isServerInputManagerOn;

	public ServerInputManager(Server server) {
		this.setServer(server);
	}

	/**
	 * Listens for input commands and executes them.
	 */
	public void run() {
		isServerInputManagerOn = true;
		
		Scanner reader = new Scanner(System.in);
		while (isServerInputManagerOn) {
			String line = reader.nextLine();
			if (line.equalsIgnoreCase("/help")) {
				printHelpMenu();
			} else if (line.equalsIgnoreCase("/disconnect")) {
				server.stopServer();
			} else if (line.startsWith("/remove: ")) {
				String name = line.substring(8).trim();
				server.disconnectUser(name);
			} else if (line.equalsIgnoreCase("/listall")) {
				server.listConnectedUsers();
			} else {
				System.out.println("Invalid command.");
			}
		}
		
		reader.close();
	}

	/**
	 * Stops waiting for user input.
	 */
	protected void disconnect() {
		isServerInputManagerOn = false;
	}
	
	/**
	 * Prints information about all supported commands.
	 */
	private void printHelpMenu() {
		System.out.println("- To stop the server enter a command \"/disconnect\"");
		System.out.println("- To disconnect a user enter a command in format: \"remove: [username]\"");
		System.out.println("- To see all connected users enter a command \"/listall\"");
	}

	private void setServer(Server server) {
		this.server = server;
	}
}
