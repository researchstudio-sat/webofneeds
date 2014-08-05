package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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

  private List<String> getModelNames(Dataset dataset) {
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

    WonMessage message = new WonMessage(MessageOntology.PROTOCOL_OSNPC,
                                        new MessageMethod(MessageOntology.METHOD_CREATE_NEED), messageContentOrig);
    Dataset messageContent = message.getMessageContent();

    // should have the same number of named graphs
    Assert.assertEquals(modelNames.size(), getModelNames(messageContent).size());
    // each named graph should be the representation of the same named graph in input content
    for (String name : modelNames) {
      Assert.assertTrue(messageContentOrig.getNamedModel(name).isIsomorphicWith(messageContent.getNamedModel(name)));
    }
    Assert.assertTrue(messageContentOrig.getDefaultModel().isIsomorphicWith(messageContent.getDefaultModel()));
    // should have one method triple
    Assert.assertTrue(message.hasMethod(MessageOntology.METHOD_CREATE_NEED));
    // should have one protocol triple
    Assert.assertTrue(message.getProtocol().equals(MessageOntology.PROTOCOL_OSNPC));
  }

  @Test
  public void testMessageAsDataset() throws Exception {
    Dataset messageContentOrig = createTestDataset();
    List<String> modelNames = getModelNames(messageContentOrig);

    WonMessage message = new WonMessage(MessageOntology.PROTOCOL_OSNPC,
                                        new MessageMethod(MessageOntology.METHOD_CREATE_NEED), messageContentOrig);
    Dataset messageAsDataset = message.asDataset();

    // should have the same number of named graphs
    Assert.assertEquals(modelNames.size(), getModelNames(messageAsDataset).size());
    // each named graph should be the representation of the same named graph in input content
    for (String name : modelNames) {
      Assert.assertTrue(messageContentOrig.getNamedModel(name).isIsomorphicWith(messageAsDataset.getNamedModel(name)));
    }

    Model addedPart = messageAsDataset.getDefaultModel().difference(messageContentOrig.getDefaultModel());

    // for debugging
    //StringWriter sw = new StringWriter();
    //addedPart.write(sw, "TURTLE");
    //System.out.println(sw.toString());
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, messageAsDataset, RDFFormat.TRIG.getLang());
    System.out.println(sw.toString());

    // except 2 triples added in this example (protocol and method)
    StmtIterator iterator = addedPart.listStatements();
    int stmtCount = 0;
    while (iterator.hasNext()) {
      Statement stmt = iterator.next();
      Assert.assertTrue("http://purl.org/webofneeds/message#hasMethod".equals(stmt.getPredicate().getURI())
                          || "http://purl.org/webofneeds/message#hasProtocol".equals(stmt.getPredicate().getURI()));
      Assert.assertTrue("http://purl.org/webofneeds/message#OSNPC".equals(stmt.getObject().asResource().getURI())
                          || "http://purl.org/webofneeds/message#CreateNeed"
        .equals(stmt.getObject().asResource().getURI()));
      stmtCount++;
    }
    Assert.assertEquals(2, stmtCount);

  }


}
