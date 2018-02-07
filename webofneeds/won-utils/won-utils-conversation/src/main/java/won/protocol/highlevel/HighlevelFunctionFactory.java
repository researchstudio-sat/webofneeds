package won.protocol.highlevel;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;

import won.protocol.util.DatasetSelectionBySparqlFunction;
import won.protocol.util.DatasetToDatasetBySparqlGSPOSelectFunction;
import won.protocol.util.DatasetToModelBySparqlFunction;

public class HighlevelFunctionFactory {
	
	private static DatasetToModelBySparqlFunction acceptedProposesFunction;
	private static DatasetSelectionBySparqlFunction acknowledgedSelection;
	private static DatasetToDatasetBySparqlGSPOSelectFunction proposalToCancelFunction;
	private static DatasetToDatasetBySparqlGSPOSelectFunction proposalFunction;
	private static DatasetToDatasetBySparqlGSPOSelectFunction agreementFunction;
	private static DatasetToModelBySparqlFunction pendingProposesFunction;
	private static DatasetToModelBySparqlFunction pendingProposesToCancelFunction;
	private static DatasetToModelBySparqlFunction acceptsProposesFunction;
	private static DatasetToModelBySparqlFunction acceptedProposesToCancelFunction;
	private static DatasetToModelBySparqlFunction acceptsProposesToCancelFunction;
	private static DatasetToModelBySparqlFunction proposesInCancelledAgreement;
	private static DatasetToModelBySparqlFunction acceptsCancelledAgreement;
	private static DatasetToModelBySparqlFunction acceptedRetracts;
	private static DatasetSelectionBySparqlFunction modifiedSelection;
	
	public static DatasetToModelBySparqlFunction getAcceptedProposesFunction() {
		if (acceptedProposesFunction == null) {
			acceptedProposesFunction = new DatasetToModelBySparqlFunction("/acceptedproposes/query.sq");
		}
		return acceptedProposesFunction;
	}
	
	public static DatasetToModelBySparqlFunction getPendingProposesFunction() {
		if (pendingProposesFunction == null) {
			pendingProposesFunction = new DatasetToModelBySparqlFunction("/pendingproposes/query.sq");
		}
		return pendingProposesFunction;
	}
	
	public static DatasetToModelBySparqlFunction getPendingProposesToCancelFunction() {
		if (pendingProposesToCancelFunction == null) {
			pendingProposesToCancelFunction = new DatasetToModelBySparqlFunction("/pendingproposestocancel/query.sq");
		}
		return pendingProposesToCancelFunction;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptsProposesFunction() {
		if (acceptsProposesFunction == null) {
			acceptsProposesFunction = new DatasetToModelBySparqlFunction("/acceptsproposal/query.sq");
		}
		return acceptsProposesFunction;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptedProposesToCancelFunction() {
		if (acceptedProposesToCancelFunction == null) {
			acceptedProposesToCancelFunction = new DatasetToModelBySparqlFunction("/acceptedproposestocancel/query.sq");
		}
		return acceptedProposesToCancelFunction;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptsProposesToCancelFunction() {
		if (acceptsProposesToCancelFunction == null) {
			acceptsProposesToCancelFunction = new DatasetToModelBySparqlFunction("/acceptsproposestocancel/query.sq");
		}
		return acceptsProposesToCancelFunction;
	}
	
	public static DatasetToModelBySparqlFunction getProposesInCancelledAgreementFunction() {
		if (proposesInCancelledAgreement == null) {
			proposesInCancelledAgreement = new DatasetToModelBySparqlFunction("/proposescancelledagreement/query.sq");
		}
		return proposesInCancelledAgreement;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptsInCancelledAgreementFunction() {
		if (acceptsCancelledAgreement == null) {
			acceptsCancelledAgreement = new DatasetToModelBySparqlFunction("/acceptscancelledagreement/query.sq");
		}
		return acceptsCancelledAgreement;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptedRetractsFunction() {
		if (acceptedRetracts == null) {
			acceptedRetracts = new DatasetToModelBySparqlFunction("/acceptedretracts/query.sq");
		}
		return acceptedRetracts;
	}
	
	
	// the original class name was AcknowledgedSelection
	public static DatasetSelectionBySparqlFunction getAcknowledgedSelection() {
		if (acknowledgedSelection == null) {
			acknowledgedSelection = new DatasetSelectionBySparqlFunction("/acknowledgement/query.sq");
		}
		return acknowledgedSelection;
	}
	
	public static DatasetSelectionBySparqlFunction getModifiedSelection() {
		if (modifiedSelection == null) {
			modifiedSelection = new DatasetSelectionBySparqlFunction("/modification/query.sq");
		}
		return modifiedSelection;
	}
	
// Insert some special code here for modifications ...
	
	// The original class name was ProposalToCancelFunction
	public static DatasetToDatasetBySparqlGSPOSelectFunction getProposalToCancelFunction() {
		if (proposalToCancelFunction == null) {
			proposalToCancelFunction = new DatasetToDatasetBySparqlGSPOSelectFunction("/proposaltocancel/query.sq");
		}
		return proposalToCancelFunction;
	}
	
	public static DatasetToDatasetBySparqlGSPOSelectFunction getProposalFunction() {
		if (proposalFunction == null) {
			proposalFunction = new DatasetToDatasetBySparqlGSPOSelectFunction("/proposal/query.sq");
		}
		return proposalFunction;
	}
	
	public static DatasetToDatasetBySparqlGSPOSelectFunction getAgreementFunction() {
		if (agreementFunction == null) {
			agreementFunction = new DatasetToDatasetBySparqlGSPOSelectFunction("/agreement/query.sq");
		}
		return agreementFunction;
	}
}
