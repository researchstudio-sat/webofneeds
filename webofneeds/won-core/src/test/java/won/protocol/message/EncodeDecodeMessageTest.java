package won.protocol.message;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.util.IsoMatcher;
import org.junit.Assert;
import org.junit.Test;

import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko Date: 05.08.2014
 */
public class EncodeDecodeMessageTest {
    private static final Lang LANG = Lang.JSONLD;
    // private static final Lang LANG = Lang.TRIG;
    private static final String RESOURCE_DIR = "/need-lifecycle_with_message_02adj/";
    private static final String RESOURCE_FILE = RESOURCE_DIR + "01_create_need/01_OA_to_WN1-without-sig.trig";
    private static final String[] RESOURCE_FILES_WITHOUT_SIG = new String[] {
                    RESOURCE_DIR + "01_create_need/01_OA_to_WN1-without-sig.trig",
                    RESOURCE_DIR + "02_connect/01_OA_to_WN1-without-sig.trig",
                    RESOURCE_DIR + "03_receive_connect/01_WN2_to_WN1-without-sig.trig",
                    RESOURCE_DIR + "04_deactivate_(by_owner)/01_WN2_to_WN1-without-sig.trig" };

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
        // for debugging
        System.out.println(encoded);
        System.out.println();
        WonMessage wonMessageOut = WonMessageDecoder.decode(LANG, encoded);
        Dataset msgDatasetOut = WonMessageEncoder.encodeAsDataset(wonMessageOut);
        // for debugging
        System.out.println(RdfUtils.writeDatasetToString(msgDatasetOut, Lang.TRIG));
        Assert.assertTrue(IsoMatcher.isomorphic(msgDatasetIn.asDatasetGraph(), msgDatasetOut.asDatasetGraph()));
    }
}
