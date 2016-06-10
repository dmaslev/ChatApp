package chat.serverside;

public class Message {
	private String text;
	private String recipient;
	private String sender;
	private boolean isSystemMessage;
	
	public Message(String text, String recipient, String sender) {
		this.text = text;
		this.recipient = recipient;
		this.sender = sender;
		this.setSystemMessage();
	}

	private void setSystemMessage() {
		if (sender.equalsIgnoreCase("admin") || sender.equalsIgnoreCase("administrator")) {
			this.isSystemMessage = true;
		} else {
			this.isSystemMessage = false;
		}
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
}