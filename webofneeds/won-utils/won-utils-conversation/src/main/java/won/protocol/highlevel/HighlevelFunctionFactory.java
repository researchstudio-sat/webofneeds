package won.protocol.highlevel;

import won.protocol.util.DatasetSelectionBySparqlFunction;
import won.protocol.util.DatasetToDatasetBySparqlGSPOSelectFunction;
import won.protocol.util.DatasetToModelBySparqlFunction;

public class HighlevelFunctionFactory {
	
	private static DatasetToModelBySparqlFunction acceptedProposesFunction;
	private static DatasetSelectionBySparqlFunction acknowledgedSelection;
	private static DatasetToDatasetBySparqlGSPOSelectFunction proposalToCancelFuncion;
	
	public static DatasetToModelBySparqlFunction getAcceptedProposesFunction() {
		if (acceptedProposesFunction == null) {
			acceptedProposesFunction = new DatasetToModelBySparqlFunction("/acceptedproposes/query.sq");
		}
		return acceptedProposesFunction;
	}
	
	public static DatasetSelectionBySparqlFunction getAcknowledgedSelection() {
		if (acknowledgedSelection == null) {
			acknowledgedSelection = new DatasetSelectionBySparqlFunction("/acknowledgement/query.sq");
		}
		return acknowledgedSelection;
	}
	
	public static DatasetToDatasetBySparqlGSPOSelectFunction getProposalToCancelFunction() {
		if (proposalToCancelFuncion == null) {
			proposalToCancelFuncion = new DatasetToDatasetBySparqlGSPOSelectFunction("/proposaltocancel/query.sq");
		}
		return proposalToCancelFuncion;
	}
	
}
