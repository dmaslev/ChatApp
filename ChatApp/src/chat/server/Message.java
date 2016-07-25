package chat.server;

public class Message {

	private String text;
	private String recipient;
	private String sender;

	// Boolean variable used to differ system message, sent from server, from
	// regular messages.
	private boolean isSystemMessage;
	
	// Variable used to define different system messages.
	private String systemCode;

	public Message(String text, String recipient, String sender, String systemCode) {
		this(text, recipient, sender);
		this.systemCode = systemCode;
		this.isSystemMessage = true;
	}
	
	public Message(String text, String recipient, String sender) {
		this.text = text;
		this.recipient = recipient;
		this.sender = sender;
		this.isSystemMessage = false;
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

	public String getSystemCode() {
		return this.systemCode;
	}
}