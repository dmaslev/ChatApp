package chat.serverside;

import java.io.IOException;
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

	public void run() {
		isServerInputManagerOn = true;
		while (isServerInputManagerOn) {
			String line = reader.nextLine();
			if (line.equalsIgnoreCase("disconnect")) {
				try {
					server.stopServer();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (line.equalsIgnoreCase("listall")) {
				server.listConnectedUsers();
			} else {
				System.out.println("Invalid command.");
			}
		}
	}
	
	public void disconnect() {
		isServerInputManagerOn = false;
	}
}