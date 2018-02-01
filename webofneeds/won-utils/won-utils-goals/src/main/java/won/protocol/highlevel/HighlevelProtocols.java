package won.protocol.highlevel;

import org.apache.jena.query.Dataset;

import won.utils.acknowledgement.AcknowledgedSelection;
import won.utils.agreement.AgreementFunction;
import won.utils.modification.ModifiedSelection;
import won.utils.proposal.ProposalFunction;
import won.utils.proposaltocancel.ProposalToCancelFunction;
import won.utils.openproposestocancel.OpenProposesToCancelFunction;

// Add missing imports here...



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
	 * Returns ?proposal agr:proposes ?clause .
	 * ?proposal == unaccepted propsal
	 * ?clause == proposed clause
	 * @param conversationDataset
	 * @return
	 */
	public static Model getOpenProposes(Dataset conversationDataset) {
		return openproposes;
	}
	
	/**
	 * Returns ?cancellationproposal agr:proposesToCancel ?acc 
	 * ?cancellationproposal == unaccepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getOpenProposesToCancel(Dataset conversationDataset) {
		return openproposestocancel;
	}
	
	/**
	 * Returns ?proposal agr:proposes ?clause
	 * ?proposal == accepted proposal in an agreement
	 * ?clause == accepted clause in an agreement
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedProposes(Dataset conversationDataset) {
		return closednproposes;
	}
	
	/**
	 * Returns ?acc agr:accepts ?prop
	 * ?propo == accepted proposal in an agreement
	 * ?acc == agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedAcceptsProposes(Dataset conversationDataset) {
		return closedproposestocancel;
	}
	
	/**
	 * Returns ?cancellationproposal agr:proposesToCancel ?acc 
	 * ?proposal == accepted proposal to cancel
	 * ?acc == agreement proposed for cancellation
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedProposesToCancel(Dataset conversationDataset) {
		return closedproposestocancel;
	}
	
	/**
	 * Returns ?cancacc agr:accepts ?cancellationproposal 
	 * ?cancellationproposal == accepted proposal to cancel
	 * ?canacc == cancallation agreement 
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedAcceptsProposesToCancel(Dataset conversationDataset) {
		return closedacceptsproposestocancel;
	}
	
	/**
	 * Returns ?prop agr:proposes ?clause 
	 * ?prop == accepted proposal in agreement that was cancelled
	 * ?clause == accepted clause in an agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedProposesInCancelledAgreement(Dataset conversationDataset) {
		return closedproposescancelledagreement;
	}
	
	
	/**
	 * Returns ?acc agr:accepts ?prop
	 * ?acc == agreement that was cancelled
	 * ?prop == accepted proposal in agreement that was cancelled
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedAcceptsInCancelledAgreement(Dataset conversationDataset) {
		return closedacceptscancelledagreement;
	}
	
	/**
	 * Returns ?retractingMessage mod:retracts ?retractedMessage
	 * ?retractingMessage == message containing mod:retracts
	 * ?retractedMessage == message that was retracted
	 * @param conversationDataset
	 * @return
	 */
	public static Model getClosedRetracts(Dataset conversationDataset) {
		return closedretracts;
	}
	
}
