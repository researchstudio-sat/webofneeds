package won.protocol.highlevel;

import java.net.URI;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;

/**
 * 
 * @author fkleedorfer
 *
 */
public class ConversationMessage implements Comparable<ConversationMessage>{
	URI messageURI;
	URI senderNeedURI;
	Set<URI> proposes = new HashSet<>();
	Set<ConversationMessage> proposesRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> proposesInverseRefs = new HashSet<ConversationMessage>();
	Set<URI> previous = new HashSet<>();
	Set<ConversationMessage> previousRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> previousInverseRefs = new HashSet<ConversationMessage>();
	Set<URI> accepts = new HashSet<>();
	Set<ConversationMessage> acceptsRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> acceptsInverseRefs = new HashSet<ConversationMessage>();
	Set<URI> retracts = new HashSet<>();
	Set<ConversationMessage> retractsRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> retractsInverseRefs = new HashSet<ConversationMessage>();
	Set<URI> proposesToCancel = new HashSet<>();
	Set<ConversationMessage> proposesToCancelRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> proposesToCancelInverseRefs = new HashSet<ConversationMessage>();
	Set<URI> contentGraphs = new HashSet<>();
	
	URI correspondingRemoteMessageURI;
	ConversationMessage correspondingRemoteMessageRef;
	
	URI isResponseTo;
	ConversationMessage isResponseToRef;
	ConversationMessage isResponseToInverseRef;
	
	URI isRemoteResponseTo;
	ConversationMessage isRemoteResponseToRef;
	ConversationMessage isRemoteResponseToInverseRef;
	
	WonMessageType messageType ;
	WonMessageDirection direction;
	DeliveryChain deliveryChain;
	
	private Integer distanceToRoot;
	
	public ConversationMessage(URI messageURI) {
		this.messageURI = messageURI;
	}
	
	public ConversationMessage getRootOfDeliveryChain() {
		return getDeliveryChain().getRoot();
	}
	
	public boolean isRootOfDeliveryChain() {
		return isFromOwner() || (isFromSystem() && !isResponse()) || (!hasCorrespondingRemoteMessage() && ! isResponse());
	}
	
	public DeliveryChain getDeliveryChain() {
		if (this.deliveryChain != null) return deliveryChain;
		if (this.isRootOfDeliveryChain()) {
			this.deliveryChain = new DeliveryChain();
			this.deliveryChain.addMessage(this);
			return this.deliveryChain;
		}
		if (isResponse()) {
			this.deliveryChain = getIsResponseToRef().getDeliveryChain();
			if (this.deliveryChain != null) {
				this.deliveryChain.addMessage(this);
				return deliveryChain;
			}
		}
		if (hasCorrespondingRemoteMessage()) {
			this.deliveryChain = getCorrespondingRemoteMessageRef().getDeliveryChain();
			if (this.deliveryChain != null) {
				this.deliveryChain.addMessage(this);
				return deliveryChain;
			}
		}
		throw new IllegalStateException("did not manage to obtain the delivery chain for message " + this.getMessageURI());
	}
	
	public boolean isResponse() {
		return this.messageType == WonMessageType.SUCCESS_RESPONSE || this.messageType == WonMessageType.FAILURE_RESPONSE;
	}
	
	public boolean hasResponse() {
		return this.isResponseToInverseRef != null;
	}
	
	public boolean hasRemoteResponse() {
		return this.isRemoteResponseToInverseRef != null;
	}
	
	public boolean hasSuccessResponse() {
		return hasResponse() && this.isResponseToInverseRef.getMessageType() == WonMessageType.SUCCESS_RESPONSE;
	}
	
	public boolean hasRemoteSuccessResponse() {
		return hasRemoteResponse() && this.isRemoteResponseToInverseRef.getMessageType() == WonMessageType.SUCCESS_RESPONSE;
	}
	
	public boolean isAcknowledgedRemotely() {
		boolean hsr = hasSuccessResponse();
		boolean hcrm = hasCorrespondingRemoteMessage();
		boolean hrsr = correspondingRemoteMessageRef.hasSuccessResponse();
		//boolean hrr = correspondingRemoteMessageRef.getIsResponseToInverseRef().hasCorrespondingRemoteMessage();
		return hsr && hcrm && hrsr;
	}
	
	public boolean isCorrespondingRemoteMessageOf(ConversationMessage other) {
		return getCorrespondingRemoteMessageRef() == other;
	}
	
	public boolean isResponseTo(ConversationMessage other) {
		return getIsRemoteResponseToRef() == other;
	}
	
	public boolean hasResponse(ConversationMessage other) {
		return other.getIsResponseToRef() == this;
	}
	
	public boolean isRemoteResponseTo(ConversationMessage other) {
		return getIsRemoteResponseToRef() == other;
	}
	
	public boolean hasRemoteResponse(ConversationMessage other) {
		return other.getIsRemoteResponseToRef() == this;
	}
	
	public boolean partOfSameExchange(ConversationMessage other) {
		return this.getDeliveryChain() == other.getDeliveryChain();
	}
	
