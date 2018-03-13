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
	private Set<DeliveryChain> interleavedDeliveryChains = new HashSet<>();
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
		return other.getMessages().stream()
				.anyMatch(
						msg -> 
						//is the root before msg?
						this.getRoot().isMessageOnPathToRoot(msg)
						//is the remote message of root before msg?
						|| (this.getRoot().hasCorrespondingRemoteMessage() 
								&& this.getRoot().getCorrespondingRemoteMessageRef().isMessageOnPathToRoot(msg)));
	}

	/**
	 * Another delivery chain is interleaved with this one both root
	 * messages are before either chain.
	 * @param other
	 * @return
	 */
	private boolean _isInterleavedWith(DeliveryChain other) {
		if (this == other) return false;
		return ! this.isAfter(other) || other.isAfter(this); 
	}

	public void rememberIfInterleavedWith(DeliveryChain other) {
		if (this.interleavedDeliveryChains.contains(other)) {
			//checked that earlier
			return;
		}
		if (_isInterleavedWith(other)) {
			this.interleavedDeliveryChains.add(other);
			other.interleavedDeliveryChains.add(this);
		}
	}
	
	public boolean isInterleavedWith(DeliveryChain other) {
		return this.interleavedDeliveryChains.contains(other);
	}
	
	public Set<DeliveryChain> getInterleavedDeliveryChains() {
		return this.interleavedDeliveryChains;
	}
	
}