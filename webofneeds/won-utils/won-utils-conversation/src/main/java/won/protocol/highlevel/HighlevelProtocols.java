package won.protocol.highlevel;

import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.protocol.util.DatasetSelectionBySparqlFunction;


public class HighlevelProtocols {
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {

		return HighlevelFunctionFactory.getAcknowledgedSelection()
				.andThen(HighlevelFunctionFactory.getModifiedSelection())
				.andThen(HighlevelFunctionFactory.getAgreementFunction())
				.apply(conversationDataset);
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
