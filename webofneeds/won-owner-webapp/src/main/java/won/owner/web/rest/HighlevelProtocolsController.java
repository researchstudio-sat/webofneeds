package won.owner.web.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

@Controller
@RequestMapping("/rest/agreement")
public class AgreementController {

	@Autowired
	private LinkedDataSource linkedDataSourceOnBehalfOfNeed;

	public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
		this.linkedDataSourceOnBehalfOfNeed = linkedDataSource;
	}

	@RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getAgreements(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		Dataset agreements = HighlevelProtocols.getAgreements(conversationDataset);
		return new ResponseEntity<>(agreements, HttpStatus.OK);
	}

	@RequestMapping(value = "/getProposals", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposals(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getProposals(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAgreementsProposedToBeCancelled", method = RequestMethod.GET)
	public ResponseEntity<Dataset> getProposalsToCancel(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getProposalsToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getOpenProposes(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getPendingProposes(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getPendingProposalsToCancelAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getOpenProposesToCancel(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getPendingProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptedProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposes(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptedProposes(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsOfProposals", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsProposes(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptsProposes(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptedPropsalsToCancel", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposesToCancel(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptedProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsOfPropsalsToCancel", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsProposesToCancel(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptsProposesToCancel(conversationDataset), HttpStatus.OK);
	}

	@RequestMapping(value = "/getProposalsInCancelledAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedProposesInCancelledAgreement(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getProposesInCancelledAgreement(conversationDataset),
				HttpStatus.OK);
	}

	@RequestMapping(value = "/getAcceptsInCancelledAgreements", method = RequestMethod.GET)
	public ResponseEntity<Model> getClosedAcceptsInCancelledAgreement(String connectionUri) {

		Dataset conversationDataset = retrieveConversationDataset(connectionUri);
		return new ResponseEntity<>(HighlevelProtocols.getAcceptsInCancelledAgreement(conversationDataset),
				HttpStatus.OK);
	}

	private Dataset retrieveConversationDataset(String connectionUri) {
		int depth = 3; // depth 3 from connection gives us the messages in the conversation
		int maxRequests = 1000;
		List<Path> propertyPaths = new ArrayList<>();
		PrefixMapping pmap = new PrefixMappingImpl();
		pmap.withDefaultMappings(PrefixMapping.Standard);
		pmap.setNsPrefix("won", WON.getURI());
		pmap.setNsPrefix("msg", WONMSG.getURI());
		propertyPaths.add(PathParser.parse("won:hasEventContainer", pmap));
		propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member", pmap));
		propertyPaths.add(PathParser.parse("won:belongsToNeed", pmap));
		propertyPaths.add(PathParser.parse("won:belongsToNeed/won:hasEventContainer", pmap));
		propertyPaths.add(PathParser.parse("won:belongsToNeed/won:hasEventContainer/rdfs:member", pmap));
		propertyPaths.add(PathParser.parse("won:hasRemoteNeed", pmap));
		propertyPaths.add(PathParser.parse("won:hasRemoteNeed/won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteNeed/won:hasEventContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteConnection", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer/rdfs:member", pmap));
		URI requesterWebId = WonLinkedDataUtils.getNeedURIforConnectionURI(URI.create(connectionUri),
				linkedDataSourceOnBehalfOfNeed);
		return linkedDataSourceOnBehalfOfNeed.getDataForResourceWithPropertyPath(URI.create(connectionUri),
				requesterWebId, propertyPaths, maxRequests, depth, false);
	}
}