	/**
	 * Compares messages for sorting them temporally.
	 * @param other
	 * @return
	 */
	public int compareTo(ConversationMessage other) {
		if (this == other) return 0;
		int o1dist = this.distanceToRoot(); 
		int o2dist = other.distanceToRoot();
		if (o1dist != o2dist) {
			return o1dist - o2dist;
		}
		if (this.isResponseTo(other)) return -1;
		if (this.isRemoteResponseTo(other)) return -1;
		if (this.isFromExternal() && this.isCorrespondingRemoteMessageOf(other)) return -1;
		return 0;
	}
	
	public boolean isAfter(ConversationMessage other) {
		if (this == other) return false;
		int o1dist = this.distanceToRoot(); 
		int o2dist = other.distanceToRoot();
		if (o1dist != o2dist) {
			return o1dist > o2dist;
		}
		if (this.isResponseTo(other)) return true;
		if (this.isRemoteResponseTo(other)) return true;
		if (this.isFromExternal() && this.isCorrespondingRemoteMessageOf(other)) return true;
		return false;
	}
	
	public int distanceToRoot() {
		return distanceToRoot(new HashSet());
	}
	
	public int distanceToRoot(Set<ConversationMessage> visited) {
		if (distanceToRoot != null) return distanceToRoot;
		visited.add(this);
		Set<ConversationMessage> checkNow = new HashSet<>();
		checkNow.addAll(this.getPreviousRefs());
		if (this.hasCorrespondingRemoteMessage()) {
			checkNow.add(this.getCorrespondingRemoteMessageRef());
		}
		checkNow.removeAll(visited);
		OptionalInt dist = checkNow.stream().map(msg -> msg.distanceToRoot(visited)).mapToInt(i->i).min();
		this.distanceToRoot = dist.isPresent() ? dist.getAsInt() + 1 : 0;
		return this.distanceToRoot;
	}
	
	public boolean isFromOwner() {
		return this.direction == WonMessageDirection.FROM_OWNER;
	}
	
	public boolean isFromExternal() {
		return this.direction == WonMessageDirection.FROM_EXTERNAL;
	}
	
	public boolean isFromSystem() {
		return this.direction == WonMessageDirection.FROM_SYSTEM;
				
	}
	
	public boolean isAcknowledgedLocally() {
		return hasSuccessResponse();
	}
	
	public boolean isRetractsMessage() {
		return !this.retractsRefs.isEmpty();
	}
	
	public boolean isAcceptsMessage() {
		return !this.acceptsRefs.isEmpty();
	}
	
	public boolean isProposesMessage() {
		return !this.proposesRefs.isEmpty();
	}
	
	public boolean isProposesToCancelMessage() {
		return !this.proposesToCancelRefs.isEmpty();
	}
	
	public URI getMessageURI() {
		return messageURI;
	}
	public URI getSenderNeedURI() {
		return senderNeedURI;
	}
	public void setSenderNeedURI(URI senderNeedURI) {
		this.senderNeedURI = senderNeedURI;
	}
	public Set<URI> getProposes() {
		return proposes;
	}
	public Set<ConversationMessage> getProposesRefs(){
		return proposesRefs;
	}
	public void addProposes(URI proposes) {
		this.proposes.add(proposes);
	}
	public void addProposesRef(ConversationMessage ref) {
		this.proposesRefs.add(ref);
	}
	
	public Set<URI> getPrevious() {
		return previous;
	}
	public Set<ConversationMessage> getPreviousRefs(){
		return previousRefs;
	}
	public void addPrevious(URI previous) {
		this.previous.add(previous);
	}
	public void addPreviousRef(ConversationMessage ref) {
		this.previousRefs.add(ref);
	}
	
