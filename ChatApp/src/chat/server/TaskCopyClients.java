package chat.server;

import java.util.Map;
import java.util.TimerTask;

public class TaskCopyClients extends TimerTask {


	private Map<String, ServersideListener> clients;
	private Map<String, ServersideListener> copyClients;

	public TaskCopyClients(Map<String, ServersideListener> clients, Map<String, ServersideListener> copyClients) {
		this.clients = clients;
		this.copyClients = copyClients;
	}

	@Override 
	public void run() {
		synchronized (clients) {
			copyClients.clear();
			copyClients.putAll(clients);
		}
	}
}
