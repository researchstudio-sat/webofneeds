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
 * @author fkleedorfer
 */
public class ConversationMessage implements Comparable<ConversationMessage> {
    URI messageURI;
    URI senderAtomURI;
    Set<URI> proposes = new HashSet<>();
    Set<ConversationMessage> proposesRefs = new HashSet<ConversationMessage>();
    Set<ConversationMessage> proposesInverseRefs = new HashSet<ConversationMessage>();
    Set<URI> claims = new HashSet<>();
    Set<ConversationMessage> claimsRefs = new HashSet<ConversationMessage>();
    Set<ConversationMessage> claimsInverseRefs = new HashSet<ConversationMessage>();
    Set<URI> rejects = new HashSet<>();
    Set<ConversationMessage> rejectsRefs = new HashSet<ConversationMessage>();
    Set<ConversationMessage> rejectsInverseRefs = new HashSet<ConversationMessage>();
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
    Set<URI> forwarded = new HashSet<>();
    Set<ConversationMessage> forwardedRefs = new HashSet<ConversationMessage>();
    Set<ConversationMessage> forwardedInverseRefs = new HashSet<ConversationMessage>();
    Set<URI> previous = new HashSet<>();
    Set<ConversationMessage> previousRefs = new HashSet<ConversationMessage>();
    Set<ConversationMessage> previousInverseRefs = new HashSet<ConversationMessage>();
    URI respondingTo;
    Optional<ConversationMessage> respondingToOption = Optional.empty();
    ConversationMessage respondingToInverseRef;
    URI remotelyRespondingTo;
    ConversationMessage remotelyRespondingToRef;
    ConversationMessage remotelyRespondingToInverseRef;
    WonMessageType messageType;
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
     * Removes all proposes, claims, rejects, accepts, proposesToCancel,
     * contentGraphs
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
        return !this.forwardedInverseRefs.isEmpty();
    }

    public ConversationMessage getRootOfDeliveryChain() {
        return getDeliveryChain().getHead();
    }

    public boolean isHeadOfDeliveryChain() {
        return isFromOwner() || // owner initiated message
                        (isFromSystem() && !isResponse()) || // system initiated Message
                        (isFromSystem() && isResponse() && (!getRespondingToOption().isPresent() // lone response
                                                                                                 // without msg
                                        && getRemotelyRespondingToRef() == null));
    }

    public boolean isEndOfDeliveryChain() {
        return this == getDeliveryChain().getEnd();
    }

    public boolean isInSameDeliveryChain(ConversationMessage other) {
        return this.getDeliveryChain() == other.getDeliveryChain();
    }

    public DeliveryChain getDeliveryChain() {
        if (this.deliveryChain != null)
            return deliveryChain;
        if (this.isHeadOfDeliveryChain()) {
            this.deliveryChain = new DeliveryChain();
            this.deliveryChain.addMessage(this);
            return this.deliveryChain;
        }
        if (isResponse()) {
            Optional<ConversationMessage> msg = Optional.empty();
            if (getRespondingToOption().isPresent()) {
                msg = getRespondingToOption();
            } else {
                msg = Optional.ofNullable(getRemotelyRespondingToRef());
            }
            if (msg.isPresent()) {
                this.deliveryChain = msg.get().getDeliveryChain();
                if (this.deliveryChain != null) {
                    this.deliveryChain.addMessage(this);
                    return deliveryChain;
                }
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
        throw new IllegalStateException(
                        "did not manage to obtain the delivery chain for message " + this.getMessageURI());
    }

    public boolean isResponse() {
        return this.messageType == WonMessageType.SUCCESS_RESPONSE
                        || this.messageType == WonMessageType.FAILURE_RESPONSE;
    }

    public boolean hasResponse() {
        return this.respondingToInverseRef != null;
    }

    public boolean hasRemoteResponse() {
        return this.remotelyRespondingToInverseRef != null;
    }

    public boolean hasSuccessResponse() {
        return hasResponse() && this.respondingToInverseRef.getMessageType() == WonMessageType.SUCCESS_RESPONSE;
    }

    public boolean hasRemoteSuccessResponse() {
        return hasRemoteResponse()
                        && this.remotelyRespondingToInverseRef.getMessageType() == WonMessageType.SUCCESS_RESPONSE;
    }

    public boolean isAcknowledgedRemotely() {
        return hasSuccessResponse() && hasRemoteSuccessResponse();
    }

    public boolean previousMessage() {
        return !this.getPreviousRefs().isEmpty();
    }

    public boolean hasSubsequentMessage() {
        return !this.getPreviousInverseRefs().isEmpty();
    }

    public boolean isResponseTo(ConversationMessage other) {
        return getRemotelyRespondingToRef() == other;
    }

    public boolean hasResponse(ConversationMessage other) {
        return other.getRespondingToOption().orElse(null) == this;
    }

    public boolean isRemoteResponseTo(ConversationMessage other) {
        return getRemotelyRespondingToRef() == other;
    }

    public boolean hasRemoteResponse(ConversationMessage other) {
        return other.getRemotelyRespondingToRef() == this;
    }

    public boolean partOfSameExchange(ConversationMessage other) {
        return this.getDeliveryChain() == other.getDeliveryChain();
    }

    /**
     * Compares messages for sorting them temporally, using the URI as tie breaker
     * so as to ensure a stable ordering.
     * 
     * @param other
     * @return
     */
    public int compareTo(ConversationMessage other) {
        if (this == other)
            return 0;
        int o1dist = this.getOrder();
        int o2dist = other.getOrder();
        if (o1dist != o2dist) {
            return o1dist - o2dist;
        }
        if (this.isResponseTo(other))
            return 1;
        if (this.isRemoteResponseTo(other))
            return 1;
        if (this.isInSameDeliveryChain(other)) {
            if (this.isHeadOfDeliveryChain() || other.isEndOfDeliveryChain()) {
                return -1;
            }
            if (this.isEndOfDeliveryChain() || other.isHeadOfDeliveryChain()) {
                return 1;
            }
        }
        // if we get to here, we should check if one of the delivery chains is earlier
        return this.getMessageURI().compareTo(other.getMessageURI());
    }

    public int getOrder() {
        if (this.order.isPresent()) {
            return this.order.getAsInt();
        }
        if (this.isResponse()) {
            if (this.getRespondingToOption().isPresent()) {
                return getRespondingToOption().get().getOrder() + 1;
            } else if (this.getRemotelyRespondingToRef() != null) {
                return getRemotelyRespondingToRef().getOrder() + 2;
            }
        }
        OptionalInt mindist = getPreviousRefs().stream().mapToInt(msg -> msg.getOrder() + 1).min();
        this.order = OptionalInt.of(mindist.orElse(0));
        return this.order.getAsInt();
    }

    public Option<ConversationMessage> getOwnConversationRoot() {
        if (this.conversationRoot.isDefined()) {
            return this.conversationRoot;
        }
        for (ConversationMessage prev : this.getPreviousRefs()) {
            Option<ConversationMessage> root = prev.getOwnConversationRoot();
            if (root.isDefined()) {
                this.conversationRoot = Option.some(root.get());
                return root;
            }
        }
        this.conversationRoot = Option.some(this);
        return this.conversationRoot;
    }

    public Set<ConversationMessage> getReachableConversationRoots() {
        Set<ConversationMessage> roots = new HashSet<>();
        Option<ConversationMessage> ownRoot = getOwnConversationRoot();
        if (ownRoot.isDefined()) {
            roots.add(ownRoot.get());
        }
        return roots;
    }

    public boolean sharesReachableRootsWith(ConversationMessage other) {
        Set<ConversationMessage> myRoots = getReachableConversationRoots();
        return other.getReachableConversationRoots().stream().anyMatch(root -> myRoots.contains(root));
    }

    public boolean isMessageOnPathToRoot(ConversationMessage other) {
        if (this == other)
            return false;
        boolean foundIt = isOtherMessageOnPathToRoot(other, new HashSet<>());
        return foundIt;
    }

    private boolean isOtherMessageOnPathToRoot(ConversationMessage other, Set<ConversationMessage> visited) {
        if (this == other)
            return true;
        if (this.getOrder() < other.getOrder()) {
            // if this is the case, it's impossible that the other message is on the path to
            // root
            return false;
        }
        if (this.knownMessagesOnPathToRoot.contains(other)) {
            return true;
        }
        visited.add(this);
        Boolean foundIt = getPreviousRefs().stream().filter(msg -> !visited.contains(msg))
                        .anyMatch(msg -> msg.isOtherMessageOnPathToRoot(other, visited));
        if (foundIt) {
            this.knownMessagesOnPathToRoot.add(other);
            return true;
        }
        if (this.respondingToOption.isPresent()) {
            if (!visited.contains(this.respondingToOption.get())) {
                foundIt = this.respondingToOption.get().isOtherMessageOnPathToRoot(other, visited);
            }
        }
        if (foundIt) {
            this.knownMessagesOnPathToRoot.add(other);
            return true;
        }
        if (this.remotelyRespondingToRef != null) {
            if (!visited.contains(this.remotelyRespondingToRef)) {
                foundIt = this.remotelyRespondingToRef.isOtherMessageOnPathToRoot(other, visited);
            }
        }
        if (foundIt) {
            this.knownMessagesOnPathToRoot.add(other);
            return true;
        }
        if (this.respondingToInverseRef != null) {
            if (!visited.contains(this.respondingToInverseRef)) {
                foundIt = this.respondingToInverseRef.isOtherMessageOnPathToRoot(other, visited);
            }
        }
        if (foundIt) {
            this.knownMessagesOnPathToRoot.add(other);
            return true;
        }
        if (this.remotelyRespondingToInverseRef != null) {
            if (!visited.contains(this.remotelyRespondingToInverseRef)) {
                foundIt = this.remotelyRespondingToInverseRef.isOtherMessageOnPathToRoot(other, visited);
            }
        }
        if (foundIt) {
            this.knownMessagesOnPathToRoot.add(other);
            return true;
        }
        return false;
    }

    public boolean isAgreementProtocolMessage() {
        return this.isRetractsMessage() || this.isProposesMessage() || this.isProposesToCancelMessage()
                        || this.isAcceptsMessage() || this.isRejectsMessage() || this.isClaimsMessage();
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

    public URI getSenderAtomURI() {
        return senderAtomURI;
    }

    public void setSenderAtomURI(URI senderAtomURI) {
        this.senderAtomURI = senderAtomURI;
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

    public Set<ConversationMessage> getProposesRefs() {
        return proposesRefs;
    }

    public void addProposes(URI proposes) {
        this.proposes.add(proposes);
    }

    public void addProposesRef(ConversationMessage ref) {
        this.proposesRefs.add(ref);
    }

    public Set<ConversationMessage> getClaimsRefs() {
        return claimsRefs;
    }

    public void addClaims(URI claims) {
        this.claims.add(claims);
    }

    public void addClaimsRef(ConversationMessage ref) {
        this.claimsRefs.add(ref);
    }

    public Set<ConversationMessage> getRejectsRefs() {
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

    public Set<ConversationMessage> getPreviousRefs() {
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

    public Set<ConversationMessage> getForwardedRefs() {
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

    public Set<ConversationMessage> getAcceptsRefs() {
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

    public Set<ConversationMessage> getRetractsRefs() {
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

    public Set<ConversationMessage> getProposesToCancelRefs() {
        return this.proposesToCancelRefs;
    }

    public void addProposesToCancelRef(ConversationMessage ref) {
        this.proposesToCancelRefs.add(ref);
    }

    public void addProposesToCancel(URI proposesToCancel) {
        this.proposesToCancel.add(proposesToCancel);
    }

    public URI getRespondingTo() {
        return respondingTo;
    }

    public void setRespondingTo(URI isResponseTo) {
        this.respondingTo = isResponseTo;
    }

    /**
     * Return an optional conversation message here - it's possible that we only
     * have the response, not the original one.
     * 
     * @return
     */
    public Optional<ConversationMessage> getRespondingToOption() {
        return respondingToOption;
    }

    public void setRespondingToRef(ConversationMessage ref) {
        this.respondingToOption = Optional.ofNullable(ref);
    }

    public URI getRemotelyRespondingTo() {
        return remotelyRespondingTo;
    }

    public void setRemotelyRespondingTo(URI isRemoteResponseTo) {
        this.remotelyRespondingTo = isRemoteResponseTo;
    }

    public ConversationMessage getRemotelyRespondingToRef() {
        return remotelyRespondingToRef;
    }

    public void setRemotelyRespondingToRef(ConversationMessage ref) {
        this.remotelyRespondingToRef = ref;
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

    public ConversationMessage getRespondingToInverseRef() {
        return respondingToInverseRef;
    }

    public void setRespondingToInverseRef(ConversationMessage ref) {
        this.respondingToInverseRef = ref;
    }

    public ConversationMessage getRemotelyRespondingToInverseRef() {
        return remotelyRespondingToInverseRef;
    }

    public void setRemotelyRespondingToInverseRef(ConversationMessage ref) {
        this.remotelyRespondingToInverseRef = ref;
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
        return "ConversationMessage [messageURI=" + messageURI + ", order=" + getOrder() + ", direction=" + direction
                        + ", messageType=" + messageType + ", deliveryChainPosition:"
                        + (this == getDeliveryChain().getHead() ? "head"
                                        : this == getDeliveryChain().getEnd() ? "end" : "middle")
                        + ", deliveryChainHead:" + getDeliveryChain().getHeadURI() + ", senderAtomURI=" + senderAtomURI
                        + ", proposes=" + proposes + ", proposesRefs:" + proposesRefs.size() + ", claims=" + claims
                        + ", claimsRefs:" + claimsRefs.size() + ", rejects=" + rejects + ", rejectsRefs:"
                        + rejectsRefs.size() + ", previous=" + previous + ", previousRefs:" + previousRefs.size()
                        + ", accepts=" + accepts + ", acceptsRefs:" + acceptsRefs.size() + ", retracts=" + retracts
                        + ", retractsRefs:" + retractsRefs.size() + ", proposesToCancel=" + proposesToCancel
                        + ", proposesToCancelRefs:" + proposesToCancelRefs.size() + ", isResponseTo= " + respondingTo
                        + ", isRemoteResponseTo=" + remotelyRespondingTo + ", isResponseToRef: "
                        + messageUriOrNullString(respondingToOption) + ", isRemoteResponseToRef:"
                        + messageUriOrNullString(remotelyRespondingToRef) + ", isResponseToInverse: "
                        + messageUriOrNullString(respondingToInverseRef) + ", isRemoteResponseToInverse: "
                        + messageUriOrNullString(remotelyRespondingToInverseRef) + ", isForwarded: "
                        + isForwardedMessage() + "]";
    }

    private Object messageUriOrNullString(ConversationMessage message) {
        return message != null ? message.getMessageURI() : "null";
    }

    private Object messageUriOrNullString(Optional<ConversationMessage> messageOpt) {
        return (messageOpt.isPresent()) ? messageOpt.get().getMessageURI() : "null";
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