	public Set<URI> getAccepts() {
		return accepts;
	}
	public Set<ConversationMessage> getAcceptsRefs(){
		return this.acceptsRefs;
	}
	public void addAcceptsRef(ConversationMessage ref) {
		this.acceptsRefs.add(ref);
	}
	public void addAccepts(URI accepts) {
		this.accepts.add(accepts);
	}
	public Set<URI> getRetracts() {
		return retracts;
	}
	public Set<ConversationMessage> getRetractsRefs(){
		return this.retractsRefs;
	}
	public void addRetractsRef(ConversationMessage ref) {
		this.retractsRefs.add(ref);
	}
	public void addRetracts(URI retracts) {
		this.retracts.add(retracts);
	}
	public Set<URI> getProposesToCancel() {
		return proposesToCancel;
	}
	public Set<ConversationMessage> getProposesToCancelRefs(){
		return this.proposesToCancelRefs;
	}
	public void addProposesToCancelRef(ConversationMessage ref) {
		this.proposesToCancelRefs.add(ref);
	}
	public void addProposesToCancel(URI proposesToCancel) {
		this.proposesToCancel.add(proposesToCancel);
	}
	public URI getCorrespondingRemoteMessageURI() {
		return correspondingRemoteMessageURI;
	}
	public ConversationMessage getCorrespondingRemoteMessageRef() {
		return this.correspondingRemoteMessageRef;
	}
	public boolean hasCorrespondingRemoteMessage() {
		return this.correspondingRemoteMessageRef != null;
	}
	public void setCorrespondingRemoteMessageURI(URI correspondingRemoteMessageURI) {
		this.correspondingRemoteMessageURI = correspondingRemoteMessageURI;
	}
	public void setCorrespondingRemoteMessageRef(ConversationMessage ref) {
		this.correspondingRemoteMessageRef = ref;
	}
	
	
	public URI getIsResponseTo() {
		return isResponseTo;
	}
	public void setIsResponseTo(URI isResponseTo) {
		this.isResponseTo = isResponseTo;
	}
	public ConversationMessage getIsResponseToRef() {
		return isResponseToRef;
	}
	public void setIsResponseToRef(ConversationMessage ref) {
		this.isResponseToRef = ref;
	}
	public URI getIsRemoteResponseTo() {
		return isRemoteResponseTo;
	}
	public void setIsRemoteResponseTo(URI isRemoteResponseTo) {
		this.isRemoteResponseTo = isRemoteResponseTo;
	}
	public ConversationMessage getIsRemoteResponseToRef() {
		return isRemoteResponseToRef;
	}
	public void setIsRemoteResponseToRef(ConversationMessage ref) {
		this.isRemoteResponseToRef = ref;
	}
	
	
	public Set<ConversationMessage> getProposesInverseRefs() {
		return proposesInverseRefs;
	}
	public void addProposesInverseRef(ConversationMessage ref) {
		this.proposesInverseRefs.add(ref);
	}
	public Set<ConversationMessage> getPreviousInverseRefs() {
		return previousInverseRefs;
	}
	public void addPreviousInverseRef(ConversationMessage ref) {
		this.previousInverseRefs.add(ref);
	}
	public Set<ConversationMessage> getAcceptsInverseRefs() {
		return acceptsInverseRefs;
	}
	public void addAcceptsInverseRef(ConversationMessage ref) {
		this.acceptsInverseRefs.add(ref);
	}
	public Set<ConversationMessage> getRetractsInverseRefs() {
		return retractsInverseRefs;
	}
	public void addRetractsInverseRef(ConversationMessage ref) {
		this.retractsInverseRefs.add(ref);
	}
	public ConversationMessage getIsResponseToInverseRef() {
		return isResponseToInverseRef;
	}
	public void setIsResponseToInverseRef(ConversationMessage ref) {
		this.isResponseToInverseRef = ref;
	}
	public ConversationMessage getIsRemoteResponseToInverseRef() {
		return isRemoteResponseToInverseRef;
	}
	public void setIsRemoteResponseToInverseRef(ConversationMessage ref) {
		this.isRemoteResponseToInverseRef = ref;
	}
	
	public Set<ConversationMessage> getProposesToCancelInverseRefs() {
		return proposesToCancelInverseRefs;
	}
	public void addProposesToCancelInverseRef(ConversationMessage ref) {
		this.proposesToCancelInverseRefs.add(ref);
	}
	public Set<URI> getContentGraphs() {
		return contentGraphs;
	}
	public void addContentGraph(URI contentGraph) {
		this.contentGraphs.add(contentGraph);
	}

	public WonMessageType getMessageType() {
		return messageType;
	}
	public void setMessageType(WonMessageType messageType) {
		this.messageType = messageType;
	}
	
	
	public WonMessageDirection getDirection() {
		return direction;
	}

	public void setDirection(WonMessageDirection direction) {
		this.direction = direction;
	}

	@Override
	public String toString() {
		return "ConversationMessage [messageURI=" + messageURI 
				+ ", direction=" + direction
				+ ", messageType=" + messageType
				+ ", distanceToRoot=" + distanceToRoot()
				+ ", senderNeedURI=" + senderNeedURI
				
				+ ", proposes="
				+ proposes + ", proposesRefs:" + proposesRefs.size() + ", previous=" + previous + ", previousRefs:"
				+ previousRefs.size() + ", accepts=" + accepts + ", acceptsRefs:" + acceptsRefs.size() + ", retracts=" + retracts
				+ ", retractsRefs:" + retractsRefs.size() + ", proposesToCancel=" + proposesToCancel
				+ ", proposesToCancelRefs:" + proposesToCancelRefs.size() + ", correspondingRemoteMessageURI="
				+ correspondingRemoteMessageURI + ", correspondingRemoteMessageRef=" + messageUriOrNullString(correspondingRemoteMessageRef)
				+ ", isResponseTo= " +isResponseTo + ", isRemoteResponseTo=" + isRemoteResponseTo 
				+ ", isResponseToRef: " + messageUriOrNullString(isResponseToRef) 
				+ ", isRemoteResponseToRef:" + messageUriOrNullString(isRemoteResponseToRef)
				+ ", isResponseToInverse: " + messageUriOrNullString(isResponseToInverseRef)
				+ ", isRemoteResponseToInverse: " + messageUriOrNullString(isRemoteResponseToInverseRef)
				+ "]";
	}

	private Object messageUriOrNullString(ConversationMessage message) {
		return message != null? message.getMessageURI():"null";
	}
	
	
}