package won.protocol.message;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

public class WonMessageTest {
    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void testForwardedMessage1() {
        Dataset input = DatasetFactory.createGeneral();
        RDFDataMgr.read(input, getResourceAsStream("wonmessage/forward/forwarded-msg-1.trig"), Lang.TRIG);
        WonMessage msg = WonMessage.of(input);
        Assert.assertEquals(URI.create("https://satvm05.researchstudio.at/won/resource/event/pcunhsv1urpd2q3bfpan"),
                        msg.getMessageURI());
        Assert.assertEquals(
                        URI.create("https://satvm05.researchstudio.at/won/resource/connection/zy478j5k7roa38f2ao9l"),
                        msg.getSenderURI());
        Assert.assertEquals(
                        URI.create("https://satvm05.researchstudio.at/won/resource/connection/nz3dg71sop2v5f82j3lm"),
                        msg.getRecipientURI());
        Assert.assertTrue(msg.getForwardedMessageURIs().contains(URI
                        .create("https://satvm05.researchstudio.at/won/resource/event/wxmlua2uu1gu")));
        Assert.assertEquals(WonMessageDirection.FROM_OWNER, msg.getEnvelopeType());
    }

    @Test
    public void testGetMessageContentDataset() {
        Dataset input = DatasetFactory.createGeneral();
        RDFDataMgr.read(input, getResourceAsStream("wonmessage/extract_content/create_message.trig"), Lang.TRIG);
        WonMessage msg = WonMessage.of(input);
        Dataset content = msg.getMessageContent();
        int count = 0;
        for (Iterator<String> it = content.listNames(); it.hasNext(); it.next()) {
            count++;
        }
        assertEquals(2, count);
        Assert.assertTrue(content.getDefaultModel().isEmpty());
        Resource atomInContentGraph = RdfUtils.findOneSubjectResource(content, RDF.type, WON.Atom);
        Assert.assertNotNull(atomInContentGraph);
        Set<String> modelsOfAtom = RdfUtils.getModelsOfSubjectResource(content, atomInContentGraph);
        Assert.assertEquals(1, modelsOfAtom.size());
        String contentGraphName = modelsOfAtom.iterator().next();
        Resource signature = RdfUtils.findOneSubjectResource(content, WONMSG.signedGraph,
                        new ResourceImpl(contentGraphName));
        // throws exception if none found
        Set<String> sigGraphNames = RdfUtils.getModelsOfSubjectResource(content, signature);
        Assert.assertEquals(1, sigGraphNames.size());
        URI atomURI = WonRdfUtils.AtomUtils.getAtomURI(content);
        URI messageURI = msg.getMessageURI();
        RdfUtils.renameResourceWithPrefix(content, messageURI.toString(), atomURI.toString());
        Assert.assertTrue(
                        content.containsNamedModel("https://node.matchat.org/won/resource/atom/so20ub00h1de#atom-sig"));
        Assert.assertTrue(content.getNamedModel("https://node.matchat.org/won/resource/atom/so20ub00h1de#atom-sig")
                        .containsResource(new ResourceImpl(
                                        "https://node.matchat.org/won/resource/atom/so20ub00h1de#atom-sig")));
        Assert.assertTrue(content.containsNamedModel("https://node.matchat.org/won/resource/atom/so20ub00h1de#atom"));
        Assert.assertTrue(content.getNamedModel("https://node.matchat.org/won/resource/atom/so20ub00h1de#atom")
                        .containsResource(new ResourceImpl("https://node.matchat.org/won/resource/atom/so20ub00h1de")));
    }

    @Test
    public void test_message_and_response_in_same_dataset() {
        WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("uri:/messageUri"),
                        URI.create("uri:/localSocket"), URI.create("uri:/localConnection"),
                        URI.create("uri:/localAtom"),
                        URI.create("uri:/localWonnode"),
                        URI.create("uri:/targetSocket"), URI.create("uri:/targetConnection"),
                        URI.create("uri:/targetAtom"), URI.create("uri:/targetWonNode"),
                        "hello").build();
        WonMessage response = WonMessageBuilder.setPropertiesForNodeResponse(msg, true, URI.create("uri:/succ1"),
                        Optional.empty(), WonMessageDirection.FROM_OWNER).build();
        Dataset both = msg.getCompleteDataset();
        RdfUtils.addDatasetToDataset(both, response.getCompleteDataset());
        WonMessage msgAndResponse = WonMessage.of(both);
        Assert.assertEquals("messageUri should be that of original message", URI.create("uri:/messageUri"),
                        msgAndResponse.getMessageURI());
    }

    @Test
    public void test_message_and_two_responses_in_same_dataset() {
        WonMessage msg = WonMessageBuilder.setMessagePropertiesForConnectionMessage(
                        URI.create("uri:/messageUri"),
                        URI.create("uri:/localSocket"), URI.create("uri:/localConnection"),
                        URI.create("uri:/localAtom"),
                        URI.create("uri:/localWonnode"),
                        URI.create("uri:/targetSocket"), URI.create("uri:/targetConnection"),
                        URI.create("uri:/targetAtom"), URI.create("uri:/targetWonNode"),
                        "hello").build();
        WonMessage response = WonMessageBuilder.setPropertiesForNodeResponse(msg, true, URI.create("uri:/succ1"),
                        Optional.empty(), WonMessageDirection.FROM_OWNER).build();
        WonMessage response2 = WonMessageBuilder.setPropertiesForNodeResponse(msg, true, URI.create("uri:/succ2"),
                        Optional.empty(), WonMessageDirection.FROM_EXTERNAL).build();
        Dataset both = msg.getCompleteDataset();
        RdfUtils.addDatasetToDataset(both, response.getCompleteDataset());
        RdfUtils.addDatasetToDataset(both, response2.getCompleteDataset());
        WonMessage msgAndResponse = WonMessage.of(both);
        Assert.assertEquals("messageUri should be that of original message", URI.create("uri:/messageUri"),
                        msgAndResponse.getMessageURI());
    }
}
