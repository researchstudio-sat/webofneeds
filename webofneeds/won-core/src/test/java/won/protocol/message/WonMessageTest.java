package won.protocol.message;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class WonMessageTest {
    private InputStream getResourceAsStream(String name) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    private String getResourceAsString(String name) throws Exception {
        byte[] buffer = new byte[256];
        StringWriter sw = new StringWriter();
        try (InputStream in = getResourceAsStream(name)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
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
}
