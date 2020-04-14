package won.protocol.agreement;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.agreement.effect.MessageEffect;
import won.protocol.agreement.effect.MessageEffectsBuilder;
import won.protocol.agreement.effect.ProposalType;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONAGR;

public class AgreementProtocolState {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Dataset pendingProposals = DatasetFactory.createGeneral();
    private final Dataset agreements = DatasetFactory.createGeneral();
    private final Dataset claims = DatasetFactory.createGeneral();
    private final Dataset cancelledAgreements = DatasetFactory.createGeneral();
    private final Dataset rejected = DatasetFactory.createGeneral();
    private Dataset conversation = null;
    private final Set<URI> retractedUris = new HashSet<URI>(); // TODO: Check!
    private final Set<URI> claimedUris = new HashSet<URI>();
    private final Set<URI> acceptedCancellationProposalUris = new HashSet<URI>();
    private Map<URI, ConversationMessage> messagesByURI = new HashMap<>();
    private Set<DeliveryChain> deliveryChains = new HashSet<>();

    public static AgreementProtocolState of(URI connectionURI, LinkedDataSource linkedDataSource) {
        Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndAtomsDataset(connectionURI,
                        linkedDataSource);
        return AgreementProtocolState.of(fullConversationDataset);
    }

    public static AgreementProtocolState of(Dataset conversation) {
        AgreementProtocolState instance = new AgreementProtocolState();
        instance.recalculate(conversation);
        return instance;
    }

    private AgreementProtocolState() {
    }

    public AgreementProtocolUris getAgreementProtocolUris() {
        AgreementProtocolUris uris = new AgreementProtocolUris();
        uris.addAgreementUris(getAgreementUris());
        uris.addAgreedMessageUris(getAgreedMessageUris());
        uris.addAcceptedCancellationProposalUris(getAcceptedCancellationProposalUris());
        uris.addCancelledAgreementUris(getCancelledAreementUris());
        // walk over pending proposals and collect the relevant uris:
        messagesByURI.values().stream().filter(m -> isPendingProposal(m.getMessageURI())).forEach(m -> {
            // so this is a pending proposal.
            // determine what it would cancel
            Set<URI> cancelled = m.getEffects().stream().filter(e -> e.isProposes()).map(e -> e.asProposes())
                            .flatMap(e -> e.getProposesToCancel().stream()).filter(this::isAgreement)
                            .collect(Collectors.toSet());
            // TODO
            uris.addCancellationPendingAgreementUris(cancelled);
            // determine what it proposes
            Set<URI> proposed = m.getEffects().stream().filter(e -> e.isProposes()).map(e -> e.asProposes())
                            .flatMap(e -> e.getProposes().stream()).collect(Collectors.toSet());
            boolean isProposal = false;
            if (!cancelled.isEmpty()) {
                // remember this is a pending proposal that would cancel stuff
                // uris.addPendingCancellationProposalUri(m.getMessageURI());
                isProposal = true;
            }
            if (!proposed.isEmpty()) {
                // remember this is a pending proposal that proposes stuff
                // TODO
                uris.addPendingProposalUri(m.getMessageURI());
                isProposal = true;
            }
            if (isProposal) {
                // this proposal is not empty - add it to the pending proposals
                ProposalUris proposal = new ProposalUris(m.getMessageURI(), m.getSenderAtomURI());
                proposal.addProposes(proposed);
                proposal.addProposesToCancel(cancelled);
                // TODO
                uris.addPendingProposal(proposal);
            }
        });
        uris.addPendingCancellationProposalUris(getCancellationPendingAgreementUris());
        uris.addProposedMessageUris(getProposedUris());
        uris.addProposedToCancelUris(getProposedToCancelUris());
        uris.addClaimedMessageUris(getClaimedUris());
        uris.addRejectedMessageUris(getRejectedUris());
        uris.addRetractedMessageUris(getRetractedUris());
        return uris;
    }

    public Dataset getConversationDataset() {
        return this.conversation;
    }

    /**
     * Returns the set of effects (<code>MessageEffect</code>s) of the head of the
     * delivery chain that the specified message belongs to, or an empty set if no
     * message is found for that URI.
     * 
     * @param messageUri
     * @return
     */
    public Set<MessageEffect> getEffects(URI messageUri) {
        ConversationMessage message = messagesByURI.get(messageUri);
        if (message == null) {
            return Collections.emptySet();
        }
        return message.getDeliveryChain().getHead().getEffects();
    }

    public Dataset getAgreements() {
        return agreements;
    }

    public Model getAgreement(URI agreementURI) {
        return agreements.getNamedModel(agreementURI.toString());
    }

    public boolean isAgreement(URI agreementUri) {
        return agreements.containsNamedModel(agreementUri.toString());
    }

    public Dataset getClaims() {
        return claims;
    }

    public Model getClaim(URI claimURI) {
        return claims.getNamedModel(claimURI.toString());
    }

    public boolean isClaim(URI claimUri) {
        return claims.containsNamedModel(claimUri.toString());
    }

    public Dataset getPendingProposals() {
        return pendingProposals;
    }

    public Model getPendingProposal(URI proposalURI) {
        return pendingProposals.getNamedModel(proposalURI.toString());
    }

    public boolean isPendingProposal(URI proposalUri) {
        return pendingProposals.containsNamedModel(proposalUri.toString()) || isPendingCancellation(proposalUri);
    }

    @Deprecated
    public Set<URI> getPendingProposalUris() {
        Set<URI> uris = RdfUtils.getGraphUris(pendingProposals);
        uris.addAll(getPendingCancellationProposalUris());
        return uris;
    }

    public Set<URI> getClauseUrisProposedByPendingProposal(URI proposalUri) {
        if (!isPendingProposal(proposalUri)) {
            return Collections.emptySet();
        }
        ConversationMessage msg = messagesByURI.get(proposalUri);
        return msg.getEffects().stream().filter(e -> e.isProposes()).map(e -> e.asProposes())
                        .filter(e -> e.hasClauses()).flatMap(e -> e.getProposes().stream()).collect(Collectors.toSet());
    }

