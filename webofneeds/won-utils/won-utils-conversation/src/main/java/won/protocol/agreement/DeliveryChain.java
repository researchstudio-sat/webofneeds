package won.protocol.agreement;

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
	private Set<DeliveryChain> containedDeliveryChains = new HashSet<>();
	private ConversationMessage head; 
	public DeliveryChain() {}
	
	public void addMessage(ConversationMessage msg) {
		if (msg.isHeadOfDeliveryChain()) {
			if (this.head != null && this.head != msg) {
				throw new IllegalArgumentException("Trying to add another head ("+ msg.getMessageURI() + ") to delivery chain " + head.getMessageURI());
			}
			this.head = msg;
		}
		this.messages.add(msg);
	}
	
	public ConversationMessage getHead() {
		return head;
	}
	
	public boolean isTerminated() {
		if (this.head == null) return false;
		switch (this.head.getMessageType()){
			//cases in which there is no remote message:
			case CREATE_NEED:
			case DEACTIVATE:
			case ACTIVATE:
			case HINT_FEEDBACK_MESSAGE:
			case HINT_NOTIFICATION:
			case NEED_CREATED_NOTIFICATION:
				return head.hasResponse();
			default:
				//all other message types have remote messages and responses 
				return head.hasResponse() &&
						head.hasCorrespondingRemoteMessage() &&
						head.getCorrespondingRemoteMessageRef().hasResponse() &&
						head.getCorrespondingRemoteMessageRef().getIsResponseToInverseRef().hasCorrespondingRemoteMessage();
		}
	}
	
	public ConversationMessage getEnd() {
		if (this.head == null) return null;
		switch (this.head.getMessageType()){
			//cases in which there is no remote message:
			case CREATE_NEED:
			case DEACTIVATE:
			case ACTIVATE:
			case HINT_FEEDBACK_MESSAGE:
			case HINT_NOTIFICATION:
			case NEED_CREATED_NOTIFICATION:
				return head.getIsResponseToInverseRef();
			default:
				//all other message types have remote messages and responses 
				if (head.hasResponse() &&
						head.hasCorrespondingRemoteMessage() &&
						head.getCorrespondingRemoteMessageRef().hasResponse()) {
					return head.getCorrespondingRemoteMessageRef().getIsResponseToInverseRef().getCorrespondingRemoteMessageRef();
				} else {
					return null;
				}
		}
	}
	
	public URI getHeadURI() {
		return head.getMessageURI();
	}
	
	public Set<ConversationMessage> getMessages(){
		return this.messages;
	}
	
	public boolean isAfter(DeliveryChain other) {
		return other.getMessages().stream()
				.anyMatch(
						msg -> 
						//is the head before msg?
						this.getHead().isMessageOnPathToRoot(msg)
						//is the remote message of head before msg?
						|| (this.getHead().hasCorrespondingRemoteMessage() 
								&& this.getHead().getCorrespondingRemoteMessageRef().isMessageOnPathToRoot(msg)));
	}
	
	/**
	 * Indicates that this delivery chain begins before the other and ends after it.
	 * @param other
	 * @return
	 */
	private boolean _contains(DeliveryChain other) {
		if (!isTerminated()) {
			return false;
		}
		if (!getHead().sharesReachableRootsWith(other.getHead())) {
			return false;
		}
		return other.getMessages().stream()
				.allMatch(
						msg -> {
						//is the head before and the end after msg?
						boolean isHeadBeforeAllOtherMsgs = 
								msg.isMessageOnPathToRoot(this.getHead()) || 
									(getHead().hasCorrespondingRemoteMessage() 
											&& msg.isMessageOnPathToRoot(getHead().getCorrespondingRemoteMessageRef()));
						
						boolean isEndAfterAllOtherMsgs = getEnd().isMessageOnPathToRoot(msg)
								 || (getEnd().hasCorrespondingRemoteMessage() 
										&& getEnd().getCorrespondingRemoteMessageRef().isMessageOnPathToRoot(msg));
						
						return isHeadBeforeAllOtherMsgs && isEndAfterAllOtherMsgs;
				});
	}

	/**
	 * Another delivery chain is interleaved with this one both head
	 * messages are before either chain.
	 * @param other
	 * @return
	 */
	private boolean _isInterleavedWith(DeliveryChain other) {
		if (this == other) return false;
		if (!getHead().sharesReachableRootsWith(other.getHead())) {
			return false;
		}
		return ! (this.isAfter(other) || other.isAfter(this)); 
	}

	public boolean contains(DeliveryChain other) {
		return this.containedDeliveryChains.contains(other);
	}
	
	public boolean containsOtherChains() {
		return !this.containedDeliveryChains.isEmpty();
	}
	
	public void determineRelationshipWith(DeliveryChain other) {
		if (this == other) return;
		if (this.interleavedDeliveryChains.contains(other)) {
			//checked that earlier
			return;
		}
		if (this.containedDeliveryChains.contains(other)) {
			//checked that earlier
			return;
		}
		if (_contains(other)) {
			this.containedDeliveryChains.add(other);
		} else if (_isInterleavedWith(other)) {
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

	@Override
	public String toString() {
		return "DeliveryChain ["
				+ "head=" + head.getMessageURI()
				+ ", isTerminated():" + isTerminated()
				+ ", end:" + ((getEnd() != null) ? getEnd().getMessageURI() : "null")
				+ ", interleaved with: " + (this.interleavedDeliveryChains.size() == 0 ? "none" : this.interleavedDeliveryChains.stream().map(x -> x.getHead()))
				+ ", contains : " + (this.containedDeliveryChains.size() == 0 ? "none" : this.containedDeliveryChains.stream().map(x -> x.getHead()))
				+ "]";
	}
	
	
	
}