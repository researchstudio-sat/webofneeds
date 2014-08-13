package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageMethod;
import won.protocol.message.WonMessageOntology;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessageEncoderTest
{

  private static final String RESOURCE_FILE = "/test_1_2graphs_1sig.trig";


  @Test
  public void testEncodeAsDataset() throws Exception {
    Dataset messageContentOrig = TestUtils.createTestDataset(RESOURCE_FILE);
    List<String> modelNames = TestUtils.getModelNames(messageContentOrig);

    WonMessage message = new WonMessage(WonMessageOntology.PROTOCOL_OSNPC_RESOURCE,
                                        new WonMessageMethod(WonMessageOntology.METHOD_CREATE_NEED_RESOURCE),
                                        messageContentOrig);
    Dataset messageAsDataset = WonMessageEncoder.encodeAsDataset(message);

    // should have the same number of named graphs
    Assert.assertEquals(modelNames.size(), TestUtils.getModelNames(messageAsDataset).size());
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

    // expect 2 triples added in this example (protocol and method)
    StmtIterator iterator = addedPart.listStatements();
    int stmtCount = 0;
    Set<String> bnodes = new HashSet<String>();
    while (iterator.hasNext()) {
      Statement stmt = iterator.next();
      if (stmt.getSubject().isAnon()) {
        bnodes.add(stmt.getSubject().getId().toString());
      }
      Assert.assertTrue(expectedMsgProperties.contains(stmt.getPredicate().getURI()));
      Assert.assertTrue(expectedMsgObjects.contains(stmt.getObject().asResource().getURI()));
      stmtCount++;
    }
    Assert.assertEquals(3, stmtCount);
    Assert.assertEquals(1, bnodes.size());

  }

  // TODO test encode as Json LD

  private static final Set<String> expectedMsgProperties = new HashSet<String>();

  static {
    expectedMsgProperties.add(WonMessageOntology.MESSAGE_ONTOLOGY_URI + WonMessageOntology.METHOD_PROPERTY);
    expectedMsgProperties.add(WonMessageOntology.MESSAGE_ONTOLOGY_URI + WonMessageOntology.PROTOCOL_PROPERTY);
    expectedMsgProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
  }

  private static final Set<String> expectedMsgObjects = new HashSet<String>();

  static {
    expectedMsgObjects.add(WonMessageOntology.METHOD_CREATE_NEED_RESOURCE);
    expectedMsgObjects.add(WonMessageOntology.PROTOCOL_OSNPC_RESOURCE);
    expectedMsgObjects.add(WonMessageOntology.MESSAGE_TYPE_RESOURCE);
  }

}
