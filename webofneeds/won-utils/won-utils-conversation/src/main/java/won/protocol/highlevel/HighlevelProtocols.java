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
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.util.iterator.ExtendedIterator;

import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.util.DynamicDatasetToDatasetBySparqlGSPOSelectFunction;
import won.protocol.util.RdfUtils;
import won.protocol.util.SparqlSelectFunction;


public class HighlevelProtocols {
	
	private static DynamicDatasetToDatasetBySparqlGSPOSelectFunction cutAfterMessageFunction;
	
	
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {
		
		//Dataset ack = HighlevelFunctionFactory.getAcknowledgedSelection().apply(conversationDataset);
		
		Map<URI, ConversationMessage> messagesByURI = new HashMap<>();
		SparqlSelectFunction<ConversationMessage> selectfunction = 
				new SparqlSelectFunction<>("/conversation/messagesForHighlevelProtocols.rq", new ConversationResultMapper(messagesByURI))
				.addOrderBy("msg", Query.ORDER_ASCENDING);
		selectfunction.apply(conversationDataset);
		
		Set<ConversationMessage> roots = new HashSet();
		Collection<ConversationMessage> messages = messagesByURI.values();
		
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
		
		 
		System.out.println("messages:");
		messages.forEach(m -> System.out.println(m));
		//apply acknowledgment protocol to whole conversation first:
		Dataset conversation = acknowledgedSelection(conversationDataset, messages);
		
		//on top of this, apply modification and agreement protocol on a per-message basis, starting with the root(s)
		
		Dataset proposals = DatasetFactory.createGeneral();
		Dataset agreements = DatasetFactory.createGeneral();
		Collection<ConversationMessage> currentMessages = new HashSet<>();
		
		currentMessages.addAll(roots);
		PriorityQueue<ConversationMessage> unprocessedMessages = 
				new PriorityQueue<ConversationMessage>(
						new Comparator<ConversationMessage>() {
							@Override
							public int compare(ConversationMessage o1, ConversationMessage o2) {
								//TODO continue here
								return 0;
							}
						});
		
		
		//TODO: we need to use a priority queue for the messages, which is 
		//sorted by temporal ordering. Each time we process a message, we 
		//add the subsequent ones to the queue, the retrieve the 
		//oldest from the queue for the next iteration.

		do {
			final Set<ConversationMessage> nextMessages = new HashSet<>();
			currentMessages.forEach(msg -> {
				if (msg.isRetractsMessage()) {
					System.out.println("retracting message:" + msg.getMessageURI());
					removeContentGraphs(conversation, msg);
					msg.getRetractsRefs()
						.stream()
						.filter(other -> other.getSenderNeedURI().equals(msg.getSenderNeedURI()))
						.forEach(toRetract -> removeContentGraphs(conversation, toRetract));
				}
				if (msg.isProposesMessage()) {
					Model proposalContent = ModelFactory.createDefaultModel();
					msg.getProposesRefs().forEach(clause -> {
						proposalContent.add(aggregateGraphs(conversation, clause.getContentGraphs()));
					});
					System.out.println("proposal: "+ msg.getMessageURI() );
					proposals.addNamedModel(msg.getMessageURI().toString(), proposalContent);
				}
				if (msg.isAcceptsMessage()) {
					msg.getAcceptsRefs()
						.stream()
						.filter(accepted -> ! accepted.getSenderNeedURI().equals(msg.getSenderNeedURI()))
						.forEach(accepted -> {
							if (accepted.isProposesMessage()) {
								acceptProposal(accepted.getMessageURI(),msg.getMessageURI(), proposals, agreements);
							}
							accepted.getProposesToCancel().forEach(toCancel -> {
								cancelAgreement(toCancel, agreements);
							});
					});
				}
				nextMessages.addAll(msg.getPreviousInverseRefs());
			});
			currentMessages = nextMessages;
		} while (!currentMessages.isEmpty());
		return agreements;
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
		System.out.println("retracting content from : " + message.getMessageURI());
		conversationDataset.begin(ReadWrite.WRITE);
		message.getContentGraphs().stream().forEach(uri -> conversationDataset.removeNamedModel(uri.toString()));
		conversationDataset.commit();
	}
	
