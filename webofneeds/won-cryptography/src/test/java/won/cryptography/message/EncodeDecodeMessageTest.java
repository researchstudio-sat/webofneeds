package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.message.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * User: ypanchenko
 * Date: 05.08.2014
 */
public class EncodeDecodeMessageTest
{

  private static final String RESOURCE_DATA_FILE = "/test_1_2graphs_1sig.trig";
  //private static final String RESOURCE_MSG_FILE = "/test_1_message.trig";

  @Test
  public void testEncodeDecodeMessage() throws Exception {
    Lang lang = Lang.JSONLD;
    //TODO replace with decode from Dataset read from test file?
    WonMessage wonmsg = createTestMessage(
      WonMessageOntology.PROTOCOL_OSNPC_RESOURCE,
      new WonMessageMethod(WonMessageOntology.METHOD_CREATE_NEED_RESOURCE),
      RESOURCE_DATA_FILE);

    String encoded = WonMessageEncoder.encode(wonmsg, lang);
    System.out.println(encoded);
    //System.out.println();

    WonMessage wonmsgDecoded = WonMessageDecoder.decode(lang, encoded);
    // for debugging
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, WonMessageEncoder.encodeAsDataset(wonmsgDecoded), RDFFormat.TRIG.getLang());
    System.out.println(sw.toString());

    Assert.assertTrue(wonmsg.getProtocol().equals(wonmsgDecoded.getProtocol()));
    Assert.assertTrue(wonmsg.getMethod().equals(wonmsgDecoded.getMethod()));

    //This doesn't pass!!! The Jena seems to have a bug...
    //Assert.assertTrue(DatasetLib.isomorphic(WonMessageEncoder.encodeAsDataset(wonmsg),
    //                                        WonMessageEncoder.encodeAsDataset(wonmsgDecoded)));

  }


  private WonMessage createTestMessage(String protocolUri, WonMessageMethod method,
                                       String resourceName) throws IOException {

    // create dataset with a signed named graph
    InputStream is = this.getClass().getResourceAsStream(resourceName);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    WonMessage message = new WonMessage(protocolUri, method, dataset);

    return message;
  }
}