    public Set<URI> getAgreementUrisCancelledByPendingProposal(URI proposalUri) {
        if (!isPendingProposal(proposalUri)) {
            return Collections.emptySet();
        }
        ConversationMessage msg = messagesByURI.get(proposalUri);
        return msg.getEffects().stream().filter(e -> e.isProposes()).map(e -> e.asProposes())
                        .filter(e -> e.hasCancellations()).flatMap(e -> e.getProposesToCancel().stream())
                        .collect(Collectors.toSet());
    }

    @Deprecated
    public Set<URI> getPendingCancellationProposalUris() {
        Iterator<String> pendingProposalsIt = pendingProposals.listNames();
        if (pendingProposalsIt == null) {
            return Collections.emptySet();
        }
        Set<URI> proposedToCancelUris = new HashSet<URI>();
        while (pendingProposalsIt.hasNext()) {
            Set<MessageEffect> effects = getEffects(URI.create(pendingProposalsIt.next()));
            effects.stream().forEach(effect -> {
                if (effect.isProposes()) {
                    proposedToCancelUris.addAll(effect.asProposes().getProposesToCancel());
                }
            });
        }
        return proposedToCancelUris;
    }

    public Set<URI> getAgreedMessageUris() {
        Iterator<String> agreementIt = agreements.listNames();
        if (agreementIt == null) {
            return Collections.emptySet();
        }
        Set<URI> agreedMessageUris = new HashSet<URI>();
        while (agreementIt.hasNext()) {
            Set<MessageEffect> effects = getEffects(URI.create(agreementIt.next()));
            effects.stream().forEach(effect -> {
                if (effect.isClaims()) {
                    agreedMessageUris.add(effect.asClaims().getClaimedMessageUri());
                } else if (effect.isProposes()) {
                    agreedMessageUris.addAll(effect.asProposes().getProposes());
                    agreedMessageUris.addAll(effect.asProposes().getProposesToCancel());
                }
            });
        }
        return agreedMessageUris;
    }

    public Dataset getCancelledAgreements() {
        return cancelledAgreements;
    }

    public Model getCancelledAgreement(URI cancelledAgreementURI) {
        return cancelledAgreements.getNamedModel(cancelledAgreementURI.toString());
    }

    public boolean isCancelledAgreement(URI agreementUri) {
        return cancelledAgreements.containsNamedModel(agreementUri.toString());
    }

    public Dataset getRejectedProposals() {
        return rejected;
    }

    public Model getRejectedProposal(URI rejectedProposalURI) {
        return rejected.getNamedModel(rejectedProposalURI.toString());
    }

    public boolean isRejectedProposal(URI rejectedProposalUri) {
        return rejected.containsNamedModel(rejectedProposalUri.toString());
    }

    @Deprecated
    public Model getPendingCancellations() {
        return pendingProposals.getDefaultModel();
    }

    @Deprecated
    public boolean isPendingCancellation(URI proposalUri) {
        return pendingProposals.getDefaultModel().contains(new ResourceImpl(proposalUri.toString()),
                        WONAGR.proposesToCancel, (RDFNode) null);
    }

    public Set<URI> getAgreementUris() {
        return RdfUtils.getGraphUris(agreements);
    }

    public Set<URI> getCancelledAreementUris() {
        return RdfUtils.getGraphUris(cancelledAgreements);
    }

    public Set<URI> getRetractedUris() {
        return retractedUris;
    }

    public Set<URI> getProposedUris() {
        Iterator<String> pendingProposalsIt = pendingProposals.listNames();
        if (pendingProposalsIt == null) {
            return Collections.emptySet();
        }
        Set<URI> proposedUris = new HashSet<URI>();
        while (pendingProposalsIt.hasNext()) {
            Set<MessageEffect> effects = getEffects(URI.create(pendingProposalsIt.next()));
            effects.stream().forEach(effect -> {
                if (effect.isProposes()) {
                    proposedUris.addAll(effect.asProposes().getProposes());
                }
            });
        }
        return proposedUris;
    }

    public Set<URI> getProposedToCancelUris() {
        Iterator<String> pendingProposalsIt = pendingProposals.listNames();
        if (pendingProposalsIt == null) {
            return Collections.emptySet();
        }
        Set<URI> proposedToCancelUris = new HashSet<URI>();
        while (pendingProposalsIt.hasNext()) {
            Set<MessageEffect> effects = getEffects(URI.create(pendingProposalsIt.next()));
            effects.stream().forEach(effect -> {
                if (effect.isProposes()) {
                    proposedToCancelUris.addAll(effect.asProposes().getProposesToCancel());
                }
            });
        }
        return proposedToCancelUris;
    }

    public Set<URI> getClaimedUris() {
        return this.claimedUris;
    }

    public Set<URI> getAcceptedCancellationProposalUris() {
        return acceptedCancellationProposalUris;
    }

    @Deprecated
    public Set<URI> getCancellationPendingAgreementUris() {
        Iterator<String> pendingProposalUris = pendingProposals.listNames();
        if (pendingProposalUris == null) {
            return Collections.emptySet();
        }
        Set<URI> cancelationUris = new HashSet<URI>();
        while (pendingProposalUris.hasNext()) {
            Set<MessageEffect> effects = getEffects(URI.create(pendingProposalUris.next()));
            effects.stream().forEach(effect -> {
                if (effect.isProposes()) {
                    cancelationUris.addAll(effect.asProposes().getProposesToCancel());
                }
            });
        }
        return cancelationUris;
    }

    public Set<URI> getRejectedUris() {
        return RdfUtils.getGraphUris(rejected);
    }

