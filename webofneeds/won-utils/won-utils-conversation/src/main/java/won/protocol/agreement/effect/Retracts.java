package won.protocol.agreement.effect;

import java.net.URI;

public class Retracts extends MessageEffect {

	private URI retractedMessageUri;
	
	public Retracts(URI messageUri, URI retractedMessageUri) {
		super(messageUri, MessageEffectType.RETRACTS);
		this.retractedMessageUri = retractedMessageUri;
	}
		
	public URI getRetractedMessageUri() {
		return retractedMessageUri;
	}

	@Override
	public String toString() {
		return "Retracts [retractedMessageUri=" + retractedMessageUri + "]";
	}
	
}
