package won.protocol.highlevel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

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
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.message.WonMessageDirection;
import won.protocol.util.DynamicDatasetToDatasetBySparqlGSPOSelectFunction;
import won.protocol.util.RdfUtils;
import won.protocol.util.SparqlSelectFunction;
import won.protocol.vocabulary.WONAGR;


public class HighlevelProtocols {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static DynamicDatasetToDatasetBySparqlGSPOSelectFunction cutAfterMessageFunction;
	
	
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {
		System.out.println("---------------- begin -------------------------");
		//Dataset ack = HighlevelFunctionFactory.getAcknowledgedSelection().apply(conversationDataset);
		
		Map<URI, ConversationMessage> messagesByURI = new HashMap<>();
		ConversationResultMapper resultMapper = new ConversationResultMapper(messagesByURI);
		SparqlSelectFunction<ConversationMessage> selectfunction = 
				new SparqlSelectFunction<>("/conversation/messagesForHighlevelProtocols.rq", resultMapper )
				.addOrderBy("msg", Query.ORDER_ASCENDING);
		selectfunction.apply(conversationDataset);
		
		Set<ConversationMessage> roots = new HashSet();
		Collection<ConversationMessage> messages = messagesByURI.values();
		System.out.println("processed " + resultMapper.getCount() + " sparql solutions");
		
		//iterate over messages and interconnect them
		messages.stream().forEach(message -> {
			if (message.getCorrespondingRemoteMessageURI() != null) {
				ConversationMessage other = messagesByURI.get(message.getCorrespondingRemoteMessageURI());
				message.setCorrespondingRemoteMessageRef(other);
				other.setCorrespondingRemoteMessageRef(message);
			}
			message.getPrevious().stream().forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addPreviousRef(other);
				other.addPreviousInverseRef(message);
			});
			message.getAccepts().stream().forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addAcceptsRef(other); 
				other.addAcceptsInverseRef(message);
			});
			message.getProposes().stream().forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addProposesRef(other);
				other.addProposesInverseRef(message);
				});
			message.getProposesToCancel().stream().forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addProposesToCancelRef(other);
				other.addProposesToCancelInverseRef(message);
				});
			message.getRetracts().stream().forEach(uri -> {
				ConversationMessage other = messagesByURI.get(uri);
				message.addRetractsRef(other);
				other.addRetractsInverseRef(message);
				});
			if (message.getIsResponseTo() != null) {
				ConversationMessage other = messagesByURI.get(message.getIsResponseTo());
				message.setIsResponseToRef(other);
				other.setIsResponseToInverseRef(message);
			}
			if (message.getIsRemoteResponseTo() != null) {
				ConversationMessage other = messagesByURI.get(message.getIsRemoteResponseTo());
				message.setIsRemoteResponseToRef(other);
				other.setIsRemoteResponseToInverseRef(message);
			}
			if (message.getPrevious().isEmpty()) {
				roots.add(message);
			}
		});
		
		//link messages to deliveryChains
		messages.forEach(m -> m.getDeliveryChain());
		 
		

		//apply acknowledgment protocol to whole conversation first:
		Dataset conversation = acknowledgedSelection(conversationDataset, messages);
		
		//on top of this, apply modification and agreement protocol on a per-message basis, starting with the root(s)
		
		Dataset proposals = DatasetFactory.createGeneral();
		Dataset agreements = DatasetFactory.createGeneral();
		
		
		PriorityQueue<ConversationMessage> currentMessages = 
				new PriorityQueue<ConversationMessage>();
		currentMessages.addAll(roots);
		
		//TODO: we need to use a priority queue for the messages, which is 
		//sorted by temporal ordering. Each time we process a message, we 
		//add the subsequent ones to the queue, the retrieve the 
		//oldest from the queue for the next iteration.

		Set<ConversationMessage> processed = new HashSet<>();
		ConversationMessage last = null;
		while(!currentMessages.isEmpty()) {

			ConversationMessage msg = currentMessages.poll();
			if (processed.contains(msg)) {
				continue;
			}
			processed.add(msg);
			System.out.println("current:" + msg);
			System.out.println("deliveryChain of current:" + msg.getDeliveryChain().getRootURI());
			System.out.println("deliveryChain size:" + msg.getDeliveryChain().getMessages().size());
			if (last != null) {
				System.out.println("previous compared to current:" + compare4Dbg(last, msg));
				System.out.println("deliveryChain of last:" + last.getDeliveryChain().getRootURI());
				System.out.println("deliveryChains are interleaved:" + msg.getDeliveryChain().isInterleavedDeliveryChain(last.getDeliveryChain()));
			}
			
			last = msg;

			if (msg.isRetractsMessage()) {
				System.out.println("retracting message:" + msg.getMessageURI());
				removeContentGraphs(conversation, msg);
				msg.getRetractsRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> other.getSenderNeedURI().equals(msg.getSenderNeedURI()))
					.filter(other -> msg.isMessageOnPathToRoot(other))
					.forEach(toRetract -> {
						removeContentGraphs(conversation, toRetract);
						if (toRetract.isProposesMessage()) {
							removeProposal(toRetract.getMessageURI(), proposals);
						}
					});
			}
			if (msg.isProposesMessage()) {
				Model proposalContent = ModelFactory.createDefaultModel();
				msg.getProposesRefs().stream()
				.filter(other -> msg != other)
				.filter(other -> msg.isMessageOnPathToRoot(other))
				.forEach(clause -> {
					System.out.println("proposing: " + clause.getMessageURI() + " in proposal: " + msg.getMessageURI());
					proposalContent.add(aggregateGraphs(conversation, clause.getContentGraphs()));
				});
				System.out.println("proposal: "+ msg.getMessageURI() + ":");
				RDFDataMgr.write(System.out, proposalContent, Lang.TURTLE);
				proposals.begin(ReadWrite.WRITE);
				proposals.addNamedModel(msg.getMessageURI().toString(), proposalContent);
				proposals.commit();
			}
			if (msg.isAcceptsMessage()) {
				msg.getAcceptsRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(other -> ! other.getSenderNeedURI().equals(msg.getSenderNeedURI()))
					.filter(other -> msg.isMessageOnPathToRoot(other))
					.forEach(accepted -> {
						acceptProposal(accepted.getMessageURI(),msg.getMessageURI(), proposals, agreements);
					});
			}
			if (msg.isProposesToCancelMessage()) {
				proposals.begin(ReadWrite.WRITE);
				final Model cancellationProposals = proposals.getDefaultModel();
				msg.getProposesToCancelRefs()
					.stream()
					.filter(other -> msg != other)
					.filter(toCancel -> msg.isMessageOnPathToRoot(toCancel))
					.forEach(proposedToCancel -> {
					System.out.println("proposing to cancel: " + proposedToCancel.getMessageURI() + " in proposal: " + msg.getMessageURI());
					cancellationProposals.add(new StatementImpl(
							cancellationProposals.getResource(msg.getMessageURI().toString()),
							WONAGR.PROPOSES_TO_CANCEL,
							cancellationProposals.getResource(proposedToCancel.getMessageURI().toString())));
					proposals.setDefaultModel(cancellationProposals);
				});
				proposals.commit();
				
			}
			currentMessages.addAll(msg.getPreviousInverseRefs());
			
			RDFDataMgr.write(System.out, proposals.getDefaultModel(), Lang.TURTLE);
		}

		System.out.println("---------------- done -------------------------");
		return agreements;
	}
	
	private static int compare4Dbg(ConversationMessage o1, ConversationMessage o2) {
		int o1dist = o1.distanceToRoot(); 
		int o2dist = o2.distanceToRoot();
		System.out.println("o1dist:" + o1dist + ", o2dist:" + o2dist);
		if (o1dist != o2dist) {
			return o1dist - o2dist;
		}
		if (o1.isAfter(o2)) {
			return 1;
		}
		if (o2.isAfter(o1)) {
			return -1;
		}
		return 0;
	}
	
	private static Dataset acknowledgedSelection(Dataset conversationDataset, Collection<ConversationMessage> messages ) {
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
					System.out.println("not acknowledged locally " + message);
					removeContentGraphs(copy, message);
				}
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
						System.out.println("not acknowledged locally " + message);
						removeContentGraphs(copy, message);
					}
					break;
				case CONNECT:
				case OPEN:
				case CONNECTION_MESSAGE :
				case CLOSE:
					if (!message.isAcknowledgedRemotely()) {
						System.out.println("not acknowledged remotely " + message);
						removeContentGraphs(copy, message);
					}
				default:
					break;
			}
		});
		return copy;
	}
	
	private static void removeContentGraphs(Dataset conversationDataset, ConversationMessage message ) {
		conversationDataset.begin(ReadWrite.WRITE);
		System.out.println("retracting content from : " + message.getMessageURI());
		message.getContentGraphs().stream().forEach(uri -> conversationDataset.removeNamedModel(uri.toString()));
		conversationDataset.commit();
	}
	
	private static Model aggregateGraphs(Dataset conversationDataset, Collection<URI> graphURIs) {
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
	
	private static void acceptProposal(URI proposalUri, URI agreementUri, Dataset proposals, Dataset agreements) {
		proposals.begin(ReadWrite.WRITE);
		agreements.begin(ReadWrite.WRITE);
		// first process proposeToCancel triples - this avoids that a message can 
		// successfully propose to cancel itself, as agreements are only made after the
		// cancellations are processed.		
		Model cancellationProposals = proposals.getDefaultModel();
		
		NodeIterator nIt = cancellationProposals.listObjectsOfProperty(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL);
		System.out.println("seeing what there is to cancel for proposal " + proposalUri + ", default model:");
		RDFDataMgr.write(System.out, cancellationProposals, Lang.TURTLE);
		while (nIt.hasNext()){
			RDFNode agreementToCancelUri = nIt.next();
			System.out.println("cancelling agreement: " + agreementToCancelUri.asResource().getURI());
			agreements.removeNamedModel(agreementToCancelUri.asResource().getURI());
		}
		cancellationProposals.remove(cancellationProposals.listStatements(cancellationProposals.getResource(proposalUri.toString()), WONAGR.PROPOSES_TO_CANCEL, (RDFNode) null));
		proposals.commit();
		agreements.commit();

		proposals.begin(ReadWrite.WRITE);
		agreements.begin(ReadWrite.WRITE);
		System.out.println("accepting: " + proposalUri + " as " + agreementUri);
		RDFDataMgr.write(System.out, proposals.getDefaultModel(), Lang.TURTLE);
		Model proposal = RdfUtils.cloneModel(proposals.getNamedModel(proposalUri.toString()));
		proposals.removeNamedModel(proposalUri.toString());
		if (agreements.containsNamedModel(agreementUri.toString())) {
			Model m = agreements.getNamedModel(agreementUri.toString());
			m.add(proposal);
			agreements.addNamedModel(agreementUri.toString(),	m);
		} else {
			agreements.addNamedModel(agreementUri.toString(), proposal);
		}
		proposals.commit();
		agreements.commit();
	}
	
	private static void removeProposal(URI proposalUri, Dataset proposals) {
		System.out.println("removing proposal: " + proposalUri);
		proposals.begin(ReadWrite.WRITE);
		proposals.removeNamedModel(proposalUri.toString());
		proposals.commit();
	}
	
	private static void cancelAgreement(URI toCancel, Dataset agreements) {
		agreements.begin(ReadWrite.WRITE);
		agreements.removeNamedModel(toCancel.toString());
		agreements.commit();
	}
	
	
	
	
	
	
	
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements2(Dataset conversationDataset) {

		// 1: calculate acknowledged selection 
		// 2: find all accept message uris (only the first one for each proposal) 
		// 3: for each accept message a:
		// 3.1	copy the conversation and cut it off after the accept message
		//      in the cut-off copy:
		// 3.2  feed through modification protocol
		// 3.3  analyze what is being accepted by a: (both cases are possible for one accept message)
		// 3.4 if a is accepting proposalsToCancel, remove the respective agreements from the result
		// 3.5 if a is accepting proposals, add a new agreement a to the result

		Dataset ack = HighlevelFunctionFactory.getAcknowledgedSelection().apply(conversationDataset);
		List<URI> accepts = getAcceptMessages(ack);
		Dataset result = DatasetFactory.createGeneral();
		List<URI> acceptedMessages = new ArrayList<>();
		for(URI acceptsMessageURI: accepts) {
			Dataset cutOff = RdfUtils.cloneDataset(ack);
			cutOff = cutOffAfterMessage(cutOff, acceptsMessageURI);
			Dataset modifiedCutOff = HighlevelFunctionFactory.getModifiedSelection().apply(cutOff);
				
			// System.out.println("modified cutoff:");
			//RDFDataMgr.write(System.out, modifiedCutOff, Lang.TRIG);
		
			// Add agreements, regardless of whether they are cancelled later... (comment added by Brent)
			Model agreement = getAgreement(modifiedCutOff, acceptsMessageURI);
			if (agreement != null && agreement.size() > 0) {
				System.out.println("adding agreement: " + acceptsMessageURI);
				result.addNamedModel(acceptsMessageURI.toString(), agreement);
			}
			
			// System.out.println("result:");
			//RDFDataMgr.write(System.out, result, Lang.TRIG);

			
			// Remove agreements that are cancelled... (comment added by Brent)
			List<URI> retractedAgreementUris = getRetractedAgreements(modifiedCutOff, acceptsMessageURI);
			for (URI retractedAgreement: retractedAgreementUris) {
				System.out.println("removing agreement: " + retractedAgreement);
				result.removeNamedModel(retractedAgreement.toString());
			}
		   //	System.out.println("result after deletion:");
			//RDFDataMgr.write(System.out, result, Lang.TRIG);

	
		}
		return result;
	
		
	/*	
		return HighlevelFunctionFactory.getAcknowledgedSelection()
				.andThen(HighlevelFunctionFactory.getModifiedSelection())
				.andThen(HighlevelFunctionFactory.getAgreementFunction())
				.apply(conversationDataset);
	 */			
	}
	
	public static List<URI> getRetractedAgreements(Dataset conversationDataset, URI acceptsMessageURI) {
		// TODO Auto-generated method stub
		RDFNode name = new ResourceImpl(acceptsMessageURI.toString()); 
		QuerySolutionMap initialBinding = new QuerySolutionMap(); 
		initialBinding.add("acceptsMessageURIforProposesToCancel", name);
		Model acceptscancelledagreement = HighlevelFunctionFactory.getRetractedAgreementsFunction(initialBinding).apply(conversationDataset);
		RDFList list = acceptscancelledagreement.createList(acceptscancelledagreement.listSubjects());
		ExtendedIterator<RDFNode> listiterator = list.iterator();
		List<URI> urilist = new ArrayList<URI>();
		
		while(listiterator.hasNext()) {
		    Object object = listiterator.next();
		    try {
				URI newuri = new URI(object.toString());
				   urilist.add(newuri);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				// should I catch the error here... or throw it for a higher level function to catch??
				e.printStackTrace();
			}
		  //   System.out.println(object.toString());
	  }

		  List<URI> urilistnoduplicates = removeTheDuplicates(urilist);
		  return urilistnoduplicates;
	}

	public static Model getAgreement(Dataset conversationDataset, URI acceptsMessageURI) {
		// TODO Auto-generated method stub
		// If A is accepting proposals, add a new agreement A to the result
		// getAgreementFunction
		// This gets all agreements, but we want to cherry pick for a particular accepts message... (hence .... conversationDataset and acceptsMessageURI)
		RDFNode name = new ResourceImpl(acceptsMessageURI.toString()); 
		QuerySolutionMap initialBinding = new QuerySolutionMap(); 
		initialBinding.add("targetedacceptsmessage", name);
		Dataset agreement = HighlevelFunctionFactory.getSingleAgreementFunction(initialBinding).apply(conversationDataset);
	
		/*
		System.out.println("party duck:");
		RDFDataMgr.write(System.out, agreement, Lang.TRIG);
		System.out.println("end of the party duck:");
		
		
		System.out.println("accepts message URI:");
		System.out.println('<'+acceptsMessageURI.toString()+'>');
		System.out.println("end of the accepts message URI:");
		*/
	/*	
		Model testagreement = agreement.getNamedModel('<'+acceptsMessageURI.toString()+'>');
		System.out.println("model party duck:");
		RDFDataMgr.write(System.out, testagreement, Lang.TRIG);
	*/
		
		// insert code here to grab model from Dataset agreed
		return agreement.getNamedModel(acceptsMessageURI.toString());
	}
	
	// this needs to be tested...
	public static List<URI> getProposalSingleAgreement(Dataset conversationDataset, URI acceptsMessageURI) {
		RDFNode name = new ResourceImpl(acceptsMessageURI.toString()); 
		QuerySolutionMap initialBinding = new QuerySolutionMap(); 
		initialBinding.add("targetedacceptsmessage", name);
		Model proposalsingleagreement = HighlevelFunctionFactory.getProposalSingleAgreementFunction(initialBinding).apply(conversationDataset);
		RDFList list = proposalsingleagreement.createList(proposalsingleagreement.listSubjects());
		ExtendedIterator<RDFNode> listiterator = list.iterator();
		List<URI> urilist = new ArrayList<URI>();
		
		while(listiterator.hasNext()) {
		    Object object = listiterator.next();
		    try {
				URI newuri = new URI(object.toString());
				   urilist.add(newuri);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				// should I catch the error here... or throw it for a higher level function to catch??
				e.printStackTrace();
			}
		  //   System.out.println(object.toString());
	  }
		return urilist;
	}

   public static Dataset cutOffAfterMessage(Dataset conversationDataset, URI acceptsMessageURI) {
		// TODO Auto-generated method stub
		RDFNode name = new ResourceImpl(acceptsMessageURI.toString()); 
		QuerySolutionMap initialBinding = new QuerySolutionMap(); 
		initialBinding.add("terminatinggraph", name);
		Dataset cutOff = HighlevelFunctionFactory.getCutOffFunction(initialBinding).apply(conversationDataset);
		return cutOff;
	}

	public static List<URI> getAcceptMessages(Dataset conversationDataset) {
		// only the first accepts message for each proposal
		// only valid accepts, too
		// okay ... right now this a dumb implementation....
	
         Model actual = HighlevelFunctionFactory.getAllAcceptsFunction().apply(conversationDataset);
		  
		  RDFList list = actual.createList(actual.listSubjects());
		  
		  ExtendedIterator<RDFNode> listiterator = list.iterator();
		  
		  List<URI> urilist = new ArrayList<URI>();
		  
		  while(listiterator.hasNext()) {
			    Object object = listiterator.next();
			    try {
					URI newuri = new URI(object.toString());
					   urilist.add(newuri);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					// should I catch the error here... or throw it for a higher level function to catch??
					e.printStackTrace();
				}
			  //   System.out.println(object.toString());
		  }
		  
		  List<URI> urilistnoduplicates = removeTheDuplicates(urilist);
		  return urilistnoduplicates;
		  // what do I return here if the query fails??
	}
	
	public static List<URI> getProposalMessages(Dataset conversationDataset) {
		// only the first accepts message for each proposal
		// only valid accepts, too
		// okay ... right now this a dumb implementation....
	
         Model actual = HighlevelFunctionFactory.getAllProposalsFunction().apply(conversationDataset);
		  
		  RDFList list = actual.createList(actual.listSubjects());
		  
		  ExtendedIterator<RDFNode> listiterator = list.iterator();
		  
		  List<URI> urilist = new ArrayList<URI>();
		  
		  while(listiterator.hasNext()) {
			    Object object = listiterator.next();
			    try {
					URI newuri = new URI(object.toString());
					   urilist.add(newuri);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					// should I catch the error here... or throw it for a higher level function to catch??
					e.printStackTrace();
				}
			  //   System.out.println(object.toString());
		  }
		return urilist;
		  
	}
	
	// https://stackoverflow.com/questions/2849450/how-to-remove-duplicates-from-a-list
	// June 24, 2015 by Bade
	
		private static List<URI> removeTheDuplicates(List<URI> myList) {
		    
//			for(ListIterator<URI>iterator = myList.listIterator(); iterator.hasNext();) {
//		        URI nextURI  = iterator.next();
//		        if(Collections.frequency(myList, nextURI) > 1) {
//		            iterator.remove();
//		        }
//		    }
//		    return myList;

			Set<URI> newSet = new HashSet<>();
			newSet.addAll(myList);
			List<URI> newList = new LinkedList<>();
			newList.addAll(newSet);
			return newList;

		}
	
	/**
	 * Calculates all open proposals present in the specified conversation dataset.
	 * Returns the envelope graph of the proposal with the contents of the proposed
	 * message inside.
	 * @param conversationDataset
	 * @return
	 */
	public static Dataset getProposals(Dataset conversationDataset) {
		return HighlevelFunctionFactory.getAcknowledgedSelection()
					.andThen(HighlevelFunctionFactory.getModifiedSelection())
					.andThen(HighlevelFunctionFactory.getProposalFunction())
					.apply(conversationDataset);
	}
	
	public static Model getProposal(Dataset conversationDataset, String proposalUri) {
        return HighlevelFunctionFactory.getAcknowledgedSelection()
                .andThen(HighlevelFunctionFactory.getModifiedSelection())
                .andThen(HighlevelFunctionFactory.getAgreementFunction())
                .apply(conversationDataset)
                .getNamedModel(proposalUri);
	}	
	
	/** reveiw and rewrite the JavaDoc descriptions below **/
	
	/**
	 * Calculates all open proposals to cancel present in the specified conversation dataset.
	 * returns envelope graph of the proposaltocancel with the contents of the target agreement to 
	 * cancel inside.
	 * @param conversationDataset
	 * @return
	 */
	public static Dataset getProposalsToCancel(Dataset conversationDataset) {
		
		return HighlevelFunctionFactory.getAcknowledgedSelection()
				.andThen(HighlevelFunctionFactory.getModifiedSelection())
				.andThen(HighlevelFunctionFactory.getProposalToCancelFunction())
				.apply(conversationDataset);
	}
	
	/**
	 * Returns ?openprop agr:proposes ?openclause  .
	 * ?openprop == unaccepted propsal
	 * ?openclause == proposed clause that is unaccepted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getPendingProposes(Dataset conversationDataset) {
		Model pendingproposes  = HighlevelFunctionFactory.getPendingProposesFunction().apply(conversationDataset);
		return pendingproposes;
	}
	
	
	/**
	 * Returns ?openprop agr:proposesToCancel ?acc . 
	 * ?openprop == unaccepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getPendingProposesToCancel(Dataset conversationDataset) {
		Model pendingproposestocancel  = HighlevelFunctionFactory.getPendingProposesToCancelFunction().apply(conversationDataset);
		return pendingproposestocancel;
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause  .
	 * ?prop == accepted proposal in an agreement
	 * ?clause == accepted clause in an agreement
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptedProposes(Dataset conversationDataset) {
		Model acceptedproposes = HighlevelFunctionFactory.getAcceptedProposesFunction().apply(conversationDataset);
		return acceptedproposes;
	}
	
	/**
	 * Returns ?acc agr:accepts ?prop .
	 * ?prop == accepted proposal in an agreement
	 * ?acc == agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptsProposes(Dataset conversationDataset) {
		Model acceptsproposes = HighlevelFunctionFactory.getAcceptsProposesFunction().apply(conversationDataset);
		return acceptsproposes;
	}
	
	/**
	 * Returns ?cancelProp2 agr:proposesToCancel ?acc .
	 * ?cancelProp2 == accepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptedProposesToCancel(Dataset conversationDataset) {
		Model acceptedproposestocancel = HighlevelFunctionFactory.getAcceptedProposesToCancelFunction().apply(conversationDataset);
		return acceptedproposestocancel;
	}
	
	/**
	 * Returns ?cancelAcc2 agr:accepts ?cancelProp2 .
	 * ?cancelProp2 . == accepted proposal to cancel
	 * ?cancelAcc2 == cancallation agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptsProposesToCancel(Dataset conversationDataset) {
		Model acceptsproposestocancel = HighlevelFunctionFactory.getAcceptsProposesToCancelFunction().apply(conversationDataset);
		return acceptsproposestocancel;
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause .
	 * ?prop == accepted proposal in agreement that was cancelled
	 * ?clause == accepted clause in an agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getProposesInCancelledAgreement(Dataset conversationDataset) {
		Model  proposesincancelledagreement = HighlevelFunctionFactory.getProposesInCancelledAgreementFunction().apply(conversationDataset);
		return proposesincancelledagreement;
	}
	
	
	/**
	 * Returns ?acc agr:accepts ?prop .
	 * ?acc == agreement that was cancelled
	 * ?prop == accepted proposal in agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptsInCancelledAgreement(Dataset conversationDataset) {
		Model  acceptscancelledagreement = HighlevelFunctionFactory.getAcceptsInCancelledAgreementFunction().apply(conversationDataset);
		return acceptscancelledagreement;
	}
	
	/**
	 * Returns ?retractingMsg mod:retracts ?retractedMsg .
	 * ?retractingMsg == message containing mod:retracts
	 * ?retractedMsg == message that was retracted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptedRetracts(Dataset conversationDataset) {
		Model  acceptedretracts = HighlevelFunctionFactory.getAcceptedRetractsFunction().apply(conversationDataset);
		return acceptedretracts;
	}
	
}
