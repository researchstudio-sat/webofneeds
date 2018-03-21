package won.owner.web.rest;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.AgreementProtocolUris;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
import java.util.Set;

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
        return new ResponseEntity<AgreementProtocolUris>(AgreementProtocolState.of(conversationDataset).getAgreementProtocolUris(), HttpStatus.OK);
    }
	
	@RequestMapping(value = "/getRetractedUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRetractedUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getRetractedUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

	@RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getAgreements(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Dataset agreements = AgreementProtocolState.of(conversationDataset).getAgreements();
		return new ResponseEntity<>(agreements, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getAgreementUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

    @RequestMapping(value = "/getAgreement", method = RequestMethod.GET)
    public ResponseEntity<Model> getAgreement(String connectionUri, String agreementUri) {
        Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
        Model agreement = AgreementProtocolState.of(conversationDataset).getAgreement(URI.create(agreementUri));

        return new ResponseEntity<>(agreement, HttpStatus.OK);
    }

	@RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposals(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Dataset proposals =  AgreementProtocolState.of(conversationDataset).getPendingProposals();

		return new ResponseEntity<>(proposals, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getPendingProposalUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getProposalUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getPendingProposalUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

    @RequestMapping(value = "/getPendingProposal", method = RequestMethod.GET)
    public ResponseEntity<Model> getProposal(String connectionUri, String proposalUri) {
        Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
        Model proposal =  AgreementProtocolState.of(conversationDataset).getPendingProposal(URI.create(proposalUri));

        return new ResponseEntity<>(proposal, HttpStatus.OK);
    }

	@RequestMapping(value = "/getCancellationPendingAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementsProposedToBeCancelledUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getCancellationPendingAgreementUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getCancelledAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getCancelledAgreementUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getCancelledAreementUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getRejectedUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRejectedProposalUris(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getRejectedUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

}
