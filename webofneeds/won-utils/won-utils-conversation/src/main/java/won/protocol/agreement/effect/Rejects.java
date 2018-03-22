package won.protocol.agreement.effect;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

public class Rejects extends MessageEffect {

	private URI rejectedMessageUri;
	
	public Rejects(URI messageUri, URI rejectedMessageUri) {
		super(messageUri);
		this.rejectedMessageUri = rejectedMessageUri;
	}
		
	public URI getRejectedMessageUri() {
		return rejectedMessageUri;
	}

	@Override
	public String toString() {
		return "Rejects [rejectedMessageUri=" + rejectedMessageUri + "]";
	}
	
	
}
