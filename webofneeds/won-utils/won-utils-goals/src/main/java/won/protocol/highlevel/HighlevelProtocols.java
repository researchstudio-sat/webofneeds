package won.protocol.highlevel;

import org.apache.jena.query.Dataset;

import won.utils.acknowledgement.AcknowledgedSelection;
import won.utils.agreement.AgreementFunction;
import won.utils.modification.ModifiedSelection;
import won.utils.proposal.ProposalFunction;


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
}
