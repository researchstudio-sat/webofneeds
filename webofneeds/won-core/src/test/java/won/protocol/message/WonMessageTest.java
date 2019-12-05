package won.protocol.message;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
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
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonMessageUriHelper;
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
        WonMessage msg = WonMessageBuilder.connectionMessage()
                        .sockets()
                        .sender(URI.create("uri:/localAtom#socket"))
                        .recipient(URI.create("uri:/targetAtom#socket"))
                        .content().text("hello").build();
        WonMessage response = WonMessageBuilder
                        .response()
                        .fromConnection(URI.create("uri:/conn1"))
                        .respondingToMessageFromOwner(msg)
                        .success()
                        .build();
        Dataset both = msg.getCompleteDataset();
        RdfUtils.addDatasetToDataset(both, response.getCompleteDataset());
        WonMessage msgAndResponse = WonMessage.of(both);
        Assert.assertEquals("messageUri should be that of original message", WonMessageUriHelper.getSelfUri(),
                        msgAndResponse.getMessageURI());
    }

    @Test
    public void test_message_and_two_responses_in_same_dataset() {
        WonMessage msg = WonMessageBuilder.connectionMessage()
                        .sockets()
                        .sender(URI.create("uri:/localAtom#socket"))
                        .recipient(URI.create("uri:/targetAtom#socket"))
                        .content().text("hello").build();
        WonMessage response = WonMessageBuilder
                        .response()
                        .fromConnection(URI.create("uri:/conn1"))
                        .respondingToMessageFromOwner(msg)
                        .success()
                        .build();
        WonMessage response2 = WonMessageBuilder
                        .response()
                        .fromConnection(URI.create("uri:/conn2"))
                        .respondingToMessageFromExternal(msg)
                        .success()
                        .build();
        Dataset both = msg.getCompleteDataset();
        RdfUtils.addDatasetToDataset(both, response.getCompleteDataset());
        RdfUtils.addDatasetToDataset(both, response2.getCompleteDataset());
        WonMessage msgAndResponse = WonMessage.of(both);
        Assert.assertEquals("messageUri should be that of original message",
                        WonMessageUriHelper.getSelfUri(),
                        msgAndResponse.getMessageURI());
    }
}
