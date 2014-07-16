package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.cryptography.service.CryptographyService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

/**
 * User: ypanchenko
 * Date: 14.07.2014
 */
public class WonSignerTest
{

  private static final String RESOURCE_FILE_1 = "/test_1_graph.trig";
  private static final String RESOURCE_FILE_2 = "/test_1_2graphs.trig";
  private static final String RESOURCE_FILE_3 = "/test_1_2graphs_1sig.trig";
  private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";

  @Test
  public void testSignOneGraph() throws Exception {

    // create dataset with one named graph
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE_1);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    String modelName = dataset.listNames().next();

    // test sign()
    WonSigner signer = new WonSigner(dataset, new SignatureAlgorithmFisteus2010());
    CryptographyService crypService = new CryptographyService();
    KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

    //KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    //kpg.initialize(2048);
    //KeyPair keyPair = kpg.genKeyPair();

    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();
    signer.sign(privateKey);

    // write for debugging
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
    os.close();

    // extract names of the named graphs
    Iterator<String> names = dataset.listNames();
    List<String> namesList = new ArrayList<String>();
    while (names.hasNext()) {
      namesList.add(names.next());
    }

    // do some checks to make sure there is 1 signed names graph
    // and one signature
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

  @Test
  public void testSignTwoGraphs() throws Exception {

    // create dataset with two named graphs
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE_2);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    Iterator<String> origNames = dataset.listNames();
    String modelName1 = origNames.next();
    String modelName2 = origNames.next();

    // test sign()
    WonSigner signer = new WonSigner(dataset, new SignatureAlgorithmFisteus2010());
    CryptographyService crypService = new CryptographyService();
    KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

    //KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    //kpg.initialize(2048);
    //KeyPair keyPair = kpg.genKeyPair();

    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();
    signer.sign(privateKey);

    // write for debugging
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
    os.close();

    // extract names of the named graphs
    Iterator<String> names = dataset.listNames();
    List<String> namesList = new ArrayList<String>();
    while (names.hasNext()) {
      namesList.add(names.next());
    }

    // do some checks to make sure there is 2 signed names graphs
    // and two signatures
    Assert.assertEquals(2, namesList.size());
    Assert.assertTrue(namesList.get(0).equals(modelName1) || namesList.get(1).equals(modelName1));
    Assert.assertTrue(namesList.get(0).equals(modelName2) || namesList.get(1).equals(modelName2));
    Assert.assertTrue(dataset.getDefaultModel().listStatements().hasNext());
    Set<String> subjs = new HashSet<String>();
    int countTriples = 0;
    StmtIterator sti = dataset.getDefaultModel().listStatements();
    while (sti.hasNext()) {
      String subj = sti.next().getSubject().toString();
      subjs.add(subj);
      countTriples++;
    }
    Assert.assertEquals(20, countTriples);
    Assert.assertTrue(subjs.contains(modelName1) && subjs.contains(modelName2));
  }

  @Test
  public void testSignTwoGraphsWhereOneAlreadyHasSignature() throws Exception {

    // create dataset with two named graph where one is already signed
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE_3);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    Iterator<String> origNames = dataset.listNames();
    String modelName1 = origNames.next();
    String modelName2 = origNames.next();

    // test sign()
    WonSigner signer = new WonSigner(dataset, new SignatureAlgorithmFisteus2010());
    CryptographyService crypService = new CryptographyService();
    KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

    //KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    //kpg.initialize(2048);
    //KeyPair keyPair = kpg.genKeyPair();

    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();
    signer.sign(privateKey);

    // write for debugging
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
    os.close();

    // extract names of the named graphs
    Iterator<String> names = dataset.listNames();
    List<String> namesList = new ArrayList<String>();
    while (names.hasNext()) {
      namesList.add(names.next());
    }

    // do some checks to make sure there is 2 signed names graphs
    // and two signatures
    Assert.assertEquals(3, namesList.size());
    Assert.assertTrue(namesList.get(0).equals(modelName1)
                        || namesList.get(1).equals(modelName1)
                        || namesList.get(2).equals(modelName1));
    Assert.assertTrue(namesList.get(0).equals(modelName2)
                        || namesList.get(1).equals(modelName2)
                        || namesList.get(2).equals(modelName2));
    Assert.assertTrue(dataset.getDefaultModel().listStatements().hasNext());
    Set<String> subjs = new HashSet<String>();
    int countTriples = 0;
    StmtIterator sti = dataset.getDefaultModel().listStatements();
    while (sti.hasNext()) {
      String subj = sti.next().getSubject().toString();
      subjs.add(subj);
      countTriples++;
    }
    Assert.assertEquals(20, countTriples);
    Assert.assertTrue(
      (subjs.contains(modelName1) && !subjs.contains(modelName2))
    || (subjs.contains(modelName2) && !subjs.contains(modelName1)));
  }
}
