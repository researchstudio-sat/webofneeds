package won.protocol.agreement.effect;

import java.net.URI;

public abstract class MessageEffect {

	private URI messageUri;

	public MessageEffect(URI messageUri) {
		super();
		this.messageUri = messageUri;
	}

	public URI getMessageUri() {
		return messageUri;
	}


}
