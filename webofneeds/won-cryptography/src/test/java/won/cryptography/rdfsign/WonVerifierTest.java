package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import won.cryptography.service.CryptographyService;

import java.io.*;
import java.net.URI;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Iterator;

/**
 * User: ypanchenko
 * Date: 14.07.2014
 */
public class WonVerifierTest
{

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  private static final String RESOURCE_FILE_1 = "/test_1_graph.trig";
  private static final String RESOURCE_FILE_2 = "/test_1_2graphs.trig";
  private static final String RESOURCE_FILE_3 = "/test_1_2graphs_1sig.trig";
  private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";

  @Test
  public void testSignAndVerifyOneGraph() throws Exception {

    // create dataset with one named graph
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE_1);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();

    // sign it
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
    //File outFile = testFolder.newFile();
    // use this when debugging:
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, dataset, RDFFormat.TRIG_BLOCKS);
    os.close();

    // read it again
    InputStream is2 = new FileInputStream(outFile);
    Dataset dataset2 = DatasetFactory.createMem();
    RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
    is2.close();

    WonVerifier verifier = new WonVerifier(dataset2);
    boolean verified = verifier.verify(publicKey);

    Assert.assertTrue(verified);
    Assert.assertEquals(1, verifier.getVerifiedURIs().size());

    // modify a model and check that it does not verify..
    Model m = dataset2.getNamedModel(dataset2.listNames().next());
    Statement stmt = m.listStatements().nextStatement();
    m.remove(stmt);

    verifier = new WonVerifier(dataset2);
    verified = verifier.verify(publicKey);

    Assert.assertTrue(!verified);

  }

  @Test
  public void testSignAndVerifyTwoGraphs() throws Exception {

    // create dataset with two named graphs
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE_2);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();

    // sign it
    WonSigner signer = new WonSigner(dataset, new SignatureAlgorithmFisteus2010());
    CryptographyService crypService = new CryptographyService();
    KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();
    signer.sign(privateKey);

    // write for debugging
    //File outFile = testFolder.newFile();
    // use this when debugging:
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, dataset, RDFFormat.TRIG_BLOCKS);
    os.close();

    // read it again
    InputStream is2 = new FileInputStream(outFile);
    Dataset dataset2 = DatasetFactory.createMem();
    RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
    is2.close();

    WonVerifier verifier = new WonVerifier(dataset2);
    boolean verified = verifier.verify(publicKey);

    Assert.assertTrue(verified);
    Assert.assertEquals(2, verifier.getVerifiedURIs().size());

    // modify a model and check that it does not verify..
    Iterator<String> names = dataset2.listNames();
    Model m = dataset2.getNamedModel(names.next());
    Model m2 = dataset2.getNamedModel(names.next());
    Statement stmt = m.listStatements().nextStatement();
    m.remove(stmt);

    verifier = new WonVerifier(dataset2);
    verified = verifier.verify(publicKey);

    Assert.assertTrue(!verified);
    Assert.assertEquals(1, verifier.getVerifiedURIs().size());

    // add the removed statement back
    m.add(stmt);
    verifier = new WonVerifier(dataset2);
    verified = verifier.verify(publicKey);
    // now it should verify again
    Assert.assertTrue(verified);


    // this shows that the attack is possible when a shared between graph
    // blank node can be replaced by other blank node in one or both of the
    // graphs, this should not validate, but currently this is a weak point
//    StmtIterator i1 = m.listStatements();
//    Resource anon = null;
//    StmtIterator i2 = null;
//    Statement s2 = null;
//    RDFNode node = null;
//    while (i1.hasNext()) {
//      Statement s1 = i1.nextStatement();
//      if (s1.getSubject().isAnon()) {
//        anon = s1.getSubject();
//        i2 = m2.listStatements(anon, null, node);
//        if (i2.hasNext()) {
//          s2 = i2.next();
//          break;
//        }
//      }
//    }
//    m2.remove(s2);
//    Resource bn = m2.createResource();
//    Statement s2mod = m2.createStatement(bn, s2.getPredicate(), s2.getObject());
//    m2.add(s2mod);
//    Assert.assertTrue(!verified);

  }

  @Test
  public void testSignTwoGraphsWhereOneAlreadyHasSignatureAndVerify() throws Exception {

    // create dataset with two named graphs one of them already with the signature
    InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE_3);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();

    // sign it
    WonSigner signer = new WonSigner(dataset, new SignatureAlgorithmFisteus2010());
    CryptographyService crypService = new CryptographyService();
    KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(RESOURCE_URI));

    PrivateKey privateKey = keyPair.getPrivate();
    PublicKey publicKey = keyPair.getPublic();
    signer.sign(privateKey);

    // write for debugging
    //File outFile = testFolder.newFile();
    // use this when debugging:
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, dataset, RDFFormat.TRIG);
    os.close();

    // read it again
    InputStream is2 = new FileInputStream(outFile);
    Dataset dataset2 = DatasetFactory.createMem();
    RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
    is2.close();

    WonVerifier verifier = new WonVerifier(dataset2);
    boolean verified = verifier.verify(publicKey);

    Assert.assertTrue(verified);
    Assert.assertEquals(2, verifier.getVerifiedURIs().size());

    // modify a model and check that it does not verify..
    Model m = dataset2.getNamedModel(dataset2.listNames().next());
    Statement stmt = m.listStatements().nextStatement();
    m.remove(stmt);

    verifier = new WonVerifier(dataset2);
    verified = verifier.verify(publicKey);

    Assert.assertTrue(!verified);
    Assert.assertEquals(1, verifier.getVerifiedURIs().size());
  }
}