    /**
     * Returns the n latest messages filtered by the specified predicate, sorted
     * descending by order (latest first).
     * 
     * @param filterPredicate
     * @param n use 0 for the latest message.
     * @return
     */
    private Stream<ConversationMessage> getMessagesAsOrderedStream(
                    java.util.function.Predicate<ConversationMessage> filterPredicate) {
        return deliveryChains.stream().map(m -> m.getHead()).filter(x -> filterPredicate.test(x))
                        .sorted((x1, x2) -> x2.getOrder() - x1.getOrder());
    }

    /**
     * Returns a list of all agreement URIs in the order they were made. The
     * oldestFirst parameter controls ascending or descending order.
     */
    public List<URI> getAgreementsInChronologicalOrder(boolean oldestFirst) {
        return deliveryChains.stream().map(m -> m.getHead()).filter(x -> isAgreement(x.getMessageURI()))
                        .sorted((x1, x2) -> (oldestFirst ? -1 : 1) * (x2.getOrder() - x1.getOrder()))
                        .map(x -> x.getMessageURI()).collect(Collectors.toList());
    }

    /**
     * Returns a list of all agreement URIs in the order they were made. The
     * oldestFirst parameter controls ascending or descending order.
     */
    public List<URI> getAgreementsAndClaimsInChronologicalOrder(boolean oldestFirst) {
        return deliveryChains.stream().map(m -> m.getHead())
                        .filter(x -> isAgreement(x.getMessageURI()) || isClaim(x.getMessageURI()))
                        .sorted((x1, x2) -> (oldestFirst ? -1 : 1) * (x2.getOrder() - x1.getOrder()))
                        .map(x -> x.getMessageURI()).collect(Collectors.toList());
    }

    /**
     * Returns the n latest message uris filtered by the specified predicate, sorted
     * descending by order (latest first).
     * 
     * @param filterPredicate
     * @param n use 0 for the latest message.
     * @return
     */
    public List<URI> getNLatestMessageUris(java.util.function.Predicate<ConversationMessage> filterPredicate, int n) {
        List<URI> uris = getMessagesAsOrderedStream(filterPredicate).map(m -> m.getMessageURI())
                        .collect(Collectors.toList());
        if (uris.size() > n) {
            return uris.subList(0, n);
        } else {
            return uris;
        }
    }

    /**
     * Returns the n-th latest message filtered by the specified predicate.
     * 
     * @param filterPredicate
     * @param n use 0 for the latest message.
     * @return
     */
    public URI getNthLatestMessage(java.util.function.Predicate<ConversationMessage> filterPredicate, int n) {
        List<URI> uris = getNLatestMessageUris(filterPredicate, n + 1);
        if (uris.size() > n) {
            return uris.get(n);
        } else {
            return null;
        }
    }

    private void logNthLatestMessage(int n, URI atomUri, String type, URI result) {
        logger.debug(n + "-th latest message " + (type == null ? "" : "of type " + type)
                        + (atomUri == null ? "" : " sent by " + atomUri) + ": "
                        + (result == null ? " none found" : atomUri));
    }

