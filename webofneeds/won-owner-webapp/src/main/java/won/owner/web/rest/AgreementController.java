package won.owner.web.rest;

import org.apache.jena.query.Dataset;
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

        //TODO: handle exceptions

        int depth = 3;  // depth 3 from connection gives us the messages in the conversation
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member", pmap));

        Dataset conversationDataset = linkedDataSource.getDataForResourceWithPropertyPath(
                URI.create(connectionUri), propertyPaths, maxRequests, depth, false);

        return HighlevelProtocols.getAgreements(conversationDataset);
    }
}
