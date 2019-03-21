package won.protocol.agreement;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.apache.thrift.Option;

import won.protocol.agreement.effect.MessageEffect;
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
	Set<URI> claims = new HashSet<>();
	Set<ConversationMessage> claimsRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> claimsInverseRefs = new HashSet<ConversationMessage>();
	Set<URI> rejects = new HashSet<>();
	Set<ConversationMessage> rejectsRefs = new HashSet<ConversationMessage>();
	Set<ConversationMessage> rejectsInverseRefs = new HashSet<ConversationMessage>();
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
	Option<ConversationMessage> conversationRoot = Option.none();
	
	URI correspondingRemoteMessageURI;
	ConversationMessage correspondingRemoteMessageRef;
	
	Set<URI> forwarded = new HashSet<>();
    Set<ConversationMessage> forwardedRefs = new HashSet<ConversationMessage>();
    Set<ConversationMessage> forwardedInverseRefs = new HashSet<ConversationMessage>();
	
	URI isResponseTo;
	Optional<ConversationMessage> isResponseToOption = Optional.empty();
	ConversationMessage isResponseToInverseRef;
	
	URI isRemoteResponseTo;
	ConversationMessage isRemoteResponseToRef;
	ConversationMessage isRemoteResponseToInverseRef;
	
	WonMessageType messageType ;
	WonMessageDirection direction;
	DeliveryChain deliveryChain;
	
	private OptionalInt minDistanceToOwnRoot = OptionalInt.empty();
	private OptionalInt maxDistanceToOwnRoot = OptionalInt.empty();
	private OptionalInt order = OptionalInt.empty();
	
	private Set<ConversationMessage> knownMessagesOnPathToRoot = new HashSet<ConversationMessage>();
	
	private Set<MessageEffect> effects = Collections.EMPTY_SET;
	
	public ConversationMessage(URI messageURI) {
		this.messageURI = messageURI;
	}
	
	/**
	 * Removes all proposes, claims, rejects, accepts, proposesToCancel, contentGraphs
	 */
	public void removeHighlevelProtocolProperties() {
		removeProposes();
		removeAccepts();
		removeProposesToCancel();
		removeRejects();
		removeRetracts();
		removeClaims();
	}
	
	private void removeProposes() {
		this.proposes = new HashSet<>();
		this.proposesRefs.forEach(other -> other.removeProposesInverseRef(this));
		this.proposesRefs = new HashSet<>();
	}
	private void removeClaims() {
		this.claims = new HashSet<>();
		this.claimsRefs.forEach(other -> other.removeClaimsInverseRef(this));
		this.claimsRefs = new HashSet<>();
	}
	private void removeProposesInverseRef(ConversationMessage other) {
		this.proposesInverseRefs.remove(other);
	}
	private void removeClaimsInverseRef(ConversationMessage other) {
		this.claimsInverseRefs.remove(other);
	}
	private void removeProposesToCancel() {
		this.proposesToCancel = new HashSet<>();
		this.proposesToCancelRefs.forEach(other -> other.removeProposesToCancelInverseRef(this));
		this.proposesToCancelRefs = new HashSet<>();
	}
	
	private void removeProposesToCancelInverseRef(ConversationMessage other) {
		this.proposesInverseRefs.remove(other);
	}
	
	private void removeAccepts() {
		this.accepts = new HashSet<>();
		this.acceptsRefs.forEach(other -> other.removeAcceptsInverseRef(this));
		this.acceptsRefs = new HashSet<>();
	}
	
	private void removeAcceptsInverseRef(ConversationMessage other) {
		this.acceptsInverseRefs.remove(other);
	}
	
	private void removeRejects() {
		this.rejects = new HashSet<>();
		this.rejectsRefs.forEach(other -> other.removeRejectsInverseRef(this));
		this.rejectsRefs = new HashSet<>();
	}
	
	private void removeRejectsInverseRef(ConversationMessage other) {
		this.rejectsInverseRefs.remove(other);
	}
	
	private void removeRetracts() {
		this.retracts = new HashSet<>();
		this.retractsRefs.forEach(other -> other.removeRetractsInverseRef(this));
		this.retractsRefs = new HashSet<>();
	}
	
	private void removeRetractsInverseRef(ConversationMessage other) {
		this.retractsInverseRefs.remove(other);
	}
	
	public boolean isForwardedMessage() {
	    return ! this.forwardedInverseRefs.isEmpty();
	}
	
	public boolean isForwardedOrRemoteMessageOfForwarded() {
        return isForwardedMessage() || hasCorrespondingRemoteMessage() && correspondingRemoteMessageRef.isForwardedMessage();
    }
	
	public ConversationMessage getRootOfDeliveryChain() {
		return getDeliveryChain().getHead();
	}
	
	public boolean isHeadOfDeliveryChain() {
		return
            isFromOwner() || //owner initiated message
            (isFromSystem() && !isResponse()) || //system initiated Message
            (!hasCorrespondingRemoteMessage() && !isResponse()) || //message not going to remote need
            (isFromSystem() && isResponse() && !getIsResponseToOption().isPresent()); //failure without original
	}
	
	public boolean isEndOfDeliveryChain() {
		return this == getDeliveryChain().getEnd();
	}
	
	public boolean isInSameDeliveryChain(ConversationMessage other) {
		return this.getDeliveryChain() == other.getDeliveryChain();
	}
	
	public DeliveryChain getDeliveryChain() {
		if (this.deliveryChain != null) return deliveryChain;
		if (this.isHeadOfDeliveryChain()) {
			this.deliveryChain = new DeliveryChain();
			this.deliveryChain.addMessage(this);
			return this.deliveryChain;
		}
		if (isResponse() && getIsResponseToOption().isPresent()) {
			this.deliveryChain = getIsResponseToOption().get().getDeliveryChain();
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
		if (isForwardedMessage()) {
			Optional<ConversationMessage> forwardingMsg = getForwardedInverseRefs().stream().findFirst();
			if (forwardingMsg.isPresent()) {
				this.deliveryChain = forwardingMsg.get().getDeliveryChain();
				if (this.deliveryChain != null) {
					this.deliveryChain.addMessage(this);
					return deliveryChain;
				}
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
		boolean hrsr = hcrm && correspondingRemoteMessageRef.hasSuccessResponse();
		boolean hrr = hrsr && correspondingRemoteMessageRef.getIsResponseToInverseRef().hasCorrespondingRemoteMessage();
		return hsr && hcrm && hrsr && hrr;
	}
	
	public boolean hasPreviousMessage() {
		return ! this.getPreviousRefs().isEmpty();
	}
	
	public boolean hasSubsequentMessage() {
		return !this.getPreviousInverseRefs().isEmpty();
	}
	
	public boolean isCorrespondingRemoteMessageOf(ConversationMessage other) {
		return getCorrespondingRemoteMessageRef() == other;
	}
	
	public boolean isResponseTo(ConversationMessage other) {
		return getIsRemoteResponseToRef() == other;
	}
	
	public boolean hasResponse(ConversationMessage other) {
		return other.getIsResponseToOption().orElse(null)  == this;
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
	 * Compares messages for sorting them temporally, using the URI as tie breaker so as to ensure a stable ordering.
	 * @param other
	 * @return
	 */
	public int compareTo(ConversationMessage other) {
		if (this == other) return 0;
		int o1dist = this.getOrder(); 
		int o2dist = other.getOrder();
		if (o1dist != o2dist) {
			return o1dist - o2dist;
		}
		if (this.isResponseTo(other)) return 1;
		if (this.isRemoteResponseTo(other)) return 1;
		if (this.isFromExternal() && this.isCorrespondingRemoteMessageOf(other)) return 1;		
		if (this.isInSameDeliveryChain(other)) {
			if (this.isHeadOfDeliveryChain() || other.isEndOfDeliveryChain()) {
				return -1;
			}
			if (this.isEndOfDeliveryChain() ||  other.isHeadOfDeliveryChain()) {
				return 1;
			}
		}

		//if we get to here, we should check if one of the delivery chains is earlier
		return this.getMessageURI().compareTo(other.getMessageURI());

	}
	
	
	public int getOrder() {
		if (this.order.isPresent()) {
			return this.order.getAsInt();
		}
		OptionalInt mindist = getPreviousRefs()
				.stream()
				.mapToInt(msg -> msg.getOrder() + 1).min();
		if (this.hasCorrespondingRemoteMessage() && this.isFromExternal()) {
			this.order =  OptionalInt.of(Math.max(mindist.orElse(0), getCorrespondingRemoteMessageRef().getOrder() +1));
		} else {
			this.order = OptionalInt.of(mindist.orElse(0));
		}
		return this.order.getAsInt();
	}
	
	public Option<ConversationMessage> getOwnConversationRoot() {
		if (this.conversationRoot.isDefined()) {
			return this.conversationRoot;
		}
		for (ConversationMessage prev: this.getPreviousRefs()) {
			Option<ConversationMessage> root = prev.getOwnConversationRoot();
			if (root.isDefined()) {
				this.conversationRoot = Option.some(root.get());
				return root;
			}
		}
		this.conversationRoot = Option.some(this);
		return this.conversationRoot;
	}
	
	public Set<ConversationMessage> getReachableConversationRoots(){
		Set<ConversationMessage> roots = new HashSet<>();
		Option<ConversationMessage> ownRoot = getOwnConversationRoot();
		if (ownRoot.isDefined()) {
			roots.add(ownRoot.get());
		}
		if (this.hasCorrespondingRemoteMessage()) {
			Option<ConversationMessage> remoteRoot = getCorrespondingRemoteMessageRef().getOwnConversationRoot();
			if (remoteRoot.isDefined()) {
				roots.add(remoteRoot.get());
			}
		}
		return roots;
	}
	
	public boolean sharesReachableRootsWith(ConversationMessage other) {
		Set<ConversationMessage> myRoots = getReachableConversationRoots();
		return other.getReachableConversationRoots()
				.stream()
				.anyMatch(root -> myRoots.contains(root));
	}
	
	public boolean isMessageOnPathToRoot(ConversationMessage other) {
		if (this == other) return false;
		boolean foundIt = isMessageOnPathToRoot(other, new HashSet<>());
		return foundIt;
	}
	
	private boolean isMessageOnPathToRoot(ConversationMessage other, Set<ConversationMessage> visited) {
		if (this == other) return true;
		if (this.getOrder() < other.getOrder()) {
			//if this is the case, it's impossible that the other message is on the path to root
			return false;
		}
		if (this.knownMessagesOnPathToRoot.contains(other)) {
			return true;
		}
		visited.add(this);
		if (!this.hasPreviousMessage()) {
			return false;
		}
		Boolean foundIt = getPreviousRefs().stream()
				.filter(msg -> !visited.contains(msg))
				.anyMatch(msg -> msg.isMessageOnPathToRoot(other, visited));
		if (foundIt) {
			this.knownMessagesOnPathToRoot.add(other);
			return true;
		}
		if (this.hasCorrespondingRemoteMessage() && !visited.contains(this.getCorrespondingRemoteMessageRef())) {
			return this.getCorrespondingRemoteMessageRef().isMessageOnPathToRoot(other, visited);
		}
		return false;
	}
	
	public boolean isAgreementProtocolMessage() {
		return this.isRetractsMessage() || this.isProposesMessage() || this.isProposesToCancelMessage() || this.isAcceptsMessage() || this.isRejectsMessage() || this.isClaimsMessage(); 
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
	public boolean isClaimsMessage() {
		return !this.claimsRefs.isEmpty();
	}
	
	public boolean isRejectsMessage() {
		return !this.rejectsRefs.isEmpty();
	}
	
	public boolean isProposesToCancelMessage() {
		return !this.proposesToCancelRefs.isEmpty();
	}
	
	public boolean proposes(ConversationMessage other) {
		return this.proposesRefs.contains(other);
	}
	public boolean claims(ConversationMessage other) {
		return this.claimsRefs.contains(other);
	}
	public boolean accepts(ConversationMessage other) {
		return this.acceptsRefs.contains(other);
	}
	
	public boolean proposesToCancel(ConversationMessage other) {
		return this.proposesToCancelRefs.contains(other);
	}
	
	public boolean retracts(ConversationMessage other) {
		return this.retractsRefs.contains(other);
	}
	
	public boolean rejects(ConversationMessage other) {
		return this.rejectsRefs.contains(other);
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
	public Set<URI> getClaims() {
		return claims;
	}
	public Set<URI> getRejects() {
		return rejects;
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
	public Set<ConversationMessage> getClaimsRefs(){
		return claimsRefs;
	}
	public void addClaims(URI claims) {
		this.claims.add(claims);
	}
	public void addClaimsRef(ConversationMessage ref) {
		this.claimsRefs.add(ref);
	}
	public Set<ConversationMessage> getRejectsRefs(){
		return rejectsRefs;
	}
	public void addRejects(URI rejects) {
		this.rejects.add(rejects);
	}
	public void addRejectsRef(ConversationMessage ref) {
		this.rejectsRefs.add(ref);
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
	
	public Set<URI> getForwarded() {
        return forwarded;
    }
    public Set<ConversationMessage> getForwardedRefs(){
        return forwardedRefs;
    }
    public void addForwarded(URI forwarded) {
        this.forwarded.add(forwarded);
    }
    public void addForwardedRef(ConversationMessage ref) {
        this.forwardedRefs.add(ref);
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
	/**
	 * Return an optional conversation message here - it's possible that we only have the response, not the
	 * original one.
	 * @return
	 */
	public Optional<ConversationMessage> getIsResponseToOption() {
		return isResponseToOption;
	}
	public void setIsResponseToRef(ConversationMessage ref) {
		this.isResponseToOption = Optional.of(ref);
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
	public Set<ConversationMessage> getClaimsInverseRefs() {
		return claimsInverseRefs;
	}
	public void addClaimsInverseRef(ConversationMessage ref) {
		this.claimsInverseRefs.add(ref);
	}
	public Set<ConversationMessage> getRejectsInverseRefs() {
		return rejectsInverseRefs;
	}
	public void addRejectsInverseRef(ConversationMessage ref) {
		this.rejectsInverseRefs.add(ref);
	}
	public Set<ConversationMessage> getPreviousInverseRefs() {
		return previousInverseRefs;
	}
	public void addPreviousInverseRef(ConversationMessage ref) {
		this.previousInverseRefs.add(ref);
	}
	public Set<ConversationMessage> getForwardedInverseRefs() {
        return forwardedInverseRefs;
    }
    public void addForwardedInverseRef(ConversationMessage ref) {
        this.forwardedInverseRefs.add(ref);
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

	
	public void setEffects(Set<MessageEffect> effects) {
		this.effects = effects;
	}
	
	public Set<MessageEffect> getEffects() {
		return effects;
	}
	
	
	@Override
	public String toString() {
		return "ConversationMessage [messageURI=" + messageURI
				+ ", order=" + getOrder()
				+ ", direction=" + direction
				+ ", messageType=" + messageType
				+ ", deliveryChainPosition:" + (this == getDeliveryChain().getHead() ? "head" : this == getDeliveryChain().getEnd() ? "end" : "middle") 
				+ ", deliveryChainHead:" + getDeliveryChain().getHeadURI()
				+ ", senderNeedURI=" + senderNeedURI				
				+ ", proposes=" + proposes + ", proposesRefs:" + proposesRefs.size()
				+ ", claims=" + claims + ", claimsRefs:" + claimsRefs.size()
				+ ", rejects=" + rejects + ", rejectsRefs:" + rejectsRefs.size()
				+ ", previous=" + previous + ", previousRefs:"
				+ previousRefs.size() + ", accepts=" + accepts + ", acceptsRefs:" + acceptsRefs.size() + ", retracts=" + retracts
				+ ", retractsRefs:" + retractsRefs.size() + ", proposesToCancel=" + proposesToCancel
				+ ", proposesToCancelRefs:" + proposesToCancelRefs.size() + ", correspondingRemoteMessageURI="
				+ correspondingRemoteMessageURI + ", correspondingRemoteMessageRef=" + messageUriOrNullString(correspondingRemoteMessageRef)
				+ ", isResponseTo= " +isResponseTo + ", isRemoteResponseTo=" + isRemoteResponseTo 
				+ ", isResponseToRef: " + messageUriOrNullString(isResponseToOption) 
				+ ", isRemoteResponseToRef:" + messageUriOrNullString(isRemoteResponseToRef)
				+ ", isResponseToInverse: " + messageUriOrNullString(isResponseToInverseRef)
				+ ", isRemoteResponseToInverse: " + messageUriOrNullString(isRemoteResponseToInverseRef)
				+ ", isForwarded: " + isForwardedMessage()
				+ "]";
	}

	private Object messageUriOrNullString(ConversationMessage message) {
		return message != null? message.getMessageURI():"null";
	}
	
	private Object messageUriOrNullString(Optional<ConversationMessage> messageOpt) {
        return (messageOpt.isPresent()) ? messageOpt.get().getMessageURI():"null";
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messageURI == null) ? 0 : messageURI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConversationMessage other = (ConversationMessage) obj;
		if (messageURI == null) {
			if (other.messageURI != null)
				return false;
		} else if (!messageURI.equals(other.messageURI))
			return false;
		return true;
	}
	
	
	
}