    public URI getLatestMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()), 0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getNthLatestMessageSentByAtom(URI atomUri, int n) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()), n);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(n, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestProposesMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()) && m.isProposesMessage()
                        && m.getEffects().stream().anyMatch(e -> e.isProposes()), 0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestProposesOrClaimsMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()) && m.isProposesMessage()
                        && m.getEffects().stream().anyMatch(e -> e.isProposes() || e.isClaims()), 0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestPendingProposesMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(
                        m -> atomUri.equals(m.getSenderAtomURI()) && m.isProposesMessage()
                                        && m.getEffects().stream().anyMatch(
                                                        e -> e.isProposes() && isPendingProposal(m.getMessageURI())),
                        0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestAcceptsMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()) && m.isAcceptsMessage()
                        && m.getEffects().stream().anyMatch(e -> e.isAccepts()), 0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestAgreement() {
        return getLatestAgreement(Optional.empty());
    }

    public URI getLatestAgreement(Optional<URI> senderAtomUri) {
        Optional<ConversationMessage> acceptMsgOpt = getMessagesAsOrderedStream(m -> m.isAcceptsMessage()
                        && (!senderAtomUri.isPresent() || senderAtomUri.get().equals(m.getSenderAtomURI()))
                        && m.getEffects().stream().filter(e -> e.isAccepts()).map(e -> e.asAccepts())
                                        .map(a -> a.getAcceptedMessageUri()).anyMatch(this::isAgreement)).findFirst();
        if (!acceptMsgOpt.isPresent()) {
            return null;
        }
        return acceptMsgOpt.get().getEffects().stream().map(e -> e.asAccepts().getAcceptedMessageUri())
                        .filter(this::isAgreement).findFirst().get();
    }

    public URI getLatestAcceptsMessage() {
        URI uri = getNthLatestMessage(m -> m.isAcceptsMessage() && m.getEffects().stream().anyMatch(e -> e.isAccepts()),
                        0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, null, null, uri);
        }
        return uri;
    }

    public URI getLatestProposesToCancelMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(
                        m -> atomUri.equals(m.getSenderAtomURI()) && m.isProposesToCancelMessage() && m.getEffects()
                                        .stream().anyMatch(e -> e.isProposes() && e.asProposes().hasCancellations()),
                        0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestPendingProposal() {
        return getLatestPendingProposal(Optional.empty(), Optional.empty());
    }

    public URI getLatestPendingProposalOrClaim() {
        return getLatestPendingProposalOrClaim(Optional.empty(), Optional.empty());
    }

    public URI getLatestPendingProposal(Optional<ProposalType> type) {
        return getLatestPendingProposal(type, Optional.empty());
    }

    public URI getLatestPendingProposalOrClaim(Optional<ProposalType> type) {
        return getLatestPendingProposalOrClaim(type, Optional.empty());
    }

    public URI getLatestPendingProposal(Optional<ProposalType> type, Optional<URI> senderAtomUri) {
        URI uri = getNthLatestMessage(m -> (m.isProposesMessage() || m.isProposesToCancelMessage())
                        && (!senderAtomUri.isPresent() || senderAtomUri.get().equals(m.getSenderAtomURI()))
                        && m.getEffects().stream().filter(e -> e.isProposes()
                                        && (!type.isPresent() || e.asProposes().getProposalType() == type.get()))
                                        .map(e -> e.getMessageUri())
                                        .anyMatch(msgUri -> isPendingProposal(msgUri) || isPendingCancellation(msgUri)),
                        0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, senderAtomUri.orElse(null), null, uri);
        }
        return uri;
    }

    public URI getLatestPendingProposalOrClaim(Optional<ProposalType> type, Optional<URI> senderAtomUri) {
        URI uri = getNthLatestMessage(m -> (m.isProposesMessage() || m.isProposesToCancelMessage()
                        || m.isClaimsMessage())
                        && (!senderAtomUri.isPresent() || senderAtomUri.get().equals(m.getSenderAtomURI()))
                        && m.getEffects().stream().filter(e -> e.isProposes()
                                        && (!type.isPresent() || e.asProposes().getProposalType() == type.get())
                                        || e.isClaims()).map(e -> e.getMessageUri())
                                        .anyMatch(msgUri -> isPendingProposal(msgUri) || isPendingCancellation(msgUri)
                                                        || isClaim(msgUri)),
                        0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, senderAtomUri.orElse(null), null, uri);
        }
        return uri;
    }

    public URI getLatestRejectsMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()) && m.isRejectsMessage()
                        && m.getEffects().stream().anyMatch(e -> e.isRejects()), 0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    public URI getLatestRetractsMessageSentByAtom(URI atomUri) {
        URI uri = getNthLatestMessage(m -> atomUri.equals(m.getSenderAtomURI()) && m.isRetractsMessage()
                        && m.getEffects().stream().anyMatch(e -> e.isRetracts()), 0);
        if (logger.isDebugEnabled()) {
            logNthLatestMessage(0, atomUri, null, uri);
        }
        return uri;
    }

    /**
     * Returns all text messages found in the head of the delivery chain of the
     * specified message, concatenated by ', '.
     * 
     * @param messageUri
     * @return
     */
    public Optional<String> getTextMessage(URI messageUri) {
        ConversationMessage msg = messagesByURI.get(messageUri);
        if (msg == null) {
            return Optional.empty();
        }
        ConversationMessage head = msg.getDeliveryChain().getHead();
        if (head == null) {
            return Optional.empty();
        }
        return head.getContentGraphs().stream().flatMap(contentGraphURI -> WonRdfUtils.MessageUtils
                        .getTextMessages(conversation.getNamedModel(contentGraphURI.toString()), head.getMessageURI())
                        .stream()).reduce((msg1, msg2) -> msg1 + ", " + msg2);
    }

    /**
     * Calculates all agreements present in the specified conversation dataset.
     */
    private void recalculate(Dataset conversationDataset) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting conversation analysis for high-level protocols");
        }
        pendingProposals.begin(ReadWrite.WRITE);
        agreements.begin(ReadWrite.WRITE);
        cancelledAgreements.begin(ReadWrite.WRITE);
        rejected.begin(ReadWrite.WRITE);
        claims.begin(ReadWrite.WRITE);
        conversationDataset.begin(ReadWrite.READ);
        this.messagesByURI = ConversationMessagesReader.readConversationMessages(conversationDataset);
        Set<ConversationMessage> roots = new HashSet<ConversationMessage>();
        Collection<ConversationMessage> messages = messagesByURI.values();
        Set<DeadReferenceConversationMessage> messagesWithDeadReferences = new HashSet<>();
        // filter out messages we don't care about
        messages = messages.stream().filter(m -> !m.getMessageType().isHintMessage()).collect(Collectors.toList());
        // iterate over messages and interconnect them
        messages.stream().forEach(message -> {
            message.getPrevious().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addPreviousRef(other);
                    other.addPreviousInverseRef(message);
                } else {
                    messagesWithDeadReferences
                                    .add(new DeadReferenceConversationMessage(message, "msg:previousMessage", uri));
                }
            });
            message.getForwarded().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addForwardedRef(other);
                    other.addForwardedInverseRef(message);
                } else {
                    messagesWithDeadReferences
                                    .add(new DeadReferenceConversationMessage(message, "msg:forwardedMessage", uri));
                }
            });
            message.getAccepts().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addAcceptsRef(other);
                    other.addAcceptsInverseRef(message);
                } else {
                    messagesWithDeadReferences.add(new DeadReferenceConversationMessage(message, "agr:accepts", uri));
                }
            });
            message.getProposes().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addProposesRef(other);
                    other.addProposesInverseRef(message);
                } else {
                    messagesWithDeadReferences.add(new DeadReferenceConversationMessage(message, "agr:proposes", uri));
                }
            });
            message.getClaims().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addClaimsRef(other);
                    other.addClaimsInverseRef(message);
                } else {
                    messagesWithDeadReferences.add(new DeadReferenceConversationMessage(message, "agr:claims", uri));
                }
            });
            message.getRejects().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addRejectsRef(other);
                    other.addRejectsInverseRef(message);
                } else {
                    messagesWithDeadReferences.add(new DeadReferenceConversationMessage(message, "agr:rejects", uri));
                }
            });
            message.getProposesToCancel().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addProposesToCancelRef(other);
                    other.addProposesToCancelInverseRef(message);
                } else {
                    messagesWithDeadReferences
                                    .add(new DeadReferenceConversationMessage(message, "agr:proposesToCancel", uri));
                }
            });
            message.getRetracts().stream().filter(uri -> !uri.equals(message.getMessageURI())).forEach(uri -> {
                ConversationMessage other = messagesByURI.get(uri);
                if (other != null) {
                    message.addRetractsRef(other);
                    other.addRetractsInverseRef(message);
                } else {
                    messagesWithDeadReferences.add(new DeadReferenceConversationMessage(message, "mod:retracts", uri));
                }
            });
            if (message.getRespondingTo() != null && !message.getRespondingTo().equals(message.getMessageURI())) {
                ConversationMessage other = messagesByURI.get(message.getRespondingTo());
                if (other != null) {
                    if (other.getSenderAtomURI().equals(message.getSenderAtomURI())) {
                        if (other.getRespondingToInverseRef() != null
                                        && !message.equals(other.getRespondingToInverseRef())) {
                            throw new InconsistentConversationDataException(
                                            "Message " + other.getMessageURI() + " has more than one response: "
                                                            + other.getRespondingToInverseRef().getMessageURI()
                                                            + " and " + message.getMessageURI());
                        }
                        message.setRespondingToRef(other);
                        other.setRespondingToInverseRef(message);
                    } else {
                        // change from respondingTo to remotelyRespondingTo
                        if (other.getRemotelyRespondingToInverseRef() != null
                                        && !message.equals(other.getRemotelyRespondingToInverseRef())) {
                            throw new InconsistentConversationDataException(
                                            "Message " + other.getMessageURI()
                                                            + " has more than one remote response: "
                                                            + other.getRemotelyRespondingToInverseRef().getMessageURI()
                                                            + " and "
                                                            + message.getMessageURI());
                        }
                        message.setRemotelyRespondingTo(message.getRespondingTo());
                        message.setRespondingTo(null); // clear original reference
                        message.setRemotelyRespondingToRef(other);
                        other.setRemotelyRespondingToInverseRef(message);
                    }
                } else {
                    messagesWithDeadReferences.add(new DeadReferenceConversationMessage(message, "msg:respondingTo",
                                    message.getRespondingTo()));
                }
            }
            if (message.getPrevious().isEmpty()) {
                roots.add(message);
            }
        });
        //
        // now revisit all messages with dead references. Throw an exception if the
        // message is not a forwarded message
        messagesWithDeadReferences.stream().forEach(deadRef -> {
            if (deadRef.message.getMessageType() == WonMessageType.FAILURE_RESPONSE) {
                // we are lenient here because we may be processing a failure response
                // a failure response may refer to an original message that the server did no
                // store
                // eg because it failed consistency checks
                return;
            }
            if (deadRef.message.isForwardedMessage()) {
                // we are lenient here because a forwarded message should not cause an
                // exception, even
                // if it points to a missing message
                return;
            }
            throw new IncompleteConversationDataException(deadRef.message.getMessageURI(), deadRef.deadReference,
                            deadRef.predicate);
        });
        if (logger.isDebugEnabled()) {
            messages.stream().forEach(m -> logger.debug(m.toString()));
        }
        // link messages to deliveryChains
        deliveryChains = messages.stream().map(m -> {
            if (logger.isDebugEnabled()) {
                logger.debug("deliveryChain for message {}: {}", m.getMessageURI(), m.getDeliveryChain());
            }
            return m.getDeliveryChain();
        }).collect(Collectors.toSet());
        // find interleaved delivery chains
        deliveryChains.stream().forEach(dc -> deliveryChains.stream().forEach(dc2 -> {
            dc.determineRelationshipWith(dc2);
        }));
        // apply acknowledgment protocol to whole conversation first:
        conversation = acknowledgedSelection(conversationDataset, messages);
        // on top of this, apply modification and agreement protocol on a per-message
        // basis, starting with the root(s)
        // expect proposals and agreements to be empty
        PriorityQueue<ConversationMessage> currentMessages = new PriorityQueue<ConversationMessage>();
        currentMessages.addAll(messages);
        // we need to use a priority queue for the messages, which is
        // sorted by temporal ordering. Each time we process a message, we
        // add the subsequent ones to the queue, the retrieve the
        // oldest from the queue for the next iteration.
        Set<ConversationMessage> processed = new HashSet<>();
        List<ConversationMessage> processedInOrder = null;
        if (logger.isDebugEnabled()) {
            processedInOrder = new ArrayList<>();
        }
        ConversationMessage last = null;
        while (!currentMessages.isEmpty()) {
            ConversationMessage msg = currentMessages.poll();
            if (processed.contains(msg)) {
                continue;
            }
            processed.add(msg);
            MessageEffectsBuilder effectsBuilder = new MessageEffectsBuilder(msg.getMessageURI());
            if (logger.isDebugEnabled() && processedInOrder != null) {
                processedInOrder.add(msg);
            }
            last = msg;
            if (!msg.isHeadOfDeliveryChain()) {
                continue;
            }
            if (!msg.isAgreementProtocolMessage()) {
                continue;
            }
            if (msg.isRetractsMessage()) {
                removeContentGraphs(conversation, msg);
                if (logger.isDebugEnabled()) {
                    msg.getRetractsRefs().forEach(other -> {
                        logger.debug("{} retracts {}", msg.getMessageURI(), other.getMessageURI());
                    });
                }
                msg.getRetractsRefs().stream().filter(other -> msg != other)
                                .filter(other -> other.getSenderAtomURI().equals(msg.getSenderAtomURI()))
                                .filter(other -> other.isHeadOfDeliveryChain())
                                .filter(other -> msg.isAfter(other)).forEach(other -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("{} retracts {}: valid, computing effects", msg.getMessageURI(),
                                                        other.getMessageURI());
                                    }
                                    boolean changedSomething = false;
                                    changedSomething = removeContentGraphs(conversation, other) || changedSomething;
                                    retractedUris.add(other.getMessageURI());
                                    if (other.isProposesMessage() || other.isProposesToCancelMessage()) {
                                        changedSomething = retractProposal(other.getMessageURI()) || changedSomething;
                                    }
                                    if (other.isClaimsMessage()) {
                                        changedSomething = retractClaim(other.getMessageURI()) || changedSomething;
                                    }
                                    if (changedSomething) {
                                        effectsBuilder.retracts(other.getMessageURI());
                                    }
                                });
                if (logger.isDebugEnabled()) {
                    logger.debug("agreement data: {}", agrDataToString());
                }
            }
            if (msg.isRejectsMessage()) {
                removeContentGraphs(conversation, msg);
                if (logger.isDebugEnabled()) {
                    msg.getRejectsRefs().forEach(other -> {
                        logger.debug("{} rejects {}", msg.getMessageURI(), other.getMessageURI());
                    });
                }
                msg.getRejectsRefs().stream().filter(other -> msg != other)
                                .filter(other -> other.isProposesMessage() || other.isProposesToCancelMessage()
                                                || other.isClaimsMessage())
                                .filter(other -> other.isHeadOfDeliveryChain())
                                .filter(other -> !other.getSenderAtomURI().equals(msg.getSenderAtomURI()))
                                .filter(other -> msg.isAfter(other)).filter(other -> {
                                    // check if msg also accepts other - in that case, the message is contradictory
                                    // in itself
                                    // Resolution: neither statement has any effect.
                                    return !msg.accepts(other);
                                }).forEach(other -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("{} rejects {}: valid, computing effects", msg.getMessageURI(),
                                                        other.getMessageURI());
                                    }
                                    boolean changedSomething = false;
                                    if (other.isProposesMessage() || other.isProposesToCancelMessage()) {
                                        changedSomething = rejectProposal(other.getMessageURI()) || changedSomething;
                                    }
                                    if (other.isClaimsMessage()) {
                                        changedSomething = rejectClaim(other.getMessageURI()) || changedSomething;
                                    }
                                    if (changedSomething) {
                                        effectsBuilder.rejects(other.getMessageURI());
                                    }
                                });
                if (logger.isDebugEnabled()) {
                    logger.debug("agreement data: {}", agrDataToString());
                }
            }
            if (msg.isProposesMessage()) {
                if (logger.isDebugEnabled()) {
                    msg.getProposesRefs().forEach(other -> {
                        logger.debug("{} proposes {}", msg.getMessageURI(), other.getMessageURI());
                    });
                }
                Model proposalContent = ModelFactory.createDefaultModel();
                msg.getProposesRefs().stream().filter(other -> msg != other)
                                .filter(other -> other.isHeadOfDeliveryChain())
                                .filter(other -> msg.isAfter(other)).forEach(other -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("{} proposes {}: valid, computing effects", msg.getMessageURI(),
                                                        other.getMessageURI());
                                    }
                                    boolean changedSomething = propose(conversationDataset, other.getContentGraphs(),
                                                    proposalContent);
                                    if (changedSomething) {
                                        effectsBuilder.proposes(other.getMessageURI());
                                    }
                                });
                pendingProposals.addNamedModel(msg.getMessageURI().toString(), proposalContent);
                if (logger.isDebugEnabled()) {
                    logger.debug("agreement data: {}", agrDataToString());
                }
            }
            if (msg.isProposesToCancelMessage()) {
                if (logger.isDebugEnabled()) {
                    msg.getProposesToCancelRefs().forEach(other -> {
                        logger.debug("{} proposesToCancel {}", msg.getMessageURI(),
                                        other.getMessageURI());
                    });
                }
                Model proposeToCancelContent = ModelFactory.createDefaultModel();
                msg.getProposesToCancelRefs().stream().filter(other -> msg != other)
                                .filter(other -> other.isHeadOfDeliveryChain())
                                .filter(toCancel -> msg.isAfter(toCancel)).forEach(other -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("{} proposesToCancel {}: valid, computing effects",
                                                        msg.getMessageURI(), other.getMessageURI());
                                    }
                                    boolean changedSomething = propose(conversationDataset, other.getContentGraphs(),
                                                    proposeToCancelContent);
                                    if (changedSomething) {
                                        effectsBuilder.proposesToCancel(other.getMessageURI());
                                    }
                                });
                pendingProposals.addNamedModel(msg.getMessageURI().toString(),
                                proposeToCancelContent);
                if (logger.isDebugEnabled()) {
                    logger.debug("agreement data: {}", agrDataToString());
                }
            }
            if (msg.isClaimsMessage()) {
                if (logger.isDebugEnabled()) {
                    msg.getClaimsRefs().forEach(other -> {
                        logger.debug("{} claims {}", msg.getMessageURI(), other.getMessageURI());
                    });
                }
                Model claimContent = ModelFactory.createDefaultModel();
                msg.getClaimsRefs().stream().filter(other -> msg != other)
                                .filter(other -> other.isHeadOfDeliveryChain())
                                .filter(other -> msg.isAfter(other)).forEach(other -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("{} claims {}: valid, computing effects", msg.getMessageURI(),
                                                        other.getMessageURI());
                                    }
                                    boolean changedSomething = claim(conversationDataset, other.getContentGraphs(),
                                                    claimContent);
                                    if (changedSomething) {
                                        effectsBuilder.claims(other.getMessageURI());
                                    }
                                });
                claims.addNamedModel(msg.getMessageURI().toString(), claimContent);
                if (logger.isDebugEnabled()) {
                    logger.debug("agreement data: {}", agrDataToString());
                }
            }
            if (msg.isAcceptsMessage()) {
                if (logger.isDebugEnabled()) {
                    msg.getAcceptsRefs().forEach(other -> {
                        logger.debug("{} accepts {}", msg.getMessageURI(), other.getMessageURI());
                    });
                }
                msg.getAcceptsRefs().stream().filter(other -> msg != other)
                                .filter(other -> other.isHeadOfDeliveryChain())
                                .filter(other -> !other.getSenderAtomURI().equals(msg.getSenderAtomURI()))
                                .filter(other -> msg.isAfter(other)).filter(other -> {
                                    // check if msg also accepts other - in that case, the message is contradictory
                                    // in itself
                                    // Resolution: neither statement has any effect.
                                    return !msg.rejects(other);
                                }).forEach(other -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("{} accepts {}: valid, computing effects", msg.getMessageURI(),
                                                        other.getMessageURI());
                                    }
                                    boolean changedSomething = false;
                                    if (other.isProposesMessage() || other.isProposesToCancelMessage()) {
                                        changedSomething = acceptProposal(other.getMessageURI()) || changedSomething;
                                    }
                                    if (other.isClaimsMessage()) {
                                        changedSomething = acceptClaim(other.getMessageURI()) || changedSomething;
                                    }
                                    if (changedSomething) {
                                        effectsBuilder.accepts(other.getMessageURI(), other.getProposesToCancel()
                                                        .stream().collect(Collectors.toSet()));
                                    }
                                });
                if (logger.isDebugEnabled()) {
                    logger.debug("agreement data: {}", agrDataToString());
                }
            }
            msg.setEffects(effectsBuilder.build());
            if (logger.isDebugEnabled() && !msg.getEffects().isEmpty()) {
                logger.debug("Effects of message {} : {}", msg.getMessageURI(), msg.getEffects());
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("messages in the order they were processed:");
            if (processedInOrder != null) {
                processedInOrder.stream().forEach(x -> logger.debug(x.toString()));
            }
            logger.debug("finished conversation analysis for high-level protocols");
        }
        pendingProposals.commit();
        agreements.commit();
        cancelledAgreements.commit();
        rejected.commit();
        claims.commit();
        conversationDataset.end();
        // recalculate claimedUris
        this.claimedUris.clear();
        Iterator<String> claimUris = claims.listNames();
        while (claimUris.hasNext()) {
            Set<MessageEffect> effects = getEffects(URI.create(claimUris.next()));
            effects.stream().forEach(effect -> {
                if (effect.isClaims()) {
                    this.claimedUris.add(effect.asClaims().getClaimedMessageUri());
                }
            });
        }
    }

    private String agrDataToString() {
        StringBuilder sb = new StringBuilder();
        sb
                        .append(singleDatasetToString("pending", pendingProposals)).append(", ")
                        .append(singleDatasetToString("agreements", agreements)).append(", ")
                        .append(singleDatasetToString("cancelledAgreements", cancelledAgreements)).append(", ")
                        .append(singleDatasetToString("rejected", rejected)).append(", ")
                        .append(singleDatasetToString("claims", claims));
        return sb.toString();
    }

    private String singleDatasetToString(String name, Dataset ds) {
        StringBuilder sb = new StringBuilder();
        Model m = ds.getDefaultModel();
        int count = 0;
        Iterator<String> it = ds.listNames();
        while (it.hasNext()) {
            count++;
            it.next();
        }
        sb.append(name).append("[");
        if (m != null && !m.isEmpty()) {
            sb.append("default graph: ")
                            .append(m.size()).append(" triples, ");
        }
        sb.append(count).append(" named graphs]");
        return sb.toString();
    }

    private Dataset acknowledgedSelection(Dataset conversationDataset, Collection<ConversationMessage> messages) {
        Dataset copy = RdfUtils.cloneDataset(conversationDataset);
        messages.stream().forEach(message -> {
            if (message.getMessageType() == null) {
                return;
            }
            if (message.getDirection() == WonMessageDirection.FROM_SYSTEM && !message.isResponse()) {
                if (!message.isAcknowledgedLocally()) {
                    notAcknowledged(copy, message);
                }
                return;
            }
            if (!message.isHeadOfDeliveryChain()) {
                // here, we are only concerned with removing content graphs of the 'main'
                // message in a delivery chain.
                // any other message does not concern us here.
                return;
            }
            switch (message.getMessageType()) {
                case SUCCESS_RESPONSE:
                case FAILURE_RESPONSE:
                    break;
                case CREATE_ATOM:
                case HINT_FEEDBACK_MESSAGE:
                case DEACTIVATE:
                case ACTIVATE:
                case ATOM_HINT_MESSAGE:
                case SOCKET_HINT_MESSAGE:
                    if (!message.isAcknowledgedLocally()) {
                        notAcknowledged(copy, message);
                    }
                    break;
                case CONNECT:
                case CONNECTION_MESSAGE:
                case CLOSE:
                    if (!message.isAcknowledgedRemotely()) {
                        notAcknowledged(copy, message);
                    }
                default:
                    break;
            }
            // delivery chain checking:
            // 1. if a chain contains another (first msg before, last msg after the other),
            // the containing chain is disregarded (content graph of root message removed
            // 2. if two delivery chains are interleaved, and none contains the other, both
            // are disregarded.
            DeliveryChain msgChain = message.getDeliveryChain();
            if (msgChain.containsOtherChains()) {
                // In this case, it is not possible to determine if the other
                // one has been delayed maliciously. Removing it is the only safe option.
                // Downside: It allows the recipient of the
                // message to delay it such that it is removed by this rule, but that at least
                // has immediate effects: the
                // message never seems acknowledged and then later, when some other chain
                // terminates, is found not to.
                // Rather, as soon as the chain of such a maliciously delayed message
                // terminates, it is dropped.
                if (logger.isDebugEnabled()) {
                    logger.debug("ignoring delivery chain {} as it contains other chains", msgChain.getHeadURI());
                }
                notAcknowledged(copy, message);
            } else {
                msgChain.getInterleavedDeliveryChains().stream().filter(otherChain -> otherChain.isTerminated())
                                .forEach(otherChain -> {
                                    // "interleaved" relationship is symmetric -> drop this message (chain), the
                                    // other message will be dropped when it is processed
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("ignoring delivery chain {} as it is interleaved with {}",
                                                        message.getMessageURI(), otherChain.getHead().getMessageURI());
                                    }
                                    notAcknowledged(copy, message);
                                });
            }
        });
        return copy;
    }

    private void notAcknowledged(Dataset copy, ConversationMessage message) {
        if (logger.isDebugEnabled()) {
            logger.debug("not acknowledged: " + message.getMessageURI());
        }
        message.removeHighlevelProtocolProperties();
        removeContentGraphs(copy, message);
    }

    /**
     * @param conversationDataset
     * @param message
     * @return true if the operation had any effect, false otherwise
     */
    private boolean removeContentGraphs(Dataset conversationDataset, ConversationMessage message) {
        AtomicBoolean changedSomething = new AtomicBoolean(false);
        message.getContentGraphs().stream().forEach(uri -> {
            String uriString = uri.toString();
            if (conversationDataset.containsNamedModel(uriString))
                conversationDataset.removeNamedModel(uriString);
            changedSomething.set(true);
        });
        return changedSomething.get();
    }

    /**
     * @param conversationDataset
     * @param graphURIs
     * @param proposal
     * @return true if the operation had any effect, false otherwise
     */
    private boolean propose(Dataset conversationDataset, Collection<URI> graphURIs, Model proposal) {
        long initialSize = proposal.size();
        graphURIs.forEach(uri -> {
            Model graph = conversationDataset.getNamedModel(uri.toString());
            if (graph != null) {
                proposal.add(RdfUtils.cloneModel(graph));
            }
        });
        // did we add anything?
        return proposal.size() - initialSize > 0;
    }

    /**
     *  
     */
    private boolean claim(Dataset conversationDataset, Collection<URI> graphURIs, Model claim) {
        long initialSize = claim.size();
        graphURIs.forEach(uri -> {
            Model graph = conversationDataset.getNamedModel(uri.toString());
            if (graph != null) {
                claim.add(RdfUtils.cloneModel(graph));
            }
        });
        // did we add anything?
        return claim.size() - initialSize > 0;
    }

    /**
     * @param proposalUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean acceptProposal(URI proposalUri) {
        boolean changedSomething = false;
        // check if the proposal has already been accepted:
        if (isAgreement(proposalUri)) {
            // accepting a proposal another time has no effect
            return changedSomething;
        }
        // first process proposeToCancel triples - this avoids that a message can
        // successfully propose to cancel itself, as agreements are only made after the
        // cancellations are processed.

        Set<MessageEffect> effects = getEffects(proposalUri);
        if (effects.size() > 0) {
            for (MessageEffect effect : effects) {
                if (effect.isProposes() && effect.asProposes().getProposesToCancel().size() > 0) {
                    this.acceptedCancellationProposalUris.addAll(effect.asProposes().getProposesToCancel());
                    changedSomething = true;
                }
                for (URI toCancel : effect.asProposes().getProposesToCancel()) {
                    changedSomething = cancelAgreement(toCancel)
                                    || changedSomething;
                }
            }
        }
        changedSomething = removeCancellationProposal(proposalUri) || changedSomething;
        // move proposal to agreements
        changedSomething = moveNamedGraph(proposalUri, pendingProposals, agreements) || changedSomething;
        return changedSomething;
    }

    /**
     * @param claimUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean acceptClaim(URI claimUri) {
        boolean changedSomething = false;
        if (isAgreement(claimUri)) {
            // accepting a claim one more time has no effect
            return changedSomething;
        }
        // move proposal to agreements
        changedSomething = moveNamedGraph(claimUri, claims, agreements) || changedSomething;
        return changedSomething;
    }

    /**
     * @param proposalUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean retractProposal(URI proposalUri) {
        boolean changedSomething = false;
        // we don't track retracted proposals (nobody cares about retracted proposals)
        // so just remove them
        if (pendingProposals.containsNamedModel(proposalUri.toString())) {
            changedSomething = true;
        }
        pendingProposals.removeNamedModel(proposalUri.toString());
        changedSomething = removeCancellationProposal(proposalUri) || changedSomething;
        return changedSomething;
    }

    /**
     * @param claimUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean retractClaim(URI claimUri) {
        boolean changedSomething = false;
        // we don't track retracted claims (nobody cares about retracted proposals)
        // so just remove them
        if (claims.containsNamedModel(claimUri.toString())) {
            changedSomething = true;
        }
        claims.removeNamedModel(claimUri.toString());
        return changedSomething;
    }

    /**
     * @param proposalUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean rejectProposal(URI proposalUri) {
        boolean changedSomething = moveNamedGraph(proposalUri, pendingProposals, rejected);
        changedSomething = removeCancellationProposal(proposalUri) || changedSomething;
        return changedSomething;
    }

    /**
     * @param claimUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean rejectClaim(URI claimUri) {
        boolean changedSomething = moveNamedGraph(claimUri, claims, rejected);
        return changedSomething;
    }

    /**
     * @param toCancel
     * @return true if the operation had any effect, false otherwise
     */
    private boolean cancelAgreement(URI toCancel) {
        return moveNamedGraph(toCancel, agreements, cancelledAgreements);
    }

    /**
     * @param proposalUri
     * @return true if the operation had any effect, false otherwise
     */
    private boolean removeCancellationProposal(URI proposalUri) {
        boolean changedSomething = false;
        Set<MessageEffect> effects = getEffects(proposalUri);
        if (effects.size() > 0) {
            for (MessageEffect effect : effects) {
                if (effect.isProposes() && effect.asProposes().getProposesToCancel().size() > 0) {
                    changedSomething = true;
                    pendingProposals.removeNamedModel(proposalUri.toString());
                }
            }
        }
        return changedSomething;
    }

    /**
     * @param graphUri
     * @param fromDataset
     * @param toDataset
     * @return true if the operation had any effect, false otherwise
     */
    private boolean moveNamedGraph(URI graphUri, Dataset fromDataset, Dataset toDataset) {
        boolean changedSomething = false;
        Model model = fromDataset.getNamedModel(graphUri.toString());
        fromDataset.removeNamedModel(graphUri.toString());
        if (model != null && model.size() > 0) {
            toDataset.addNamedModel(graphUri.toString(), model);
            changedSomething = true;
        }
        return changedSomething;
    }

    private class DeadReferenceConversationMessage {
        ConversationMessage message;
        String predicate;
        URI deadReference;

        public DeadReferenceConversationMessage(ConversationMessage message, String predicate, URI deadReference) {
            super();
            this.message = message;
            this.predicate = predicate;
            this.deadReference = deadReference;
        }
    }
}
