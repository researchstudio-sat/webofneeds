package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import org.apache.jena.riot.Lang;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko
 * Date: 05.08.2014
 */
public class EncodeDecodeMessageTest
{

  //TODO Lang.JSONLD is replaced temporarily by Lang.TRIG due to the bug with JSONLD
  private static final Lang LANG =  Lang.JSONLD;
  //private static final Lang LANG =  Lang.TRIG;

  private static final String RESOURCE_DIR = "/need-lifecycle_with_message_02adj/";
  private static final String RESOURCE_FILE =
    RESOURCE_DIR + "01_create_need/01_OA_to_WN1-without-sig.trig";

  private static final String[] RESOURCE_FILES_WITHOUT_SIG = new String[] {
    RESOURCE_DIR + "01_create_need/01_OA_to_WN1-without-sig.trig",
    RESOURCE_DIR + "02_connect/01_OA_to_WN1-without-sig.trig",
    RESOURCE_DIR + "03_receive_connect/01_WN2_to_WN1-without-sig.trig",
    RESOURCE_DIR + "04_deactivate_(by_owner)/01_WN2_to_WN1-without-sig.trig"
  };


  @Test
  public void testEncodeDecodeOneMessage() throws Exception {
    Dataset msgDatasetIn = TestUtils.createTestDataset(RESOURCE_FILE);
    performTest(msgDatasetIn);
  }

  @Test
  public void testEncodeDecodeAllMessagesWithoutSignature() throws Exception {
    for (String inResource : RESOURCE_FILES_WITHOUT_SIG) {
      Dataset msgDatasetIn = TestUtils.createTestDataset(inResource);
      performTest(msgDatasetIn);
    }
  }

  // TODO test with signatures


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

    Assert.assertTrue(wonMessageIn.equals(wonMessageOut));

    // TODO This test doesn't pass with JSONLD!!! The Jena has a bug, see:
    // https://issues.apache.org/jira/browse/JENA-758
    // The Jena people seem to already solve this bug in the Jena svn,
    // so probably it will be working in next Jena release
    Assert.assertTrue(DatasetLib.isomorphic(msgDatasetIn, msgDatasetOut));
  }

}
