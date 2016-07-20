package chat.server;

import java.io.IOException;
import java.util.Scanner;

public class ServerCommandDispatcher extends Thread {

	private Server server;
	private boolean isServerInputManagerOn;

	public ServerCommandDispatcher(Server server) {
		this.setServer(server);
	}

	/**
	 * Listens for input commands and executes them.
	 */
	public void run() {
		isServerInputManagerOn = true;

		Scanner reader = new Scanner(System.in);
		try {
			while (isServerInputManagerOn) {
				String line = reader.nextLine();
				if (line.equalsIgnoreCase("/help")) {
					printHelpMenu();
				} else if (line.startsWith("/disconnect")) {
					String[] args = line.split("\\s+");
					if (args.length > 1 && args[1].equals("false")) {
						server.stopServer(false);
					}

					server.stopServer(true);
				} else if (line.startsWith("/remove: ")) {
					String name = line.substring(8).trim();
					server.disconnectUser(name);
				} else if (line.equalsIgnoreCase("/listall")) {
					server.printConnectedUsers();
				} else {
					System.out.println("Invalid command.");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			reader.close();
		}
	}

	/**
	 * Stops waiting for user input.
	 */
	void shutdown() {
		isServerInputManagerOn = false;
	}

	/**
	 * Prints information about all supported commands.
	 */
	private void printHelpMenu() {
		System.out.println(
				"- To stop the server enter a command \"/disconnect\". "
				+ "If you want to wait all messages currently in the queue to be sent you can "
				+ "add \"true\" in the command or \"false\" otherwise. "
				+ "If you don't select it all messages will be waited.");
		System.out.println("- To disconnect a user enter a command in format: \"/remove: [username]\".");
		System.out.println("- To see all connected users enter a command \"/listall\".");
	}

	private void setServer(Server server) {
		this.server = server;
	}
}
