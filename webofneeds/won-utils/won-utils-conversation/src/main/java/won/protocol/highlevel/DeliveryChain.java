package won.protocol.highlevel;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

/**
 * Group of messages that represent the delivery of one message, including the responses.
 * @author fkleedorfer
 *
 */
public class DeliveryChain {
	private Set<ConversationMessage> messages = new HashSet<>();
	private ConversationMessage root; 
	public DeliveryChain() {}
	
	public void addMessage(ConversationMessage msg) {
		if (msg.isRootOfDeliveryChain()) {
			if (this.root != null && this.root != msg) {
				throw new IllegalArgumentException("Trying to add another root ("+ msg.getMessageURI() + ") to delivery chain " + root.getMessageURI());
			}
			this.root = msg;
		}
		this.messages.add(msg);
	}
	
	public ConversationMessage getRoot() {
		return root;
	}
	
	public URI getRootURI() {
		return root.getMessageURI();
	}
	
	public Set<ConversationMessage> getMessages(){
		return this.messages;
	}
	
	public boolean isAfter(DeliveryChain other) {
		return messages.stream()
				.allMatch(m -> 
					other.getMessages().stream()
						.allMatch(
								o ->  m.isAfter(o) || ! o.isAfter(m) 
						));
	}

	/**
	 * Another delivery chain is interleaved with this one both root
	 * messages are before either chain.
	 * @param other
	 * @return
	 */
	public boolean isInterleavedDeliveryChain(DeliveryChain other) {
		if (this == other) return false;
		return this.isBefore(other.getRoot()) && other.isBefore(this.root);
	}
	
	/**
	 * This chain is before the specified other message if there is a path
	 * from other to any of the messages in the chain.
	 * @param other
	 * @return
	 */
	public boolean isBefore(ConversationMessage other) {
		return this.messages.stream().anyMatch(m -> other.isAfter(m));
	}
	
	
	public Set<DeliveryChain> getInterleavedDeliveryChains() {
		Set ret = new HashSet<>();
		//start at the root - only messages after the root can be interleaved
		return ret;
	}
	
}