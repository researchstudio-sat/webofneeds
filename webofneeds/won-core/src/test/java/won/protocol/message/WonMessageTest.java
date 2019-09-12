package won.protocol.message;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
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
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class WonMessageTest {
    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    private String getResourceAsString(String name) throws Exception {
        byte[] buffer = new byte[256];
        StringWriter sw = new StringWriter();
        try (InputStream in = getResourceAsStream(name)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = in.read(buffer)) > -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return new String(baos.toByteArray(), Charset.defaultCharset());
        }
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
        WonMessage msg = new WonMessage(input);
        Assert.assertEquals(URI.create("https://satvm05.researchstudio.at/won/resource/event/hsvug43m3rvz9qei25zh"),
                        msg.getMessageURI());
        Assert.assertEquals(URI.create("https://satvm05.researchstudio.at/won/resource/event/pcunhsv1urpd2q3bfpan"),
                        msg.getCorrespondingRemoteMessageURI());
        Assert.assertEquals(
                        URI.create("https://satvm05.researchstudio.at/won/resource/connection/zy478j5k7roa38f2ao9l"),
                        msg.getSenderURI());
        Assert.assertEquals(
                        URI.create("https://satvm05.researchstudio.at/won/resource/connection/nz3dg71sop2v5f82j3lm"),
                        msg.getRecipientURI());
        Assert.assertEquals(URI.create("https://satvm05.researchstudio.at/won/resource/event/e5syo59w9t3if0y428r8"),
                        msg.getForwardedMessageURI());
        Assert.assertEquals(WonMessageDirection.FROM_EXTERNAL, msg.getEnvelopeType());
    }

    @Test
    public void testGetMessageContentDataset() {
        Dataset input = DatasetFactory.createGeneral();
        RDFDataMgr.read(input, getResourceAsStream("wonmessage/extract_content/create_message.trig"), Lang.TRIG);
        WonMessage msg = new WonMessage(input);
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
}
