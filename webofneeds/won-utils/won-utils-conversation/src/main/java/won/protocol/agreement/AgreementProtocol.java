package won.protocol.agreement;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.DynamicDatasetToDatasetBySparqlGSPOSelectFunction;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * @deprecated replaced by {@Link won.protocol.agreement.AgreementProtocolState}
 */
@Deprecated
public class AgreementProtocol {
	private static final Logger logger = LoggerFactory.getLogger(AgreementProtocol.class);
	
	private static DynamicDatasetToDatasetBySparqlGSPOSelectFunction cutAfterMessageFunction;
	
	
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static AgreementProtocolUris getHighlevelProtocolUris(Dataset connectionDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Model getAgreement(Dataset conversationDataset, URI agreementURI) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Calculates all open proposals present in the specified conversation dataset.
	 * Returns the envelope graph of the proposal with the contents of the proposed
	 * message inside.
	 * @param conversationDataset
	 * @return
	 */
	public static Dataset getProposals(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Model getProposal(Dataset conversationDataset, String proposalUri) {
		throw new UnsupportedOperationException("not yet implemented");
	}	

	public static Set<URI> getProposalUris(Dataset conversationDataset){
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Set<URI> getAgreementUris(Dataset conversationDataset){
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Set<URI> getCancelledAgreementUris(Dataset conversationDataset){
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Set<URI> getAgreementsProposedToBeCancelledUris(Dataset conversationDataset){
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Set<URI> getRejectedProposalUris(Dataset conversationDataset){
		throw new UnsupportedOperationException("not yet implemented");
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
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?openprop agr:proposes ?openclause  .
	 * ?openprop == unaccepted propsal
	 * ?openclause == proposed clause that is unaccepted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getPendingProposes(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	
	/**
	 * Returns ?openprop agr:proposesToCancel ?acc . 
	 * ?openprop == unaccepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getPendingProposesToCancel(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause  .
	 * ?prop == accepted proposal in an agreement
	 * ?clause == accepted clause in an agreement
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptedProposes(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?acc agr:accepts ?prop .
	 * ?prop == accepted proposal in an agreement
	 * ?acc == agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptsProposes(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?cancelProp2 agr:proposesToCancel ?acc .
	 * ?cancelProp2 == accepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptedProposesToCancel(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?cancelAcc2 agr:accepts ?cancelProp2 .
	 * ?cancelProp2 . == accepted proposal to cancel
	 * ?cancelAcc2 == cancallation agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptsProposesToCancel(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause .
	 * ?prop == accepted proposal in agreement that was cancelled
	 * ?clause == accepted clause in an agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getProposesInCancelledAgreement(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	
	/**
	 * Returns ?acc agr:accepts ?prop .
	 * ?acc == agreement that was cancelled
	 * ?prop == accepted proposal in agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptsInCancelledAgreement(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	/**
	 * Returns ?retractingMsg mod:retracts ?retractedMsg .
	 * ?retractingMsg == message containing mod:retracts
	 * ?retractedMsg == message that was retracted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getAcceptedRetracts(Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	public static List<URI> getRetractedAgreements(Dataset input, URI acceptsMessageURI) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
	public static Set<URI> getRetractedUris(Dataset input){
		throw new UnsupportedOperationException("not yet implemented");
	}

	public static Dataset cutOffAfterMessage(Dataset input, URI acceptsMessageURI) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	public static List<URI> getAcceptMessages(Dataset input) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	public static List<URI> getProposalSingleAgreement(Dataset actual, URI acceptsMessageURI) {
		throw new UnsupportedOperationException("not yet implemented");
	}
	
}
