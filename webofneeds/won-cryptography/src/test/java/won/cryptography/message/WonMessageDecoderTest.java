package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import org.junit.Assert;
import org.junit.Test;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessageDecoderTest
{

  private static final String RESOURCE_CONTENT = "/test_1_message_content.trig";
  private static final String RESOURCE_MESSAGE = "/test_1_message.trig";
  private static final String RESOURCE_META = "/test_1_message_meta.ttl";


  @Test
  public void testDecodeFromDataset() throws Exception {

    Dataset messageAsDataset = TestUtils.createTestDataset(RESOURCE_MESSAGE);
    Dataset messageContentExpected = TestUtils.createTestDataset(RESOURCE_CONTENT);
    Model messageMetaExpected = TestUtils.createTestModel(RESOURCE_META);

    WonMessage wonmsg = WonMessageDecoder.decodeFromDataset(messageAsDataset);
    Dataset messageContent = wonmsg.getMessageContent();
    Model messageMeta = wonmsg.getMessageMetadata();

    Assert.assertTrue(DatasetLib.isomorphic(messageContentExpected, messageContent));
    Assert.assertTrue(messageMetaExpected.isIsomorphicWith(messageMeta));

    //System.out.println("MSG_META");
    //TestUtils.print(messageMeta);
    //System.out.println("MSG_META_EXP");
    //TestUtils.print(messageMetaExpected);

  }

  // TODO test decode from Json LD


}
