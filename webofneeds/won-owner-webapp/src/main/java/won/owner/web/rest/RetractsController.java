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
@RequestMapping("/rest/retracts")
public class RetractsController {

    @Autowired
    private LinkedDataSource linkedDataSource;

    public void setLinkedDataSource(LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    @RequestMapping(value = "/getRetracts", method = RequestMethod.GET)
    public Model getClosedRetracts(String connectionUri) {

        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);
        return HighlevelProtocols.getAcceptedRetracts(conversationDataset);
    }
}
