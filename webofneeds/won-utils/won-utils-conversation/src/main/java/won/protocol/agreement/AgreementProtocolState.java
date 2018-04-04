package won.protocol.agreement;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.agreement.effect.MessageEffect;
import won.protocol.agreement.effect.MessageEffectsBuilder;
import won.protocol.agreement.effect.ProposalType;
import won.protocol.message.WonMessageDirection;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONAGR;


public class AgreementProtocolState {
	private  final Logger logger = LoggerFactory.getLogger(AgreementProtocolState.class);
	
	private final Dataset pendingProposals = DatasetFactory.createGeneral();
	private final Dataset agreements = DatasetFactory.createGeneral();
	private final Dataset cancelledAgreements = DatasetFactory.createGeneral();
	private final Dataset rejected = DatasetFactory.createGeneral();
	private Dataset conversation = null;
	private final Set<URI> retractedUris = new HashSet<URI>();
	private final Set<URI> acceptedCancellationProposalUris = new HashSet<URI>();
	private Map<URI, ConversationMessage> messagesByURI = new HashMap<>();
	private Set<DeliveryChain> deliveryChains = new HashSet<>();
	
	public static AgreementProtocolState of(URI connectionURI, LinkedDataSource linkedDataSource) {
		Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionURI, linkedDataSource);
	    return AgreementProtocolState.of(fullConversationDataset);
	}
	
	public static AgreementProtocolState of(Dataset conversation) {
		AgreementProtocolState instance = new AgreementProtocolState();
		instance.recalculate(conversation);
		return instance;
	}
	
	private AgreementProtocolState() {}
	
	
	public AgreementProtocolUris getAgreementProtocolUris() {
		AgreementProtocolUris uris = new AgreementProtocolUris();
		uris.addAgreementUris(getAgreementUris());
		uris.addAcceptedCancellationProposalUris(getAcceptedCancellationProposalUris());
		uris.addCancellationPendingAgreementUris(getCancellationPendingAgreementUris());
		uris.addCancelledAgreementUris(getCancelledAreementUris());
		uris.addPendingCancellationProposalUris(getPendingCancellationProposalUris());
		uris.addPendingProposalUris(getPendingProposalUris());
		uris.addRejectedMessageUris(getRejectedUris());
		uris.addRetractedMessageUris(getRetractedUris());
		return uris;
	}
	
	
	public Dataset getConversationDataset() {
		return this.conversation;
	}
	/**
	 * Returns the set of effects (<code>MessageEffect</code>s) of the head of the delivery
	 * chain that the specified message belongs to, or an empty set if no message is
	 * found for that URI.
	 * 
	 * @param messageUri
	 * @return
	 */
	public Set<MessageEffect> getEffects(URI messageUri){
		ConversationMessage message = messagesByURI.get(messageUri);
		if (message == null) {
			return Collections.EMPTY_SET;
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
	
	public Dataset getPendingProposals() {
		return pendingProposals;
	}
	
	public Model getPendingProposal(URI proposalURI) {
		return pendingProposals.getNamedModel(proposalURI.toString());
	}
	
	public boolean isPendingProposal(URI proposalUri) {
		return pendingProposals.containsNamedModel(proposalUri.toString());
	}
	
	public Model getProposals(URI proposalURI) {
		return pendingProposals.getNamedModel(proposalURI.toString());
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
	
	public Model getPendingCancellations() {
		return pendingProposals.getDefaultModel();
	}
	
	public boolean isPendingCancellation(URI proposalUri) {
		return pendingProposals.getDefaultModel().contains(new ResourceImpl(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL, (RDFNode) null);
	}
	
	public Set<URI> getAgreementUris(){
		return RdfUtils.getGraphUris(agreements);		
	}
	
	public Set<URI> getPendingProposalUris(){
		return RdfUtils.getGraphUris(pendingProposals);		
	}	
	
	public Set<URI> getCancelledAreementUris(){
		return RdfUtils.getGraphUris(cancelledAgreements);		
	}
	
	public Set<URI> getRetractedUris() {
		return retractedUris;
	}
	
	public Set<URI> getAcceptedCancellationProposalUris() {
		return acceptedCancellationProposalUris;
	}
			
	public Set<URI> getCancellationPendingAgreementUris(){
		Model cancellations = pendingProposals.getDefaultModel();
		if (cancellations == null) {
			return Collections.EMPTY_SET;
		}
		Set ret = new HashSet<URI>();
		NodeIterator it = cancellations.listObjectsOfProperty(WONAGR.PROPOSES_TO_CANCEL);
		while(it.hasNext()) {
			String uri = it.next().asResource().getURI();
			ret.add(URI.create(uri));
		}
		return ret;
	}
	
	public Set<URI> getPendingCancellationProposalUris(){
		Model cancellations = pendingProposals.getDefaultModel();
		if (cancellations == null) {
			return Collections.EMPTY_SET;
		}
		Set ret = new HashSet<URI>();
		ResIterator it = cancellations.listSubjectsWithProperty(WONAGR.PROPOSES_TO_CANCEL);
		while(it.hasNext()) {
			String uri = it.next().asResource().getURI();
			ret.add(URI.create(uri));
		}
		return ret;
	}
	
	public Set<URI> getRejectedUris(){
		return RdfUtils.getGraphUris(rejected);		
	}
	
	
	/**
	 * Returns the n latest messages filtered by the specified predicate, sorted descending by order (latest first).
	 * @param filterPredicate
	 * @param n use 0 for the latest message.
	 * @return
	 */
	private Stream<ConversationMessage> getMessagesAsOrderedStream(java.util.function.Predicate<ConversationMessage> filterPredicate) {
		return deliveryChains.stream()
				.map(m -> m.getHead())
				.filter(x -> filterPredicate.test(x))
				.sorted((x1,x2) -> x2.getOrder() - x1.getOrder());
	}
	
	/**
	 * Returns the n latest message uris filtered by the specified predicate, sorted descending by order (latest first).
	 * @param filterPredicate
	 * @param n use 0 for the latest message.
	 * @return
	 */
	public List<URI> getNLatestMessageUris(java.util.function.Predicate<ConversationMessage> filterPredicate, int n) {
		List<URI> uris = getMessagesAsOrderedStream(filterPredicate)
				.map(m -> m.getMessageURI())
				.collect(Collectors.toList());
		if (uris.size() > n) {
			return uris.subList(0,n);
		} else {
			return uris;
		}
	}
	
	/**
	 * Returns the n-th latest message filtered by the specified predicate.
	 * @param filterPredicate
	 * @param n use 0 for the latest message.
	 * @return
	 */
	public URI getNthLatestMessage(java.util.function.Predicate<ConversationMessage> filterPredicate, int n) {
		List<URI> uris = getNLatestMessageUris(filterPredicate, n+1);
		if (uris.size() > n) {
			return uris.get(n);
		} else {
			return null;
		}
	}
	
	private void logNthLatestMessage(int n, URI needUri, String type, URI result) {
		logger.debug(
				n + "-th latest message "
				+ (type == null ? "" : "of type " + type ) 
				+ ( needUri == null ? "" : " sent by " + needUri ) 
				+ ": " 
				+ ( result == null ? " none found" : needUri ));
	}
	
	public URI getLatestMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	public URI getNthLatestMessageSentByNeed(URI needUri, int n) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()), n);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(n, needUri, null, uri);
		}
		return uri;
	}
	
	public URI getLatestProposesMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()) 
				&& m.isProposesMessage() && m.getEffects().stream().anyMatch(e->e.isProposes()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	public URI getLatestPendingProposesMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()) 
				&& m.isProposesMessage() && m.getEffects().stream().anyMatch(e->e.isProposes()
						&& isPendingProposal(m.getMessageURI())), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	public URI getLatestAcceptsMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()) 
				&& m.isAcceptsMessage() && m.getEffects().stream().anyMatch(e->e.isAccepts()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	public URI getLatestAgreement() {
		return getLatestAgreement(Optional.empty());
	}
	
	public URI getLatestAgreement(Optional<URI> senderNeedUri) {
		Optional<ConversationMessage> acceptMsgOpt = getMessagesAsOrderedStream(
				m -> m.isAcceptsMessage() && 
				(! senderNeedUri.isPresent() || senderNeedUri.get().equals(m.getSenderNeedURI())) &&
				m.getEffects()
					.stream()
					.filter(e->e.isAccepts())
					.map(e -> e.asAccepts())
					.map(a -> a.getAcceptedMessageUri())
					.anyMatch(this::isAgreement)).findFirst();
		if (!acceptMsgOpt.isPresent()) {
			return null;
		}
		return acceptMsgOpt.get().getEffects()
				.stream()
				.map(e -> e.asAccepts().getAcceptedMessageUri())
				.filter(this::isAgreement)
				.findFirst().get();
	}
	
	public URI getLatestAcceptsMessage() {
		URI uri = getNthLatestMessage(m ->  
				m.isAcceptsMessage() && m.getEffects().stream().anyMatch(e->e.isAccepts()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, null, null, uri);
		}
		return uri;
	}
	
	
	public URI getLatestProposesToCancelMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()) 
				&& m.isProposesToCancelMessage() && m.getEffects().stream().anyMatch(e->e.isProposes() && e.asProposes().hasCancellations()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	
	public URI getLatestPendingProposal() {
		return getLatestPendingProposal(Optional.empty(), Optional.empty());
	}
	
	public URI getLatestPendingProposal(Optional<ProposalType> type) {
		return getLatestPendingProposal(type, Optional.empty());
	}
	
	public URI getLatestPendingProposal(Optional<ProposalType> type, Optional<URI> senderNeedUri) {
		URI uri = getNthLatestMessage(
				m -> (m.isProposesMessage() || m.isProposesToCancelMessage()) &&
				(! senderNeedUri.isPresent() || senderNeedUri.get().equals(m.getSenderNeedURI())) && 
				m.getEffects()
					.stream()
					.filter(e->e.isProposes() && (!type.isPresent() || e.asProposes().getProposalType() == type.get()))
					.map(e -> e.getMessageUri())
					.anyMatch(msgUri -> isPendingProposal(msgUri) || isPendingCancellation(msgUri)), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, senderNeedUri.orElse(null), null, uri);
		}
		return uri;
	}
	
	public URI getLatestRejectsMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()) 
				&& m.isRejectsMessage() && m.getEffects().stream().anyMatch(e->e.isRejects()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	public URI getLatestRetractsMessageSentByNeed(URI needUri) {
		URI uri = getNthLatestMessage(m -> needUri.equals(m.getSenderNeedURI()) 
				&& m.isRetractsMessage() && m.getEffects().stream().anyMatch(e->e.isRetracts()), 0);
		if (logger.isDebugEnabled()) {
			logNthLatestMessage(0, needUri, null, uri);
		}
		return uri;
	}
	
	/**
	 * Returns all text messages found in the head of the delivery chain of the specified message, concatenated by ', '.
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
		return head.getContentGraphs()
			.stream()
			.flatMap(contentGraphURI -> WonRdfUtils.MessageUtils.getTextMessages(
							conversation.getNamedModel(contentGraphURI.toString()), 
							head.getMessageURI()).stream())
			.reduce((msg1, msg2) -> msg1 + ", " + msg2);
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
		conversationDataset.begin(ReadWrite.READ);
		
		
		this.messagesByURI = ConversationMessagesReader.readConversationMessages(conversationDataset);
		
		Set<ConversationMessage> roots = new HashSet();
		Collection<ConversationMessage> messages = messagesByURI.values();
		
		
		//iterate over messages and interconnect them
		messages.stream().forEach(message -> {
			if (message.getCorrespondingRemoteMessageURI() != null && ! message.getCorrespondingRemoteMessageURI().equals(message.getMessageURI())) {
				ConversationMessage other = messagesByURI.get(message.getCorrespondingRemoteMessageURI());
				throwExceptionIfOtherisMissing(message.getMessageURI(), message.getCorrespondingRemoteMessageURI(), other, "msg:hasCorrespondingRemoteMessage");
				message.setCorrespondingRemoteMessageRef(other);
                other.setCorrespondingRemoteMessageRef(message);
			}
			message.getPrevious().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				throwExceptionIfOtherisMissing(message.getMessageURI(), uri, other, "msg:hasPreviousMessage");
				message.addPreviousRef(other);
				other.addPreviousInverseRef(message);
			});
			message.getAccepts().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				throwExceptionIfOtherisMissing(message.getMessageURI(), uri, other, "agr:accepts");
				message.addAcceptsRef(other);
				other.addAcceptsInverseRef(message);
			});
			message.getProposes().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				throwExceptionIfOtherisMissing(message.getMessageURI(), uri, other, "agr:proposes");
				message.addProposesRef(other);
				other.addProposesInverseRef(message);
				});
			message.getRejects().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				throwExceptionIfOtherisMissing(message.getMessageURI(), uri, other, "agr:rejects");
				message.addRejectsRef(other);
				other.addRejectsInverseRef(message);
				});
			message.getProposesToCancel().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				throwExceptionIfOtherisMissing(message.getMessageURI(), uri, other, "agr:proposesToCancel");
				message.addProposesToCancelRef(other);
				other.addProposesToCancelInverseRef(message);
				});
			message.getRetracts().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				throwExceptionIfOtherisMissing(message.getMessageURI(), uri, other, "mod:retracts");
				message.addRetractsRef(other);
				other.addRetractsInverseRef(message);
				});
			if (message.getIsResponseTo() != null && ! message.getIsResponseTo().equals(message.getMessageURI())) {
				ConversationMessage other = messagesByURI.get(message.getIsResponseTo());
				throwExceptionIfOtherisMissing(message.getMessageURI(), message.getIsResponseTo(), other, "msg:isResponseTo");
				message.setIsResponseToRef(other);
				other.setIsResponseToInverseRef(message);
			}
			if (message.getIsRemoteResponseTo() != null && ! message.getIsRemoteResponseTo().equals(message.getMessageURI())) {
				ConversationMessage other = messagesByURI.get(message.getIsRemoteResponseTo());
				throwExceptionIfOtherisMissing(message.getMessageURI(), message.getIsRemoteResponseTo(), other, "msg:isRemoteResponseTo");
				message.setIsRemoteResponseToRef(other);
				other.setIsRemoteResponseToInverseRef(message);
			}
			if (message.getPrevious().isEmpty()) {
				roots.add(message);
			}
		});

		//link messages to deliveryChains
		deliveryChains = 
				messages.stream().map(m -> {
					if (logger.isDebugEnabled()) {
						logger.debug("deliveryChain for message {}: {}", m.getMessageURI(), m.getDeliveryChain());
					}
					return m.getDeliveryChain();
				})
				.collect(Collectors.toSet());
		
		//find interleaved delivery chains
		deliveryChains.stream().forEach(dc -> deliveryChains.stream().forEach(dc2 -> {
			dc.determineRelationshipWith(dc2);	
		}));			
				

		//apply acknowledgment protocol to whole conversation first:
		conversation = acknowledgedSelection(conversationDataset, messages);
		
		//on top of this, apply modification and agreement protocol on a per-message basis, starting with the root(s)
		
		//expect proposals and agreements to be empty
		
		PriorityQueue<ConversationMessage> currentMessages = 
				new PriorityQueue<ConversationMessage>();
		currentMessages.addAll(messages);
		
		//TODO: we need to use a priority queue for the messages, which is 
		//sorted by temporal ordering. Each time we process a message, we 
		//add the subsequent ones to the queue, the retrieve the 
		//oldest from the queue for the next iteration.

		Set<ConversationMessage> processed = new HashSet<>();
		List<ConversationMessage> processedInOrder = null;
		if (logger.isDebugEnabled()) {
			processedInOrder = new ArrayList<>();
		}
		ConversationMessage last = null;
		while(!currentMessages.isEmpty()) {

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
			if (!msg.isHeadOfDeliveryChain() ) {
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
				msg.getRetractsRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> other.getSenderNeedURI().equals(msg.getSenderNeedURI()))
					.filter(other -> other.isHeadOfDeliveryChain())
					.filter(other -> msg.isMessageOnPathToRoot(other))
					
					.forEach(other -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("{} retracts {}: valid", msg.getMessageURI(), other.getMessageURI());
                        }
                        boolean changedSomething = false;
                        changedSomething = removeContentGraphs(conversation, other) || changedSomething;
                        retractedUris.add(other.getMessageURI());
                        if (other.isProposesMessage()) {
                            changedSomething = retractProposal(other.getMessageURI()) || changedSomething;
                        }
                        if (changedSomething) {
                            effectsBuilder.retracts(other.getMessageURI());
                        }
                    });
			}
			if (msg.isRejectsMessage()) {
				removeContentGraphs(conversation, msg);
				if (logger.isDebugEnabled()) {
					msg.getRejectsRefs().forEach(other -> {
						logger.debug("{} rejects {}", msg.getMessageURI(), other.getMessageURI());
					});
				}
				msg.getRejectsRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> other.isProposesMessage())
					.filter(other -> other.isHeadOfDeliveryChain())
					.filter(other -> ! other.getSenderNeedURI().equals(msg.getSenderNeedURI()))
					.filter(other -> msg.isMessageOnPathToRoot(other))
					.filter(other -> {
						// check if msg also accepts other - in that case, the message is contradictory in itself 
						// Resolution: neither statement has any effect.
						return !msg.accepts(other);
					})
					.forEach(other -> {
						if (logger.isDebugEnabled()) {
							logger.debug("{} rejects {}: valid", msg.getMessageURI(), other.getMessageURI());
						}
						boolean changedSomething = rejectProposal(other.getMessageURI());
						if (changedSomething) {
							effectsBuilder.rejects(other.getMessageURI());
						}
				});
			}
			if (msg.isProposesMessage()) {
				if (logger.isDebugEnabled()) {
					msg.getProposesRefs().forEach(other -> {
						logger.debug("{} proposes {}", msg.getMessageURI(), other.getMessageURI());
					});
				}
				Model proposalContent = ModelFactory.createDefaultModel();
				msg.getProposesRefs().stream()
				.filter(other -> msg != other)
				.filter(other -> other.isHeadOfDeliveryChain())
				.filter(other -> msg.isMessageOnPathToRoot(other))
				.forEach(other -> {
					if (logger.isDebugEnabled()) {
						logger.debug("{} proposes {}: valid", msg.getMessageURI(), other.getMessageURI());
					}
					boolean changedSomething =  propose(conversationDataset, other.getContentGraphs(), proposalContent);
					if (changedSomething) {
						effectsBuilder.proposes(other.getMessageURI());
					}
				});
				

				pendingProposals.addNamedModel(msg.getMessageURI().toString(), proposalContent);
			}
			if (msg.isAcceptsMessage()) {
				if (logger.isDebugEnabled()) {
					msg.getAcceptsRefs().forEach(other -> {
						logger.debug("{} accepts {}", msg.getMessageURI(), other.getMessageURI());
					});
				}
				msg.getAcceptsRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> other.isHeadOfDeliveryChain())
					.filter(other -> ! other.getSenderNeedURI().equals(msg.getSenderNeedURI()))
					.filter(other -> msg.isMessageOnPathToRoot(other))
					.filter(other -> {
						// check if msg also accepts other - in that case, the message is contradictory in itself 
						// Resolution: neither statement has any effect.
						return !msg.rejects(other);
					})
					.forEach(other -> {
						if (logger.isDebugEnabled()) {
							logger.debug("{} accepts {}: valid", msg.getMessageURI(), other.getMessageURI());
						}
						boolean changedSomething = acceptProposal(other.getMessageURI());
						if (changedSomething) { 
							effectsBuilder.accepts(other.getMessageURI(), other.getProposesToCancel().stream().collect(Collectors.toSet()));
						}
					});
			}
			if (msg.isProposesToCancelMessage()) {
				if (logger.isDebugEnabled()) {
					msg.getProposesToCancelRefs().forEach(other -> {
						logger.debug("{} proposesToCancel {}", msg.getMessageURI(), other.getMessageURI());
					});
				}
				final Model cancellationProposals = pendingProposals.getDefaultModel();
				msg.getProposesToCancelRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> other.isHeadOfDeliveryChain())
					.filter(toCancel -> msg.isMessageOnPathToRoot(toCancel))
					.forEach(other -> {
						if (logger.isDebugEnabled()) {
							logger.debug("{} proposesToCancel {}: valid", msg.getMessageURI(), other.getMessageURI());
						}
					cancellationProposals.add(new StatementImpl(
							cancellationProposals.getResource(msg.getMessageURI().toString()),
							WONAGR.PROPOSES_TO_CANCEL,
							cancellationProposals.getResource(other.getMessageURI().toString())));
					pendingProposals.setDefaultModel(cancellationProposals);
					effectsBuilder.proposesToCancel(other.getMessageURI());
				});
			}
			msg.setEffects(effectsBuilder.build());
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
		conversationDataset.end();
	}
	
	private void throwExceptionIfOtherisMissing(URI messageUri, URI otherMessageUri, ConversationMessage otherMessage, String predicate) {
		if (otherMessage != null) {
			return;
		}
		throw new IncompleteConversationDataException(messageUri, otherMessageUri, predicate);
	}
	
	
	private  Dataset acknowledgedSelection(Dataset conversationDataset, Collection<ConversationMessage> messages ) {
		Dataset copy = RdfUtils.cloneDataset(conversationDataset);
		messages.stream().forEach(message -> {
			if (message.getMessageType() == null) {
				return;
			}
			if (message.getDirection() == WonMessageDirection.FROM_EXTERNAL) {
				return;
			}
			if (message.getDirection() == WonMessageDirection.FROM_SYSTEM && ! message.isResponse()) {
				if (!message.isAcknowledgedLocally()) {
					notAcknowledged(copy, message);
				}
				return;
			}
			if (!message.isHeadOfDeliveryChain()) {
				// here, we are only concerned with removing content graphs of the 'main' message in a delivery chain.
				// any other message does not concern us here.
				return;
			}
			switch (message.getMessageType()) {
				case SUCCESS_RESPONSE:
				case FAILURE_RESPONSE:
					break;
				case CREATE_NEED:
				case HINT_FEEDBACK_MESSAGE:
				case DEACTIVATE:
				case ACTIVATE:
				case HINT_MESSAGE:
					if (!message.isAcknowledgedLocally()) {
						notAcknowledged(copy, message);
					}
					break;
				case CONNECT:
				case OPEN:
				case CONNECTION_MESSAGE :
				case CLOSE:
					if (!message.isAcknowledgedRemotely()) {
						notAcknowledged(copy, message);
					}
				default:
					break;
			}
			// delivery chain checking:
			// 1. if a chain contains another (first msg before, last msg after the other),
			// 	  the containing chain is disregarded (content graph of root message removed
			// 2. if two delivery chains are interleaved, and none contains the other, both are disregarded.

			DeliveryChain msgChain = message.getDeliveryChain();
			if (msgChain.containsOtherChains()) {
				// In this case, it is not possible to determine if the other
				// one has been delayed maliciously. Removing it is the only safe option. Downside: It allows the recipient of the
				// message to delay it such that it is removed by this rule, but that at least has immediate effects: the 
				// message never seems acknowledged and then later, when some other chain terminates, is found not to. 
				// Rather, as soon as the chain of such a maliciously delayed message terminates, it is dropped.
				if (logger.isDebugEnabled()) {
					logger.debug("ignoring delivery chain {} as it contains other chains", msgChain.getHeadURI()); 
				}
				notAcknowledged(copy, message);
			} else {
				msgChain.getInterleavedDeliveryChains().stream()
				.filter(otherChain -> otherChain.isTerminated())
				.forEach(otherChain -> {
						// "interleaved" relationship is symmetric -> drop this message (chain), the other message will be dropped when it is processed 
						if (logger.isDebugEnabled()) {
							logger.debug("dropping delivery chain {} as it is interleaved with {}",  message.getMessageURI(), otherChain.getHead().getMessageURI());
						}
						notAcknowledged(copy, message);
					}
				);
			}
		});
		return copy;
	}

	

	private  void notAcknowledged(Dataset copy, ConversationMessage message) {
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
	private boolean removeContentGraphs(Dataset conversationDataset, ConversationMessage message ) {
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
	 * 
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
		//did we add anything?
		return proposal.size() - initialSize > 0;
	}
	
	/**
	 * 
	 * @param proposalUri
	 * @return true if the operation had any effect, false otherwise
	 */
	private boolean acceptProposal(URI proposalUri) {
		boolean changedSomething = false;
		// first process proposeToCancel triples - this avoids that a message can 
		// successfully propose to cancel itself, as agreements are only made after the
		// cancellations are processed.		
		Model cancellationProposals = pendingProposals.getDefaultModel();
		NodeIterator nIt = cancellationProposals.listObjectsOfProperty(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL);
		if (nIt.hasNext()) {
			//remember that this proposal contained a cancellation
			this.acceptedCancellationProposalUris.add(proposalUri);
			changedSomething = true;
		}
		while (nIt.hasNext()){
			RDFNode agreementToCancelUri = nIt.next();
			changedSomething = cancelAgreement(URI.create(agreementToCancelUri.asResource().getURI())) || changedSomething;
		}
		changedSomething = removeCancellationProposal(proposalUri) || changedSomething;
		
		// move proposal to agreements
		changedSomething = moveNamedGraph(proposalUri, pendingProposals, agreements) || changedSomething;
		return changedSomething;
		
	}
	
	/**
	 * 
	 * @param proposalUri
	 * @return true if the operation had any effect, false otherwise
	 */
	private boolean retractProposal(URI proposalUri) {
		boolean changedSomething = false;
		// we don't track retracted proposals (nobody cares about retracted proposals)
		// so just remove them
		if (pendingProposals.containsNamedModel(proposalUri.toString())){
			changedSomething = true;
		}
		pendingProposals.removeNamedModel(proposalUri.toString());
		changedSomething = removeCancellationProposal(proposalUri) || changedSomething;
		return changedSomething;
	}
	
	/**
	 * 
	 * @param proposalUri
	 * @return true if the operation had any effect, false otherwise
	 */
	private boolean rejectProposal(URI proposalUri) {
		boolean changedSomething = moveNamedGraph(proposalUri, pendingProposals, rejected);
		changedSomething = removeCancellationProposal(proposalUri) || changedSomething;
		return changedSomething;
	}
	
	/**
	 * 
	 * @param toCancel
	 * @return true if the operation had any effect, false otherwise
	 */
	private boolean cancelAgreement(URI toCancel) {
		return moveNamedGraph(toCancel, agreements, cancelledAgreements);
	}
	
	/**
	 * 
	 * @param proposalUri
	 * @return true if the operation had any effect, false otherwise
	 */
	private boolean removeCancellationProposal(URI proposalUri) {
		boolean changedSomething = false;
		Model cancellationProposals = pendingProposals.getDefaultModel();
		StmtIterator it = cancellationProposals.listStatements(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL, (RDFNode) null);
		changedSomething = it.hasNext();
		cancellationProposals.remove(it);
		return changedSomething;
	}
	
	/**
	 * 
	 * @param graphUri
	 * @param fromDataset
	 * @param toDataset
	 * @return true if the operation had any effect, false otherwise
	 */
	private  boolean moveNamedGraph(URI graphUri, Dataset fromDataset, Dataset toDataset) {
		boolean changedSomething = false;
		Model model = fromDataset.getNamedModel(graphUri.toString());
		fromDataset.removeNamedModel(graphUri.toString());
		if (model != null && model.size() > 0) {
			toDataset.addNamedModel(graphUri.toString(), model);
			changedSomething = true;
		}
		return changedSomething;
	}


}
