package won.protocol.highlevel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.util.iterator.ExtendedIterator;

import won.protocol.util.RdfUtils;


public class HighlevelProtocols {
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {

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
		for(URI acceptsMessageURI: accepts) {
			Dataset cutOff = RdfUtils.cloneDataset(ack);
			cutOff = cutOffAfterMessage(cutOff, acceptsMessageURI);
			Dataset modifiedCutOff = HighlevelFunctionFactory.getModifiedSelection().apply(cutOff);
			
			Model agreement = getAgreement(modifiedCutOff, acceptsMessageURI);
			if (agreement != null && agreement.size() > 0) {
				result.addNamedModel(acceptsMessageURI.toString(), agreement);
			}
			
			List<URI> retractedAgreementUris = getRetractedAgreements(modifiedCutOff, acceptsMessageURI);
			for (URI retractedAgreement: retractedAgreementUris) {
				result.removeNamedModel(retractedAgreement.toString());
			}
		}
		return result;
	
		
	/*	
		return HighlevelFunctionFactory.getAcknowledgedSelection()
				.andThen(HighlevelFunctionFactory.getModifiedSelection())
				.andThen(HighlevelFunctionFactory.getAgreementFunction())
				.apply(conversationDataset);
	 */			
	}
	
	private static List<URI> getRetractedAgreements(Dataset conversationDataset, URI acceptsMessageURI) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Model getAgreement(Dataset conversationDataset, URI acceptsMessageURI) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Dataset cutOffAfterMessage(Dataset conversationDataset, URI acceptsMessageURI) {
		// TODO Auto-generated method stub
		RDFNode name = new ResourceImpl(acceptsMessageURI.toString()); 
		QuerySolutionMap initialBinding = new QuerySolutionMap(); 
		initialBinding.add("terminatinggraph", name);
		
		return null;
	}

	private static List<URI> getAcceptMessages(Dataset conversationDataset) {
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
			    System.out.println(object.toString());
		  }
		  return urilist;
		  // what do I return here if the query fails??
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
