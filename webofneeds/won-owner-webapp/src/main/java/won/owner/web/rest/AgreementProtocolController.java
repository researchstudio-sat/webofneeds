package won.owner.web.rest;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.AgreementProtocolUris;
import won.protocol.agreement.effect.MessageEffect;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.WonConversationUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

@Controller
@RequestMapping("/rest/agreement")
public class AgreementProtocolController {
    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfAtom;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSourceOnBehalfOfAtom = linkedDataSource;
    }

    @RequestMapping(value = "/getAgreementProtocolUris", method = RequestMethod.GET)
    public ResponseEntity<AgreementProtocolUris> getHighlevelProtocolUris(URI connectionUri) {
        return new ResponseEntity<AgreementProtocolUris>(
                        getAgreementProtocolState(connectionUri).getAgreementProtocolUris(), HttpStatus.OK);
    }

    @RequestMapping(value = "/getAgreementProtocolDataset", method = RequestMethod.GET, produces = {
                    "application/ld+json",
                    "application/trig", "application/n-quads" })
    public ResponseEntity<Dataset> getAgreementProtocolDataset(URI connectionUri) {
        Dataset agreementProtocolDataset = getAgreementProtocolState(connectionUri)
                        .getAgreementProtocolDataset();
        return new ResponseEntity<Dataset>(agreementProtocolDataset, HttpStatus.OK);
    }

    @RequestMapping(value = "/getMessageEffects", method = RequestMethod.GET)
    public ResponseEntity<Set<MessageEffect>> getMessageEffects(URI connectionUri, URI messageUri) {
        Set<MessageEffect> uris = getAgreementProtocolState(connectionUri).getEffects(messageUri);
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getRetractedUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getRetractedUris(URI connectionUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getRetractedUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
    public ResponseEntity<Dataset> getAgreements(URI connectionUri) {
        Dataset agreements = getAgreementProtocolState(connectionUri).getAgreements();
        return new ResponseEntity<>(agreements, HttpStatus.OK);
    }

    @RequestMapping(value = "/getAgreementUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getAgreementUris(URI connectionUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getAgreementUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getAgreement", method = RequestMethod.GET)
    public ResponseEntity<Model> getAgreement(URI connectionUri, String agreementUri) {
        Model agreement = getAgreementProtocolState(connectionUri).getAgreement(URI.create(agreementUri));
        return new ResponseEntity<>(agreement, HttpStatus.OK);
    }

    @RequestMapping(value = "/getAgreedMessageUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getAgreedMessageUris(URI connectionUri, String agreementUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getAgreedMessageUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
    public ResponseEntity<Dataset> getProposals(URI connectionUri) {
        Dataset proposals = getAgreementProtocolState(connectionUri).getPendingProposals();
        return new ResponseEntity<>(proposals, HttpStatus.OK);
    }

    @RequestMapping(value = "/getPendingProposalUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getProposalUris(URI connectionUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getPendingProposalUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getPendingProposal", method = RequestMethod.GET)
    public ResponseEntity<Model> getProposal(URI connectionUri, String proposalUri) {
        Model proposal = getAgreementProtocolState(connectionUri).getPendingProposal(URI.create(proposalUri));
        return new ResponseEntity<>(proposal, HttpStatus.OK);
    }

    @RequestMapping(value = "/getCancellationPendingAgreementUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getAgreementsProposedToBeCancelledUris(URI connectionUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getCancellationPendingAgreementUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getCancelledAgreementUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getCancelledAgreementUris(URI connectionUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getCancelledAreementUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    @RequestMapping(value = "/getRejectedUris", method = RequestMethod.GET)
    public ResponseEntity<Set<URI>> getRejectedProposalUris(URI connectionUri) {
        Set<URI> uris = getAgreementProtocolState(connectionUri).getRejectedUris();
        return new ResponseEntity<>(uris, HttpStatus.OK);
    }

    private AgreementProtocolState getAgreementProtocolState(URI connectionUri) {
        try {
            AuthenticationThreadLocal.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
            return WonConversationUtils.getAgreementProtocolState(connectionUri, linkedDataSourceOnBehalfOfAtom);
        } finally {
            // be sure to remove the principal from the threadlocal
            AuthenticationThreadLocal.remove();
        }
    }
}
