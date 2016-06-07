package chat.serverside;

public class Message {
	private String text;
	private String recipient;
	private String sender;
	
	public Message(String text, String recipient, String sender) {
		this.text = text;
		this.recipient = recipient;
		this.sender = sender;
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
}
