package won.protocol.message;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRepRdfUtils;
import won.protocol.vocabulary.REP;
import won.protocol.vocabulary.SCHEMA;

import java.net.URI;

public class WonMessageRepTest {
    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void exchangeRandomHash() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        baseRes.addProperty(REP.RandomHash, "7a84d381e462d83ac918c90eb045c6077d764b2ca03e30906fc4c30ef43ad57d");

        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                URI.create("messageUri"),
                URI.create("localConnection"),
                URI.create("localAtom"),
                URI.create("localWonNode"),
                URI.create("targetConnection"),
                URI.create("TargetAtom"),
                URI.create("remoteWonNode"),
                model).build();

        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
        // TODO assert equals #RandomHash with RandomHash above


//        Model model = WonRepRdfUtils.MessageUtils.textMessage("HALLO");
//        model = WonRepRdfUtils.MessageUtils.addStuff(model);
//        Resource baseRes = RdfUtils.findOrCreateBaseResource(model);
        Resource locationResource = model.createResource();
        baseRes.addProperty(REP.RandomHash, locationResource);
        Resource geoResource = model.createResource();
        geoResource.addLiteral(SCHEMA.NAME, "literal");

        locationResource.addProperty(SCHEMA.NAME, "test");
        locationResource.addProperty(SCHEMA.GEO, geoResource);

        final WonMessage msg2 = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                URI.create("messageUri"),
                URI.create("localConnection"),
                URI.create("localAtom"),
                URI.create("localWonNode"),
                URI.create("targetConnection"),
                URI.create("TargetAtom"),
                URI.create("remoteWonNode"), model).build();

//        RDFDataMgr.write(System.out, msg2.getCompleteDataset(), Lang.TRIG);
    }

    @Test
    public void blindSign() {
        Model model = WonRepRdfUtils.createBaseModel();
        Resource baseRes = model.createResource();
        baseRes.addProperty(REP.ReputationToken, "7a84d381e462d83ac918c90eb045c6077d764b2ca03e30906fc4c30ef43ad57d");

        final WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                URI.create("messageUri"),
                URI.create("localConnection"),
                URI.create("localAtom"),
                URI.create("localWonNode"),
                URI.create("targetConnection"),
                URI.create("TargetAtom"),
                URI.create("remoteWonNode"),
                model).build();

        RDFDataMgr.write(System.out, msg.getCompleteDataset(), Lang.TRIG);
    }
}
