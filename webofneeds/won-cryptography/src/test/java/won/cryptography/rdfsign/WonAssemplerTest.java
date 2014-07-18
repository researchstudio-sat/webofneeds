package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class WonAssemplerTest
{

  private static final String RESOURCE_FILE = "/test_1_graph.trig";


  @Test
  public void testAssembleOneGraphSignature() throws Exception {

    // The Signingframework reader cannot reproduce the correct graph
    // structure, it has problems with blank nodes [] parts.
    // GraphCollection gc = TriGPlusReader.readFile(inFile);

    // create dataset with one named graph and convert it into GraphCollection
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    String modelName = dataset.listNames().next();
    GraphCollection gc = ModelConverter.modelToGraphCollection(modelName, dataset);

    // create mock signature
    SignatureData mockSigData = createMockSignature();
    gc.setSignature(mockSigData);

    // test assemble()
    WonAssembler.assemble(gc, dataset);

    // use for debugging
    //File outFile = File.createTempFile("won", ".trig");
    //System.out.println(outFile);
    //OutputStream os = new FileOutputStream(outFile);
    //RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
    //os.close();

    // extract names of the named graphs
    Iterator<String> names = dataset.listNames();
    List<String> namesList = new ArrayList<String>();
    while (names.hasNext()) {
      namesList.add(names.next());
    }

    // do some checks to make sure there is 1 signed names graph
    Assert.assertEquals(1, namesList.size());
    Assert.assertTrue(dataset.getDefaultModel().listStatements().hasNext());
    Set<String> subjs = new HashSet<String>();
    int countTriples = 0;
    StmtIterator sti = dataset.getDefaultModel().listStatements();
    while (sti.hasNext()) {
      String subj = sti.next().getSubject().toString();
      subjs.add(subj);
      countTriples++;
    }
    Assert.assertEquals(10, countTriples);
    Assert.assertTrue(subjs.contains(modelName));
  }


  private SignatureData createMockSignature() throws NoSuchAlgorithmException {

    SignatureData mockSigData = new SignatureData();
    mockSigData.setHash(new BigInteger(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}));
    mockSigData.setSignature("\"blahblahSignature\"");
    mockSigData.setDigestGen(MessageDigest.getInstance("sha-256"));
    mockSigData.setCanonicalizationMethod("blahblahCanonicalizationMethod");
    mockSigData.setGraphDigestMethod("blahblahGraphDigestMethod");
    mockSigData.setSerializationMethod("blahblahSerializationmethod");
    mockSigData.setSignatureMethod("blahblahSigMathod");
    mockSigData.setVerificationCertificate("\"blahblahVerificationCertificate\"");

    return mockSigData;
  }
}
