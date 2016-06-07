package chat.clientside;
import java.io.BufferedReader;
import java.io.IOException;

public class ClientInputListener extends Thread {
	private BufferedReader listener;
	
	public ClientInputListener(BufferedReader input) {
		this.listener = input;
	}
	
	public void run() {
		while(!isInterrupted()) {
			try {
				String message = listener.readLine();
				if (message == null) {
					//Lost connection
					break;
				}
				
				display(message);
			} catch (IOException e) {
				System.out.println("Connection lost.");
				interrupt();
			}
		}
	}

	private void display(String message) {
		System.out.println(message);
	}
}
