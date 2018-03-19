package won.protocol.highlevel;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v1.lang.GentleThread;

import won.protocol.message.WonMessageDirection;
import won.protocol.util.RdfUtils;
import won.protocol.util.SparqlSelectFunction;
import won.protocol.vocabulary.WONAGR;


public class HighlevelProtocolAnalyzer {
	private  final Logger logger = LoggerFactory.getLogger(HighlevelProtocolAnalyzer.class);
	
	private final Dataset proposals = DatasetFactory.createGeneral();
	private final Dataset agreements = DatasetFactory.createGeneral();
	private final Dataset cancelled = DatasetFactory.createGeneral();
	private final Dataset rejected = DatasetFactory.createGeneral();
	private final Set<URI> retractedUris = new HashSet<URI>();
	
	public HighlevelProtocolAnalyzer(Dataset conversation) {
		recalculate(conversation);
	}
	
	public Dataset getAgreements() {
		return agreements;
	}
	
	public Dataset getProposals() {
		return proposals;
	}
	
	public Dataset getCancelledAgreements() {
		return cancelled;
	}
	
	public Model getPendingCancellations() {
		return proposals.getDefaultModel();
	}
	
	
	public Set<URI> getAgreementUris(){
		return RdfUtils.getGraphUris(agreements);		
	}
	
	public Set<URI> getProposalUris(){
		return RdfUtils.getGraphUris(proposals);		
	}	
	
	public Set<URI> getCancelledAreementUris(){
		return RdfUtils.getGraphUris(cancelled);		
	}
	
	public Set<URI> getRetractedUris() {
		return retractedUris;
	}
	