	private static Model aggregateGraphs(Dataset conversationDataset, Collection<URI> graphURIs) {
		Model result = ModelFactory.createDefaultModel();
		graphURIs.forEach(uri -> {
			Model graph = conversationDataset.getNamedModel(uri.toString());
			if (graph != null) {
				result.add(RdfUtils.cloneModel(graph));
			}
		});
		return result;
	}
	
	private static void acceptProposal(URI proposalUri, URI agreementUri, Dataset proposals, Dataset agreements) {
		System.out.println("accepting: " + proposalUri + " as " + agreementUri);
		proposals.begin(ReadWrite.WRITE);
		agreements.begin(ReadWrite.WRITE);
		Model proposal = proposals.getNamedModel(proposalUri.toString());
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
	
	private static void cancelAgreement(URI toCancel, Dataset agreements) {
		agreements.removeNamedModel(toCancel.toString());
	}
	
	private static class ConversationResultMapper implements Function<QuerySolution, ConversationMessage>{
		
		private Map<URI, ConversationMessage> knownMessages = null;
		
		public ConversationResultMapper(Map messages) {
			this.knownMessages = messages;
		}
		
		public Map<URI, ConversationMessage> getKnownMessages(){
			return this.knownMessages;
		}
		
		@Override
		public ConversationMessage apply(QuerySolution solution) {
			URI messageUri = getUriVar(solution, "msg");
			System.out.println(solution);
			ConversationMessage ret = knownMessages.get(messageUri);
			if (ret == null) {
				ret = new ConversationMessage(messageUri);
			}
			URI senderNeedUri = getUriVar(solution, "senderNeed");
			if (senderNeedUri != null) {
				ret.setSenderNeedURI(senderNeedUri);
			}
			URI previous = getUriVar(solution, "previous");
			if (previous != null) {
				ret.addPrevious(previous);
			}
			URI retracts = getUriVar(solution, "retracts");
			if (retracts != null) {
				ret.addRetracts(retracts);
			}
			URI proposes = getUriVar(solution, "proposes");
			if (proposes != null) {
				ret.addProposes(proposes);
			}
			URI proposesToCancel = getUriVar(solution, "proposesToCancel");
			if (proposesToCancel != null) {
				ret.addProposesToCancel(proposesToCancel);
			}
			URI accepts = getUriVar(solution, "accepts");
			if (accepts != null){
				ret.addAccepts(accepts);
			}
			URI correspondingRemoteMessage = getUriVar(solution, "correspondingRemoteMessage");
			if (correspondingRemoteMessage != null) {
				ret.setCorrespondingRemoteMessageURI(correspondingRemoteMessage);
			}
			URI isResponseTo = getUriVar(solution, "isResponseTo");
			if (isResponseTo != null) {
				ret.setIsResponseTo(isResponseTo);
			}
			URI isRemoteResponseTo = getUriVar(solution, "isRemoteResponseTo");
			if (isRemoteResponseTo != null) {
				ret.setIsRemoteResponseTo(isRemoteResponseTo);
			}			
			URI type = getUriVar(solution, "msgType");
			if (type != null) {
				ret.setMessageType(WonMessageType.getWonMessageType(type));
			}
			URI direction = getUriVar(solution, "direction");
			if (direction != null) {
				ret.setDirection(WonMessageDirection.getWonMessageDirection(direction));
			}
			URI contentGraph = getUriVar(solution, "contentGraph");
			if (contentGraph != null) {
				ret.addContentGraph(contentGraph);
			}
			this.knownMessages.put(messageUri, ret);
			return ret;
		}
		
		private URI getUriVar(QuerySolution solution, String name) {
			 RDFNode node = solution.get(name);
			 if (node == null) {
				 return null;
			 }
			 return URI.create(node.asResource().getURI());
		}
	}
	
	/**
	 * @author bshamb
	 *
	 */
	private static class ConversationMessage {
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
		
		public ConversationMessage(URI messageURI) {
			this.messageURI = messageURI;
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
			return "ConversationMessage [messageURI=" + messageURI + ", senderNeedURI=" + senderNeedURI
					+ ", direction=" + direction
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
