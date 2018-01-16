package won.protocol.highlevel;

import org.apache.jena.query.Dataset;


public class HighlevelProtocols {
	/**
	 * Calculates all agreements present in the specified conversation dataset.
	 */
	public static Dataset getAgreements(Dataset conversationDataset) {
		//TODO: use AcknowledgedSelection, ModifiedSelection, and AgreementFunction 
		//to calculate agrements
		return null;
	}
	
	/**
	 * Calculates all open proposals present in the specified conversation dataset.
	 * @param conversationDataset
	 * @return
	 */
	public static Dataset getProposals(Dataset conversationDataset) {
		//TODO: use AcknowledgedSelection, ModifiedSelection, and a (yet to be created ProposalFunction)
		//to calculate all open proposals.
		return null;
	}
	
	
}
