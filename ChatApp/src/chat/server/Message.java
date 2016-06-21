package chat.server;

import java.net.Socket;

public class Message {

	private String text;
	private String recipient;
	private String sender;

	// Boolean variable used to differ system message, sent from server, from
	// regular messages.
	private boolean isSystemMessage;
	
	// Variable used to define different system messages.
	private int systemCode;
	private Socket socket;

	public Message(String text, String recipient, String sender) {
		this.text = text;
		this.recipient = recipient;
		this.sender = sender;
		this.isSystemMessage = false;
	}

	public Message(String text, String recipient, int systemCode) {
		this(text, recipient, "admin");
		this.systemCode = systemCode;
		this.isSystemMessage = true;

	}

	public Message(String text, Socket socket, int systemCode) {
		this.text = text;
		this.socket = socket;
		this.systemCode = systemCode;
		this.isSystemMessage = true;
	}

	public boolean getIsSystemMessage() {
		return this.isSystemMessage;
	}

	public String getMessageText() {
		return this.text;
	}

	public String getRecipient() {
		return this.recipient;
	}

	public String getSender() {
		return this.sender;
	}
	
	public Socket getSocket() {
		return this.socket;
	}

	public int getSystemCode() {
		return this.systemCode;
	}
}