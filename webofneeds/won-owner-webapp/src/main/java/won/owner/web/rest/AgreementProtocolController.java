package won.owner.web.rest;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.AgreementProtocolUris;
import won.protocol.util.AuthenticationThreadLocal;
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
    public ResponseEntity<AgreementProtocolUris> getHighlevelProtocolUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
        return new ResponseEntity<AgreementProtocolUris>(AgreementProtocolState.of(conversationDataset).getAgreementProtocolUris(), HttpStatus.OK);
    }
	
	@RequestMapping(value = "/getRetractedUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRetractedUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getRetractedUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

	@RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getAgreements(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Dataset agreements = AgreementProtocolState.of(conversationDataset).getAgreements();
		return new ResponseEntity<>(agreements, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getAgreementUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

    @RequestMapping(value = "/getAgreement", method = RequestMethod.GET)
    public ResponseEntity<Model> getAgreement(URI connectionUri, String agreementUri) {
    	Dataset conversationDataset = getConversationDataset(connectionUri);
        Model agreement = AgreementProtocolState.of(conversationDataset).getAgreement(URI.create(agreementUri));

        return new ResponseEntity<>(agreement, HttpStatus.OK);
    }

	@RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposals(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Dataset proposals =  AgreementProtocolState.of(conversationDataset).getPendingProposals();

		return new ResponseEntity<>(proposals, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getPendingProposalUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getProposalUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getPendingProposalUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

    @RequestMapping(value = "/getPendingProposal", method = RequestMethod.GET)
    public ResponseEntity<Model> getProposal(URI connectionUri, String proposalUri) {
    	Dataset conversationDataset = getConversationDataset(connectionUri);
        Model proposal =  AgreementProtocolState.of(conversationDataset).getPendingProposal(URI.create(proposalUri));

        return new ResponseEntity<>(proposal, HttpStatus.OK);
    }

	@RequestMapping(value = "/getCancellationPendingAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementsProposedToBeCancelledUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getCancellationPendingAgreementUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getCancelledAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getCancelledAgreementUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getCancelledAreementUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/getRejectedUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRejectedProposalUris(URI connectionUri) {
		Dataset conversationDataset = getConversationDataset(connectionUri);
		Set<URI> uris = AgreementProtocolState.of(conversationDataset).getRejectedUris();
		return new ResponseEntity<>(uris, HttpStatus.OK);
	}

	
	private Dataset getConversationDataset(URI connectionUri) {
		try {
			AuthenticationThreadLocal.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
			return WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSourceOnBehalfOfNeed);	
		} finally {
			// be sure to remove the principal from the threadlocal
			AuthenticationThreadLocal.remove();
		}
	}
}