	public Set<URI> getProposedToBeCancelledAgreementUris(){
		Model cancellations = proposals.getDefaultModel();
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
	
	public Set<URI> getRejectedProposalUris(){
		return RdfUtils.getGraphUris(rejected);		
	}
	
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	private void recalculate(Dataset conversationDataset) {
		if (logger.isDebugEnabled()) {
			logger.debug("starting conversation analysis for high-level protocols");
		}
		proposals.begin(ReadWrite.WRITE);
		agreements.begin(ReadWrite.WRITE);
		cancelled.begin(ReadWrite.WRITE);
		rejected.begin(ReadWrite.WRITE);
		conversationDataset.begin(ReadWrite.READ);
		
		Map<URI, ConversationMessage> messagesByURI = new HashMap<>();
		ConversationResultMapper resultMapper = new ConversationResultMapper(messagesByURI);
		SparqlSelectFunction<ConversationMessage> selectfunction = 
				new SparqlSelectFunction<>("/conversation/messagesForHighlevelProtocols.rq", resultMapper )
				.addOrderBy("msg", Query.ORDER_ASCENDING);
		selectfunction.apply(conversationDataset);
		
		Set<ConversationMessage> roots = new HashSet();
		Collection<ConversationMessage> messages = messagesByURI.values();
		
		
		//iterate over messages and interconnect them
		messages.stream().forEach(message -> {
			if (message.getCorrespondingRemoteMessageURI() != null && ! message.getCorrespondingRemoteMessageURI().equals(message.getMessageURI())) {
				ConversationMessage other = messagesByURI.get(message.getCorrespondingRemoteMessageURI());
				message.setCorrespondingRemoteMessageRef(other);
				other.setCorrespondingRemoteMessageRef(message);
			}
			message.getPrevious().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addPreviousRef(other);
				other.addPreviousInverseRef(message);
			});
			message.getAccepts().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addAcceptsRef(other); 
				other.addAcceptsInverseRef(message);
			});
			message.getProposes().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addProposesRef(other);
				other.addProposesInverseRef(message);
				});
			message.getRejects().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addRejectsRef(other);
				other.addRejectsInverseRef(message);
				});
			message.getProposesToCancel().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addProposesToCancelRef(other);
				other.addProposesToCancelInverseRef(message);
				});
			message.getRetracts().stream().filter(uri -> !uri.equals(message.getMessageURI()))
				.forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addRetractsRef(other);
				other.addRetractsInverseRef(message);
				});
			if (message.getIsResponseTo() != null && ! message.getIsResponseTo().equals(message.getMessageURI())) {
				ConversationMessage other = messagesByURI.get(message.getIsResponseTo());
				message.setIsResponseToRef(other);
				other.setIsResponseToInverseRef(message);
			}
			if (message.getIsRemoteResponseTo() != null && ! message.getIsRemoteResponseTo().equals(message.getMessageURI())) {
				ConversationMessage other = messagesByURI.get(message.getIsRemoteResponseTo());
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
		conversationDataset.end();
		
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
			if (logger.isDebugEnabled() && processedInOrder != null) {
				processedInOrder.add(msg);
			}
			
			
			last = msg;
			if (!msg.isHeadOfDeliveryChain() ) {
				continue;
			}
			if (!msg.isHighlevelProtocolMessage()) {
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
						removeContentGraphs(conversation, other);
						retractedUris.add(other.getMessageURI());
						if (other.isProposesMessage()) {
							retractProposal(other.getMessageURI());
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
					.forEach(other -> {
						if (logger.isDebugEnabled()) {
							logger.debug("{} rejects {}: valid", msg.getMessageURI(), other.getMessageURI());
						}
						rejectProposal(other.getMessageURI());
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
					proposalContent.add(aggregateGraphs(conversation, other.getContentGraphs()));
				});
				

				proposals.addNamedModel(msg.getMessageURI().toString(), proposalContent);
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
					.forEach(other -> {
						if (logger.isDebugEnabled()) {
							logger.debug("{} accepts {}: valid", msg.getMessageURI(), other.getMessageURI());
						}
						acceptProposal(other.getMessageURI());
					});
			}
			if (msg.isProposesToCancelMessage()) {
				if (logger.isDebugEnabled()) {
					msg.getProposesToCancelRefs().forEach(other -> {
						logger.debug("{} proposesToCancel {}", msg.getMessageURI(), other.getMessageURI());
					});
				}
				final Model cancellationProposals = proposals.getDefaultModel();
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
					proposals.setDefaultModel(cancellationProposals);
				});
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("messages in the order they were processed:");
			if (processedInOrder != null) {
				processedInOrder.stream().forEach(x -> logger.debug(x.toString()));
			}
			logger.debug("finished conversation analysis for high-level protocols");
		}
		
		proposals.commit();
		agreements.commit();
		cancelled.commit();
		rejected.commit();
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
	
	private  void removeContentGraphs(Dataset conversationDataset, ConversationMessage message ) {
		message.getContentGraphs().stream().forEach(uri -> conversationDataset.removeNamedModel(uri.toString()));
	}
	
	private  Model aggregateGraphs(Dataset conversationDataset, Collection<URI> graphURIs) {
		Model result = ModelFactory.createDefaultModel();
		graphURIs.forEach(uri -> {
			Model graph = conversationDataset.getNamedModel(uri.toString());
			if (graph != null) {
				result.add(RdfUtils.cloneModel(graph));
			}
		});
		return result;
	}
	
	private void acceptProposal(URI proposalUri) {
		// first process proposeToCancel triples - this avoids that a message can 
		// successfully propose to cancel itself, as agreements are only made after the
		// cancellations are processed.		
		Model cancellationProposals = proposals.getDefaultModel();
		NodeIterator nIt = cancellationProposals.listObjectsOfProperty(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL);
		while (nIt.hasNext()){
			RDFNode agreementToCancelUri = nIt.next();
			cancelAgreement(URI.create(agreementToCancelUri.asResource().getURI()));
		}
		removeCancellationProposal(proposalUri);
		
		// move proposal to agreements
		moveNamedGraph(proposalUri, proposals, agreements);
		
	}
	
	private void retractProposal(URI proposalUri) {
		// we don't track retracted proposals (nobody cares about retracted proposals)
		// so just remove them
		proposals.removeNamedModel(proposalUri.toString());
		removeCancellationProposal(proposalUri);
	}
	
	private void rejectProposal(URI proposalUri) {
		moveNamedGraph(proposalUri, proposals, rejected);
		removeCancellationProposal(proposalUri);
	}
	
	private void cancelAgreement(URI toCancel) {
		moveNamedGraph(toCancel, agreements, cancelled);
	}
	
	private void removeCancellationProposal(URI proposalUri) {
		Model cancellationProposals = proposals.getDefaultModel();
		NodeIterator nIt = cancellationProposals.listObjectsOfProperty(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL);
		while (nIt.hasNext()){
			RDFNode agreementToCancelUri = nIt.next();
			agreements.removeNamedModel(agreementToCancelUri.asResource().getURI());
		}
		cancellationProposals.remove(cancellationProposals.listStatements(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL, (RDFNode) null));
	}
	
	private  void moveNamedGraph(URI graphUri, Dataset fromDataset, Dataset toDataset) {
		Model model = fromDataset.getNamedModel(graphUri.toString());
		fromDataset.removeNamedModel(graphUri.toString());
		if (model != null && model.size() > 0) {
			toDataset.addNamedModel(graphUri.toString(), model);
		}
	}

	
	

}
