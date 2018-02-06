package won.protocol.highlevel;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.utils.acceptedproposestocancel.AcceptedProposesToCancelFunction;
import won.utils.acceptedretracts.AcceptedRetractsFunction;
import won.utils.acceptscancelledagreement.AcceptsCancelledAgreementFunction;
import won.utils.acknowledgement.AcknowledgedSelection;
import won.utils.agreement.AgreementFunction;
import won.utils.modification.ModifiedSelection;
import won.utils.proposal.ProposalFunction;
import won.utils.proposaltocancel.ProposalToCancelFunction;
import won.utils.pendingproposes.PendingProposesFunction;
import won.utils.pendingproposestocancel.PendingProposesToCancelFunction;
import won.utils.closedproposescancelledagreement.ClosedProposesCancelledAgreementFunction;
import won.utils.closedproposes.ClosedProposesFunction;
import won.utils.closedacceptsproposestocancel.ClosedAcceptsProposesToCancelFunction;
import won.utils.closedacceptsproposal.ClosedAcceptsProposalFunction;


public class HighlevelProtocols {
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {
		AcknowledgedSelection acknowledgedSelection = new AcknowledgedSelection();
        ModifiedSelection modifiedSelection = new ModifiedSelection();
        AgreementFunction agreementFunction = new AgreementFunction();

		Dataset acknowledged = acknowledgedSelection.applyAcknowledgedSelection(conversationDataset);
        Dataset modified = modifiedSelection.applyModificationSelection(acknowledged);
        Dataset agreed = agreementFunction.applyAgreementFunction(modified);
		
		return agreed;
	}
	
	/**
	 * Calculates all open proposals present in the specified conversation dataset.
	 * Returns the envelope graph of the proposal with the contents of the proposed
	 * message inside.
	 * @param conversationDataset
	 * @return
	 */
	public static Dataset getProposals(Dataset conversationDataset) {
        AcknowledgedSelection acknowledgedSelection = new AcknowledgedSelection();
        ModifiedSelection modifiedSelection = new ModifiedSelection();
        ProposalFunction proposalFunction = new ProposalFunction();

        Dataset acknowledged = acknowledgedSelection.applyAcknowledgedSelection(conversationDataset);
        Dataset modified = modifiedSelection.applyModificationSelection(acknowledged);
        Dataset proposed = proposalFunction.applyProposalFunction(modified);

        return proposed;
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
		AcknowledgedSelection acknowledgedSelection = new AcknowledgedSelection();
        ModifiedSelection modifiedSelection = new ModifiedSelection();
        ProposalToCancelFunction proposalToCancelFunction = new ProposalToCancelFunction();
        
        Dataset acknowledged = acknowledgedSelection.applyAcknowledgedSelection(conversationDataset);
        Dataset modified = modifiedSelection.applyModificationSelection(acknowledged);
        Dataset proposedtocancel = proposalToCancelFunction.applyProposalToCancelFunction(modified);
        
		return proposedtocancel;
	}
	
	/**
	 * Returns ?openprop agr:proposes ?openclause  .
	 * ?openprop == unaccepted propsal
	 * ?openclause == proposed clause that is unaccepted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getOpenProposes(Dataset conversationDataset) {
		Model openproposes = PendingProposesFunction.sparqlTest(conversationDataset);
		return openproposes;
	}
	
	/**
	 * Returns ?openprop agr:proposesToCancel ?acc . 
	 * ?openprop == unaccepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getOpenProposesToCancel(Dataset conversationDataset) {
		Model openproposestocancel = PendingProposesToCancelFunction.sparqlTest(conversationDataset);
		return openproposestocancel;
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause  .
	 * ?prop == accepted proposal in an agreement
	 * ?clause == accepted clause in an agreement
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedProposes(Dataset conversationDataset) {
		Model closedproposes = ClosedProposesFunction.sparqlTest(conversationDataset);
		return closedproposes;
	}
	
	/**
	 * Returns ?acc agr:accepts ?prop .
	 * ?prop == accepted proposal in an agreement
	 * ?acc == agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedAcceptsProposes(Dataset conversationDataset) {
		Model closedacceptsproposes = ClosedAcceptsProposalFunction.sparqlTest(conversationDataset);
		return closedacceptsproposes;
	}
	
	/**
	 * Returns ?cancelProp2 agr:proposesToCancel ?acc .
	 * ?cancelProp2 == accepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedProposesToCancel(Dataset conversationDataset) {
		Model closedproposestocancel = AcceptedProposesToCancelFunction.sparqlTest(conversationDataset);
		return closedproposestocancel;
	}
	
	/**
	 * Returns ?cancelAcc2 agr:accepts ?cancelProp2 .
	 * ?cancelProp2 . == accepted proposal to cancel
	 * ?cancelAcc2 == cancallation agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedAcceptsProposesToCancel(Dataset conversationDataset) {
		Model closedacceptsproposestocancel = ClosedAcceptsProposesToCancelFunction.sparqlTest(conversationDataset);
		return closedacceptsproposestocancel;
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause .
	 * ?prop == accepted proposal in agreement that was cancelled
	 * ?clause == accepted clause in an agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedProposesInCancelledAgreement(Dataset conversationDataset) {
		Model closedproposescancelledagreement = ClosedProposesCancelledAgreementFunction.sparqlTest(conversationDataset);
		return closedproposescancelledagreement;
	}
	
	
	/**
	 * Returns ?acc agr:accepts ?prop .
	 * ?acc == agreement that was cancelled
	 * ?prop == accepted proposal in agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedAcceptsInCancelledAgreement(Dataset conversationDataset) {
		Model closedacceptscancelledagreement = AcceptsCancelledAgreementFunction.sparqlTest(conversationDataset);
		return closedacceptscancelledagreement;
	}
	
	/**
	 * Returns ?retractingMsg mod:retracts ?retractedMsg .
	 * ?retractingMsg == message containing mod:retracts
	 * ?retractedMsg == message that was retracted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedRetracts(Dataset conversationDataset) {
		Model closedretracts = AcceptedRetractsFunction.sparqlTest(conversationDataset);
		return closedretracts;
	}
	
}
