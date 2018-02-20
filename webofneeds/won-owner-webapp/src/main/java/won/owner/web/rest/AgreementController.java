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
import won.protocol.util.linkeddata.WonLinkedDataUtils;
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

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAgreements(conversationDataset);
    }

    @RequestMapping(value = "/getProposals", method = RequestMethod.GET)
    public Dataset getProposals(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getProposals(conversationDataset);
    }
    
    @RequestMapping(value = "/getAgreementsProposedToBeCancelled", method = RequestMethod.GET)
    public Dataset getProposalsToCancel(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getProposalsToCancel(conversationDataset);
    }
    
    

    @RequestMapping(value = "/getPendingProposals", method = RequestMethod.GET)
    public Model getOpenProposes(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getPendingProposes(conversationDataset);
    }
    
    @RequestMapping(value = "/getPendingProposalsToCancelAgreements", method = RequestMethod.GET)
    public Model getOpenProposesToCancel(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getPendingProposesToCancel(conversationDataset);
    }
    
    @RequestMapping(value = "/getAcceptedProposals", method = RequestMethod.GET)
    public Model getClosedProposes(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAcceptedProposes(conversationDataset);
    }
    
    @RequestMapping(value = "/getAcceptsOfProposals", method = RequestMethod.GET)
    public Model getClosedAcceptsProposes(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAcceptsProposes(conversationDataset);
    }
    
    @RequestMapping(value = "/getAcceptedPropsalsToCancel", method = RequestMethod.GET)
    public Model getClosedProposesToCancel(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAcceptedProposesToCancel(conversationDataset);
    }
    
    @RequestMapping(value = "/getAcceptsOfPropsalsToCancel", method = RequestMethod.GET)
    public Model getClosedAcceptsProposesToCancel(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAcceptsProposesToCancel(conversationDataset);
    }

    @RequestMapping(value = "/getProposalsInCancelledAgreements", method = RequestMethod.GET)
    public Model getClosedProposesInCancelledAgreement(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getProposesInCancelledAgreement(conversationDataset);
    }
    
    @RequestMapping(value = "/getAcceptsInCancelledAgreements", method = RequestMethod.GET)
    public Model getClosedAcceptsInCancelledAgreement(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAcceptsInCancelledAgreement(conversationDataset);
    }
}
