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

	public Message(String text, String recipient, String sender) {
		this.text = text;
		this.recipient = recipient;
		this.sender = sender;
		this.setSystemMessage();
	}

	public Message(int systemCode, String sender) {
		this.systemCode = systemCode;
		this.sender = sender;
		this.isSystemMessage = true;
	}

	public Message(int systemCode, Socket client) {
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

	public int getSystemCode() {
		return this.systemCode;
	}

	private void setSystemMessage() {
		if (sender.equalsIgnoreCase("admin") || sender.equalsIgnoreCase("administrator")) {
			this.isSystemMessage = true;
		} else {
			this.isSystemMessage = false;
		}
	}
}