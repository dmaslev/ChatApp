package chat.clientside;
import java.io.BufferedReader;
import java.io.IOException;

public class ClientMessageListener extends Thread {
	private BufferedReader listener;
	
	public ClientMessageListener(BufferedReader input) {
		this.listener = input;
	}

	public void run() {
		while(true) {
			try {
				String message = listener.readLine();
				display(message);
			} catch (IOException e) {
				System.out.println("Server disconnected");
				break;
			}
		}
	}

	private void display(String message) {
		System.out.println(message);
	}
}
