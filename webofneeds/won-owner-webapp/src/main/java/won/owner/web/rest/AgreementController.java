package won.owner.web.rest;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/rest/agreement")
public class AgreementController {

    @Autowired
    private LinkedDataSource linkedDataSource;

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    @RequestMapping(value = "/getAgreements", method = RequestMethod.GET)
    public Dataset getAgreements(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getAgreements(conversationDataset);
    }

    @RequestMapping(value = "/getProposals", method = RequestMethod.GET)
    public Dataset getProposals(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getProposals(conversationDataset);
    }
    
    @RequestMapping(value = "/getAgreementsProposedToBeCancelled", method = RequestMethod.GET)
    public Dataset getProposalsToCancel(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getProposalsToCancel(conversationDataset);
    }
    
    

    @RequestMapping(value = "/getOpenProposals", method = RequestMethod.GET)
    public Model getOpenProposes(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getOpenProposes(conversationDataset);
    }
    
    @RequestMapping(value = "/getOpenProposalsToCancelAgreements", method = RequestMethod.GET)
    public Model getOpenProposesToCancel(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getOpenProposesToCancel(conversationDataset);
    }
    
    @RequestMapping(value = "/getClosedProposals", method = RequestMethod.GET)
    public Model getClosedProposes(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getClosedProposes(conversationDataset);
    }
    
    @RequestMapping(value = "/getClosedAcceptsOfProposals", method = RequestMethod.GET)
    public Model getClosedAcceptsProposes(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getClosedAcceptsProposes(conversationDataset);
    }
    
    @RequestMapping(value = "/getClosedPropsalsToCancel", method = RequestMethod.GET)
    public Model getClosedProposesToCancel(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getClosedProposesToCancel(conversationDataset);
    }
    
    @RequestMapping(value = "/getClosedAcceptsOfPropsalsToCancel", method = RequestMethod.GET)
    public Model getClosedAcceptsProposesToCancel(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getClosedAcceptsProposesToCancel(conversationDataset);
    }

    @RequestMapping(value = "/getClosedProposalsInCancelledAgreements", method = RequestMethod.GET)
    public Model getClosedProposesInCancelledAgreement(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getClosedProposesInCancelledAgreement(conversationDataset);
    }
    
    @RequestMapping(value = "/getClosedAcceptsOfProposalsInCancelledAgreements", method = RequestMethod.GET)
    public Model getClosedAcceptsInCancelledAgreement(String connectionUri) {

        Dataset conversationDataset = retrieveConversationDataset(connectionUri);
        return HighlevelProtocols.getClosedAcceptsInCancelledAgreement(conversationDataset);
    }
    
  
    
    private Dataset retrieveConversationDataset(String connectionUri) {

        int depth = 3;  // depth 3 from connection gives us the messages in the conversation
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member", pmap));
        return linkedDataSource.getDataForResourceWithPropertyPath(
                URI.create(connectionUri), propertyPaths, maxRequests, depth, false);
    }
}
