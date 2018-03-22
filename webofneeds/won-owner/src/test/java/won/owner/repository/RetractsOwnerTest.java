package won.owner.repository;

import org.apache.jena.query.Dataset;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.protocol.agreement.AgreementProtocol;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSourceBase;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;


@RunWith(SpringJUnit4ClassRunner.class)
// Just use the owner-agreement-test.xml configuration file and do not try to create owner-retracts-test.xml configuration...(interpreting Heinko's suggestion)
// The configuration should be the same..
@ContextConfiguration({"classpath:/spring/owner-agreement-test.xml"})
public class RetractsOwnerTest {

    public void setLinkedDataSource(LinkedDataSourceBase linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    @Autowired
    private LinkedDataSource linkedDataSource;

    @Ignore
    @Test
    public void test() {

        // set this to some connection uri which is valid, reachable and permitted to be accessed by this test
        final String CONNECTION_URI_WITH_CONVERSATION = "https://localhost:8443/won/resource/connection/662qkmmt804z6apnr8b4";

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
                URI.create(CONNECTION_URI_WITH_CONVERSATION),
                propertyPaths, maxRequests, depth, false);
        Dataset agreementDataset = AgreementProtocol.getAgreements(conversationDataset);
    }
}
