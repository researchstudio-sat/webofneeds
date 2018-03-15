package won.protocol.highlevel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import won.protocol.message.WonMessageDirection;
import won.protocol.util.DynamicDatasetToDatasetBySparqlGSPOSelectFunction;
import won.protocol.util.RdfUtils;
import won.protocol.util.SparqlSelectFunction;
import won.protocol.vocabulary.WONAGR;


public class HighlevelProtocolAnalyzer {
	private  final Logger logger = LoggerFactory.getLogger(HighlevelProtocolAnalyzer.class);
	
	private final Dataset proposals = DatasetFactory.createGeneral();
	private final Dataset agreements = DatasetFactory.createGeneral();
	
	public HighlevelProtocolAnalyzer(Dataset conversation) {
		recalculate(conversation);
	}
	
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	private void recalculate(Dataset conversationDataset) {
		if (logger.isDebugEnabled()) {
			logger.debug("starting conversation analysis for high-level protocols");
		}
		
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
			dc.rememberIfInterleavedWith(dc2);	
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
							logger.debug("{} retracts {}: passes all tests", msg.getMessageURI(), other.getMessageURI());
						}
						removeContentGraphs(conversation, other);
						if (other.isProposesMessage()) {
							removeProposal(other.getMessageURI(), proposals);
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
							logger.debug("{} rejects {}: passes all tests", msg.getMessageURI(), other.getMessageURI());
						}
						removeProposal(other.getMessageURI(), proposals);
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
						logger.debug("{} proposes {}: passes all tests", msg.getMessageURI(), other.getMessageURI());
					}
					proposalContent.add(aggregateGraphs(conversation, other.getContentGraphs()));
				});
				

				proposals.begin(ReadWrite.WRITE);
				proposals.addNamedModel(msg.getMessageURI().toString(), proposalContent);
				proposals.commit();
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
							logger.debug("{} accepts {}: passes all tests", msg.getMessageURI(), other.getMessageURI());
						}
						acceptProposal(other.getMessageURI(),other.getMessageURI(), proposals, agreements);
					});
			}
			if (msg.isProposesToCancelMessage()) {
				if (logger.isDebugEnabled()) {
					msg.getProposesToCancelRefs().forEach(other -> {
						logger.debug("{} proposesToCancel {}", msg.getMessageURI(), other.getMessageURI());
					});
				}
				proposals.begin(ReadWrite.WRITE);
				final Model cancellationProposals = proposals.getDefaultModel();
				msg.getProposesToCancelRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> other.isHeadOfDeliveryChain())
					.filter(toCancel -> msg.isMessageOnPathToRoot(toCancel))
					.forEach(other -> {
						if (logger.isDebugEnabled()) {
							logger.debug("{} proposesToCancel {}: passes all tests", msg.getMessageURI(), other.getMessageURI());
						}
					cancellationProposals.add(new StatementImpl(
							cancellationProposals.getResource(msg.getMessageURI().toString()),
							WONAGR.PROPOSES_TO_CANCEL,
							cancellationProposals.getResource(other.getMessageURI().toString())));
					proposals.setDefaultModel(cancellationProposals);
				});
				proposals.commit();
				
			}
			

		}
		if (logger.isDebugEnabled()) {
			logger.debug("messages in the order they were processed:");
			if (processedInOrder != null) {
				processedInOrder.stream().forEach(x -> logger.debug(x.toString()));
			}
			logger.debug("finished conversation analysis for high-level protocols");
		}
		conversationDataset.end();
	}
	
	public Dataset getAgreements() {
		return agreements;
	}
	
	public Dataset getProposals() {
		return proposals;
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
			// 1. if delivery chains are interleaved, and one contains the other (first msg before, last msg after the other),
			// 	  the containing chain is disregarded (content graph of root message removed
			// 2. if two delivery chains are interleaved, and none contains the other, both are disregarded.

			DeliveryChain msgChain = message.getDeliveryChain();
			msgChain.getInterleavedDeliveryChains().stream()
				.filter(otherChain -> otherChain.isTerminated())
				.forEach(otherChain -> {
					// does the other contain this one? In this case, it is not possible to determine if the other
					// one has been delayed maliciously. Removing it is the only safe option. Downside: It allows the recipient of the
					// message to delay it such that it is removed by this rule, but that at least has immediate effects: the 
					// message never seems acknowledged and then later, when some other chain terminates, is found not to. 
					// Rather, as soon as the chain of such a maliciously delayed message terminates, it is dropped.
					if (otherChain.contains(msgChain)) {
						//remove the other
						if (logger.isDebugEnabled()) {
							logger.debug("ignoring delivery chain {} as it conatins {}", otherChain.getHead().getMessageURI(), message.getMessageURI());
						}
						notAcknowledged(copy, otherChain.getHead());
					} else {
						//the other does not contain this one: 
						// * either this one contains the other -> drop this one
						// * or both are same-time: drop both  (in which case the other one is removed when that message's chain is checked)
						if (logger.isDebugEnabled()) {
							logger.debug("ignoring delivery chain {} as it is interleaved with {}",  message.getMessageURI(), otherChain.getHead().getMessageURI());
						}
						notAcknowledged(copy, message);
					}
				});
		
		{
				
			}

		});
		return copy;
	}



	private  void notAcknowledged(Dataset copy, ConversationMessage message) {
		message.removeHighlevelProtocolProperties();
		removeContentGraphs(copy, message);
	}
	
	private  void removeContentGraphs(Dataset conversationDataset, ConversationMessage message ) {
		conversationDataset.begin(ReadWrite.WRITE);
		message.getContentGraphs().stream().forEach(uri -> conversationDataset.removeNamedModel(uri.toString()));
		conversationDataset.commit();
	}
	
	private  Model aggregateGraphs(Dataset conversationDataset, Collection<URI> graphURIs) {
		conversationDataset.begin(ReadWrite.READ);
		Model result = ModelFactory.createDefaultModel();
		graphURIs.forEach(uri -> {
			Model graph = conversationDataset.getNamedModel(uri.toString());
			if (graph != null) {
				result.add(RdfUtils.cloneModel(graph));
			}
		});
		conversationDataset.end();
		return result;
	}
	
	private  void acceptProposal(URI proposalUri, URI agreementUri, Dataset proposals, Dataset agreements) {
		proposals.begin(ReadWrite.WRITE);
		agreements.begin(ReadWrite.WRITE);
		// first process proposeToCancel triples - this avoids that a message can 
		// successfully propose to cancel itself, as agreements are only made after the
		// cancellations are processed.		
		Model cancellationProposals = proposals.getDefaultModel();
		
		NodeIterator nIt = cancellationProposals.listObjectsOfProperty(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL);

		while (nIt.hasNext()){
			RDFNode agreementToCancelUri = nIt.next();
			agreements.removeNamedModel(agreementToCancelUri.asResource().getURI());
		}
		cancellationProposals.remove(cancellationProposals.listStatements(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL, (RDFNode) null));
		proposals.commit();
		agreements.commit();

		proposals.begin(ReadWrite.WRITE);
		agreements.begin(ReadWrite.WRITE);
		Model proposal = RdfUtils.cloneModel(proposals.getNamedModel(proposalUri.toString()));
		proposals.removeNamedModel(proposalUri.toString());
		if (proposal != null && proposal.size() > 0 ) {
			if (agreements.containsNamedModel(agreementUri.toString())) {
				Model m = agreements.getNamedModel(agreementUri.toString());
				m.add(proposal);
				agreements.addNamedModel(agreementUri.toString(),	m);
			} else {
				agreements.addNamedModel(agreementUri.toString(), proposal);
			}
		}
		proposals.commit();
		agreements.commit();
	}
	
	private  void removeProposal(URI proposalUri, Dataset proposals) {
		proposals.begin(ReadWrite.WRITE);
		proposals.removeNamedModel(proposalUri.toString());
		proposals.commit();
	}
	
	private  void cancelAgreement(URI toCancel, Dataset agreements) {
		agreements.begin(ReadWrite.WRITE);
		agreements.removeNamedModel(toCancel.toString());
		agreements.commit();
	}
	

}
