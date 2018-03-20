package won.owner.web.rest;

import java.net.URI;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import won.protocol.highlevel.AgreementProtocolUris;
import won.protocol.highlevel.AgreementProtocol;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Controller
@RequestMapping("/rest/agreement")
public class AgreementProtocolController {

	@Autowired
	private LinkedDataSource linkedDataSourceOnBehalfOfNeed;

	public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
		this.linkedDataSourceOnBehalfOfNeed = linkedDataSource;
	}
	
	@RequestMapping(value = "/getAgreementProtocolUris", method = RequestMethod.GET)
    public ResponseEntity<AgreementProtocolUris> getHighlevelProtocolUris(String connectionUri) {
        Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
        return new ResponseEntity<AgreementProtocolUris>(AgreementProtocol.getHighlevelProtocolUris(conversationDataset), HttpStatus.OK);
    }
	
	@RequestMapping(value = "/getRetractedUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRetractedUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocol.getRetractedUris(conversationDataset);
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

	@RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getAgreements(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Dataset agreements = AgreementProtocol.getAgreements(conversationDataset);
		return new ResponseEntity<>(agreements, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocol.getAgreementUris(conversationDataset);
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

    @RequestMapping(value = "/getAgreement", method = RequestMethod.GET)
    public ResponseEntity<Model> getAgreement(String connectionUri, String agreementUri) {
        Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
        Model agreement = AgreementProtocol.getAgreement(conversationDataset, URI.create(agreementUri));

        return new ResponseEntity<>(agreement, HttpStatus.OK);
    }

	@RequestMapping(value = "/getProposals", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposals(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Dataset proposals =  AgreementProtocol.getProposals(conversationDataset);

		return new ResponseEntity<>(proposals, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getProposalUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getProposalUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocol.getProposalUris(conversationDataset);
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

    @RequestMapping(value = "/getProposal", method = RequestMethod.GET)
    public ResponseEntity<Model> getProposal(String connectionUri, String proposalUri) {
        Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
        Model proposal =  AgreementProtocol.getProposal(conversationDataset, proposalUri);

        return new ResponseEntity<>(proposal, HttpStatus.OK);
    }

	@RequestMapping(value = "/getAgreementsProposedToBeCancelled", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposalsToCancel(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getProposalsToCancel(conversationDataset), HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getAgreementsProposedToBeCancelledUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementsProposedToBeCancelledUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocol.getAgreementsProposedToBeCancelledUris(conversationDataset);
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getCancelledAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getCancelledAgreementUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocol.getCancelledAgreementUris(conversationDataset);
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getRejectedProposalUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRejectedProposalUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocol.getRejectedProposalUris(conversationDataset);
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

	@RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getOpenProposes(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		System.out.println("conversation:");
		RDFDataMgr.write(System.err, conversationDataset, Lang.TRIG);
		Model model = AgreementProtocol.getPendingProposes(conversationDataset);
		System.out.println("pendingProposes");
		RDFDataMgr.write(System.err, model, Lang.TRIG);
		return new ResponseEntity<>(model, HttpStatus.OK);
	}

	@RequestMapping(value = "/getPendingProposalsToCancelAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getOpenProposesToCancel(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getPendingProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptedProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposes(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		System.out.println("conversation:");
		RDFDataMgr.write(System.err, conversationDataset, Lang.TRIG);
		Model model = AgreementProtocol.getAcceptedProposes(conversationDataset);
		System.out.println("pendingProposes");
		RDFDataMgr.write(System.err, model, Lang.TRIG);
		return new ResponseEntity<>(model, HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsOfProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsProposes(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getAcceptsProposes(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptedPropsalsToCancel", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposesToCancel(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getAcceptedProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsOfPropsalsToCancel", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsProposesToCancel(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getAcceptsProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getProposalsInCancelledAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposesInCancelledAgreement(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getProposesInCancelledAgreement(conversationDataset),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsInCancelledAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsInCancelledAgreement(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(AgreementProtocol.getAcceptsInCancelledAgreement(conversationDataset),
				HttpStatus.OK);
	}
}
