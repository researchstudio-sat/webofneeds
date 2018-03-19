package won.owner.web.rest;

import java.net.URI;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

@Controller
@RequestMapping("/rest/highlevel")
public class HighlevelProtocolsController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private LinkedDataSource linkedDataSourceOnBehalfOfNeed;

	public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
		this.linkedDataSourceOnBehalfOfNeed = linkedDataSource;
	}

	@RequestMapping(value = "/getRetracts", method = RequestMethod.GET)
	public Model getRetracts(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			return HighlevelProtocols.getAcceptedRetracts(conversationDataset);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getRetractedUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRetractedUris(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Set<URI> uris = HighlevelProtocols.getRetractedUris(conversationDataset);
			return new ResponseEntity<>(uris, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getAgreements(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Dataset agreements = HighlevelProtocols.getAgreements(conversationDataset);

			return new ResponseEntity<>(agreements, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementUris(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Set<URI> uris = HighlevelProtocols.getAgreementUris(conversationDataset);
			return new ResponseEntity<>(uris, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getAgreement", method = RequestMethod.GET)
	public ResponseEntity<Model> getAgreement(String connectionUri, String agreementUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Model agreement = HighlevelProtocols.getAgreement(conversationDataset, URI.create(agreementUri));

			return new ResponseEntity<>(agreement, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getProposals", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposals(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Dataset proposals = HighlevelProtocols.getProposals(conversationDataset);

			return new ResponseEntity<>(proposals, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getProposalUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getProposalUris(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Set<URI> uris = HighlevelProtocols.getProposalUris(conversationDataset);
			return new ResponseEntity<>(uris, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getProposal", method = RequestMethod.GET)
	public ResponseEntity<Model> getProposal(String connectionUri, String proposalUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Model proposal = HighlevelProtocols.getProposal(conversationDataset, proposalUri);

			return new ResponseEntity<>(proposal, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getAgreementsProposedToBeCancelled", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposalsToCancel(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			return new ResponseEntity<>(HighlevelProtocols.getProposalsToCancel(conversationDataset), HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getAgreementsProposedToBeCancelledUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getAgreementsProposedToBeCancelledUris(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Set<URI> uris = HighlevelProtocols.getAgreementsProposedToBeCancelledUris(conversationDataset);
			return new ResponseEntity<>(uris, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getCancelledAgreementUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getCancelledAgreementUris(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Set<URI> uris = HighlevelProtocols.getCancelledAgreementUris(conversationDataset);
			return new ResponseEntity<>(uris, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getRejectedProposalUris", method = RequestMethod.GET)
	public ResponseEntity<Set<URI>> getRejectedProposalUris(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			Set<URI> uris = HighlevelProtocols.getRejectedProposalUris(conversationDataset);
			return new ResponseEntity<>(uris, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getOpenProposes(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			System.out.println("conversation:");
			RDFDataMgr.write(System.err, conversationDataset, Lang.TRIG);
			Model model = HighlevelProtocols.getPendingProposes(conversationDataset);
			System.out.println("pendingProposes");
			RDFDataMgr.write(System.err, model, Lang.TRIG);
			return new ResponseEntity<>(model, HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getPendingProposalsToCancelAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getOpenProposesToCancel(String connectionUri) {
		try {
			Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
					linkedDataSourceOnBehalfOfNeed);
			return new ResponseEntity<>(HighlevelProtocols.getPendingProposesToCancel(conversationDataset),
					HttpStatus.OK);
		} catch (Exception e) {
			logger.debug("caught Exception, re-throwing.", e);
			throw e;
		}
	}

	@RequestMapping(value = "/getAcceptedProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposes(String connectionUri) {
		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
				linkedDataSourceOnBehalfOfNeed);
		System.out.println("conversation:");
		RDFDataMgr.write(System.err, conversationDataset, Lang.TRIG);
		Model model = HighlevelProtocols.getAcceptedProposes(conversationDataset);
		System.out.println("pendingProposes");
		RDFDataMgr.write(System.err, model, Lang.TRIG);
		return new ResponseEntity<>(model, HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsOfProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsProposes(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
				linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptsProposes(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptedPropsalsToCancel", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposesToCancel(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
				linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptedProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsOfPropsalsToCancel", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsProposesToCancel(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
				linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptsProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getProposalsInCancelledAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposesInCancelledAgreement(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
				linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(HighlevelProtocols.getProposesInCancelledAgreement(conversationDataset),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsInCancelledAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsInCancelledAgreement(String connectionUri) {

		Dataset conversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri,
				linkedDataSourceOnBehalfOfNeed);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptsInCancelledAgreement(conversationDataset),
				HttpStatus.OK);
	}
}
