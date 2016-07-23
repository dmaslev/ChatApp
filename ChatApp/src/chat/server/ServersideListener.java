package chat.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import chat.constants.SystemCode;

public class ServersideListener extends Thread {

	private Socket clientSocket;
	private BufferedWriter output;
	private OutputStreamWriter outputInner;
	private BufferedReader input;
	private InputStreamReader inputInner;
	private boolean keepRunning;

	private MessageDispatcher messageCenter;
	private Server messageServer;
	private String username;

	public ServersideListener(Socket clientSocket, MessageDispatcher messageCenter, Server messageServer) {
		this.clientSocket = clientSocket;
		this.messageCenter = messageCenter;
		this.messageServer = messageServer;
	}

	/**
	 * Listens for messages from client and sends them to message dispatcher.
	 */
	@Override
	public void run() {
		this.keepRunning = true;

		try {
			try {
				inputInner = new InputStreamReader(clientSocket.getInputStream());
				input = new BufferedReader(inputInner);
			} catch (IOException e) {
				// Failed to open input stream.
				shutdown();
				keepRunning = false;
				e.printStackTrace();
			}

			while (keepRunning) {
				String messageType = input.readLine();
				String textReceived = input.readLine();
				if (messageType == null) {
					// Client socket was closed.
					shutdown();
					return;
				}

				if (messageType.equals(SystemCode.REGISTER)) {
					String address = this.clientSocket.getInetAddress().toString();

					boolean successfulLogin = messageServer.addUser(textReceived, output, address, this);
					if (!successfulLogin) {
						keepRunning = false;
						return;
					}

					setUsername(textReceived);
				} else if (messageType.equals(SystemCode.LOGOUT)) {
					keepRunning = false;
					messageCenter.disconnectUser(username);
				} else if (messageType.equals(SystemCode.REGULAR_MESSAGE)) {
					String recipient = input.readLine();
					Message message = new Message(textReceived, recipient, username);
					messageCenter.addMessageToQueue(message);
				}
			}
		} catch (IOException ioException) {
			// Connection lost
			username = clientSocket.getInetAddress().toString();
			messageServer.removeUser(username);
			keepRunning = false;
			ioException.printStackTrace();
		}
	}

	public Socket getSocket() {
		return this.clientSocket;
	}

	public void openOutputStream() throws IOException  {
		try {
			outputInner = new OutputStreamWriter(this.clientSocket.getOutputStream());
			this.output = new BufferedWriter(outputInner);
		} catch (IOException e) {
			shutdown();
			throw new IOException("Opening output stream failed.", e);
		}
	}

	public String getUsername() {
		return this.username;
	}

	void shutdown()  {
		this.keepRunning = false;

		try {
			input.close();
		} catch (IOException e) {
			// Closing the input stream failed. Close the inner stream.
			try {
				inputInner.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}

		try {
			output.close();
		} catch (IOException e) {
			// Closing the output stream failed. Close the inner stream.
			try {
				outputInner.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}

		try {
			clientSocket.close();
		} catch (IOException e) {
			// Closing the socket failed.
			e.printStackTrace();
		}
	}

	private void setUsername(String name) {
		this.username = name;
	}
}