package won.protocol.agreement;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.agreement.effect.MessageEffect;
import won.protocol.agreement.effect.MessageEffectsBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONAGR;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class AgreementProtocolState {
	private  final Logger logger = LoggerFactory.getLogger(AgreementProtocolState.class);
	
	private final Dataset pendingProposals = DatasetFactory.createGeneral();
	private final Dataset agreements = DatasetFactory.createGeneral();
	private final Dataset cancelledAgreements = DatasetFactory.createGeneral();
	private final Dataset rejected = DatasetFactory.createGeneral();
	private final Set<URI> retractedUris = new HashSet<URI>();
	private final Set<URI> acceptedCancellationProposalUris = new HashSet<URI>();
	private Map<URI, ConversationMessage> messagesByURI = new HashMap<>();
	
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
	
	public Dataset getPendingProposals() {
		return pendingProposals;
	}
	
	public Model getPendingProposal(URI proposalURI) {
		return pendingProposals.getNamedModel(proposalURI.toString());
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
	
	public Dataset getRejectedProposals() {
		return rejected;
	}
	
	public Model getRejectedProposal(URI rejectedProposalURI) {
		return rejected.getNamedModel(rejectedProposalURI.toString());
	}
	
	public Model getPendingCancellations() {
		return pendingProposals.getDefaultModel();
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
                if (other == null){
                    message.setCorrespondingRemoteMessageURI(null);
                } else {
                    message.setCorrespondingRemoteMessageRef(other);
                    other.setCorrespondingRemoteMessageRef(message);
                }
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
		Set<DeliveryChain> deliveryChains = 
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
		Dataset conversation = acknowledgedSelection(conversationDataset, messages);
		
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
		throw new WonProtocolException("message " + messageUri + " refers to other " + otherMessageUri + " via " + predicate + ", but that other message is not present in the conversation");
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
