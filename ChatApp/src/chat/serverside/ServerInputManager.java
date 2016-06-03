package chat.serverside;

import java.io.IOException;
import java.util.Scanner;

public class ServerInputManager extends Thread {
	private Server server;
	private Scanner reader;

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
		while (true) {
			try {
				String line = reader.nextLine();
				if (line.equalsIgnoreCase("disconnect")) {
					server.stopServer();
				} else if (line.equalsIgnoreCase("listall")) {
					server.listConnectedUsers();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
