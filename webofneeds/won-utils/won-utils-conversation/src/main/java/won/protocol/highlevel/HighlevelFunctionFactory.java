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
	// Code for testing minified version of the modification query..
	private static DatasetSelectionBySparqlFunction miniMod;
	
	public static DatasetToModelBySparqlFunction getAcceptedProposesFunction() {
		if (acceptedProposesFunction == null) {
			acceptedProposesFunction = new DatasetToModelBySparqlFunction("/acceptedproposes/query.rq");
		}
		return acceptedProposesFunction;
	}
	
	public static DatasetToModelBySparqlFunction getPendingProposesFunction() {
		if (pendingProposesFunction == null) {
			pendingProposesFunction = new DatasetToModelBySparqlFunction("/pendingproposes/query.rq");
		}
		return pendingProposesFunction;
	}
	
	public static DatasetToModelBySparqlFunction getPendingProposesToCancelFunction() {
		if (pendingProposesToCancelFunction == null) {
			pendingProposesToCancelFunction = new DatasetToModelBySparqlFunction("/pendingproposestocancel/query.rq");
		}
		return pendingProposesToCancelFunction;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptsProposesFunction() {
		if (acceptsProposesFunction == null) {
			acceptsProposesFunction = new DatasetToModelBySparqlFunction("/acceptsproposal/query.rq");
		}
		return acceptsProposesFunction;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptedProposesToCancelFunction() {
		if (acceptedProposesToCancelFunction == null) {
			acceptedProposesToCancelFunction = new DatasetToModelBySparqlFunction("/acceptedproposestocancel/query.rq");
		}
		return acceptedProposesToCancelFunction;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptsProposesToCancelFunction() {
		if (acceptsProposesToCancelFunction == null) {
			acceptsProposesToCancelFunction = new DatasetToModelBySparqlFunction("/acceptsproposestocancel/query.rq");
		}
		return acceptsProposesToCancelFunction;
	}
	
	public static DatasetToModelBySparqlFunction getProposesInCancelledAgreementFunction() {
		if (proposesInCancelledAgreement == null) {
			proposesInCancelledAgreement = new DatasetToModelBySparqlFunction("/proposescancelledagreement/query.rq");
		}
		return proposesInCancelledAgreement;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptsInCancelledAgreementFunction() {
		if (acceptsCancelledAgreement == null) {
			acceptsCancelledAgreement = new DatasetToModelBySparqlFunction("/acceptscancelledagreement/query.rq");
		}
		return acceptsCancelledAgreement;
	}
	
	public static DatasetToModelBySparqlFunction getAcceptedRetractsFunction() {
		if (acceptedRetracts == null) {
			acceptedRetracts = new DatasetToModelBySparqlFunction("/acceptedretracts/query.rq");
		}
		return acceptedRetracts;
	}
	
	
	// the original class name was AcknowledgedSelection
	public static DatasetSelectionBySparqlFunction getAcknowledgedSelection() {
		if (acknowledgedSelection == null) {
			acknowledgedSelection = new DatasetSelectionBySparqlFunction("/acknowledgement/query.rq");
		}
		return acknowledgedSelection;
	}
	
	public static DatasetSelectionBySparqlFunction getModifiedSelection() {
		if (modifiedSelection == null) {
			modifiedSelection = new DatasetSelectionBySparqlFunction("/modification/query.rq");
		}
		return modifiedSelection;
	}
	
// Insert some special code here for modifications ...
	
	// The original class name was ProposalToCancelFunction
	public static DatasetToDatasetBySparqlGSPOSelectFunction getProposalToCancelFunction() {
		if (proposalToCancelFunction == null) {
			proposalToCancelFunction = new DatasetToDatasetBySparqlGSPOSelectFunction("/proposaltocancel/query.rq");
		}
		return proposalToCancelFunction;
	}
	
	public static DatasetToDatasetBySparqlGSPOSelectFunction getProposalFunction() {
		if (proposalFunction == null) {
			proposalFunction = new DatasetToDatasetBySparqlGSPOSelectFunction("/proposal/query.rq");
		}
		return proposalFunction;
	}
	
	public static DatasetToDatasetBySparqlGSPOSelectFunction getAgreementFunction() {
		if (agreementFunction == null) {
			agreementFunction = new DatasetToDatasetBySparqlGSPOSelectFunction("/agreement/query.rq");
		}
		return agreementFunction;
	}
}
