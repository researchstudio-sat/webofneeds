package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageMethod;
import won.protocol.message.WonMessageOntology;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessageTest
{

  private static final String RESOURCE_FILE = "/test_1_2graphs_1sig.trig";


  private Dataset createTestDataset() throws IOException {

    // create dataset with two named graph where one is already signed
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    return dataset;

  }

  public static List<String> getModelNames(Dataset dataset) {
    List<String> modelNames = new ArrayList<String>();
    Iterator<String> names = dataset.listNames();
    while (names.hasNext()) {
      modelNames.add(names.next());
    }
    return modelNames;
  }


  @Test
  public void testMessageAsWonMessage() throws Exception {
    Dataset messageContentOrig = createTestDataset();
    List<String> modelNames = getModelNames(messageContentOrig);

    WonMessage message = new WonMessage(WonMessageOntology.PROTOCOL_OSNPC_RESOURCE,
                                        new WonMessageMethod(WonMessageOntology.METHOD_CREATE_NEED_RESOURCE),
                                        messageContentOrig);
    Dataset messageContent = message.getMessageContent();

    // should have the same number of named graphs
    Assert.assertEquals(modelNames.size(), getModelNames(messageContent).size());
    // each named graph should be the representation of the same named graph in input content
    for (String name : modelNames) {
      Assert.assertTrue(messageContentOrig.getNamedModel(name).isIsomorphicWith(messageContent.getNamedModel(name)));
    }
    Assert.assertTrue(messageContentOrig.getDefaultModel().isIsomorphicWith(messageContent.getDefaultModel()));
    // should have one method triple
    Assert.assertTrue(message.hasMethod(WonMessageOntology.METHOD_CREATE_NEED_RESOURCE));
    // should have one protocol triple
    Assert.assertTrue(message.getProtocol().equals(WonMessageOntology.PROTOCOL_OSNPC_RESOURCE));
  }


}
