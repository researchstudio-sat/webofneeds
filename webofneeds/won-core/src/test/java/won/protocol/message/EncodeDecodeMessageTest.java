package won.protocol.message;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.util.IsoMatcher;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko Date: 05.08.2014
 */
public class EncodeDecodeMessageTest {
    private static final Lang LANG = Lang.JSONLD;
    // private static final Lang LANG = Lang.TRIG;
    private static final String RESOURCE_DIR = "/atom-lifecycle_with_message_02adj/";
    private static final String RESOURCE_FILE = RESOURCE_DIR + "01_create_atom/01_OA_to_WN1-without-sig.trig";
    private static final String[] RESOURCE_FILES_WITHOUT_SIG = new String[] {
                    RESOURCE_DIR + "01_create_atom/01_OA_to_WN1-without-sig.trig",
                    RESOURCE_DIR + "02_connect/01_OA_to_WN1-without-sig.trig",
                    RESOURCE_DIR + "03_receive_connect/01_WN2_to_WN1-without-sig.trig",
                    RESOURCE_DIR + "04_deactivate_(by_owner)/01_WN2_to_WN1-without-sig.trig" };

    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void testEncodeDecodeOneMessage() throws Exception {
        Dataset msgDatasetIn = Utils.createTestDataset(RESOURCE_FILE);
        performTest(msgDatasetIn);
    }

    @Test
    public void testEncodeDecodeAllMessagesWithoutSignature() throws Exception {
        for (String inResource : RESOURCE_FILES_WITHOUT_SIG) {
            Dataset msgDatasetIn = Utils.createTestDataset(inResource);
            performTest(msgDatasetIn);
        }
    }

    private void performTest(final Dataset msgDatasetIn) {
        WonMessage wonMessageIn = WonMessageDecoder.decodeFromDataset(msgDatasetIn);
        String encoded = WonMessageEncoder.encode(wonMessageIn, LANG);
        WonMessage wonMessageOut = WonMessageDecoder.decode(LANG, encoded);
        Dataset msgDatasetOut = WonMessageEncoder.encodeAsDataset(wonMessageOut);
        Assert.assertTrue(IsoMatcher.isomorphic(msgDatasetIn.asDatasetGraph(), msgDatasetOut.asDatasetGraph()));
    }
}
