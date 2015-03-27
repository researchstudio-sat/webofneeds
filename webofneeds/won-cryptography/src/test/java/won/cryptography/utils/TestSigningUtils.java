package won.cryptography.utils;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import won.cryptography.service.CertificateService;
import won.cryptography.service.KeyPairService;
import won.cryptography.service.KeyStoreService;

import java.io.*;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * User: ypanchenko
 * Date: 24.03.2015
 */
public class TestSigningUtils {


  public static final String KEYS_FILE =
    "/won-signed-messages/test-keys.jks";
  public static final String NEED_KEY_NAME = "TEST-NEED-KEY";
  public static final String OWNER_KEY_NAME = "TEST-OWNER-KEY";
  public static final String NODE_KEY_NAME = "TEST-NODE-KEY";

  //theoretically can be a public key WebID...
  public String needCertUri = "http://localhost:8080/won/resource/need/3144709509622353000/certificate#1";
  public String ownerCertUri = "http://localhost:8080/owner/certificate#1";
  public String nodeCertUri = "http://localhost:8080/node/certificate#1";

  //public static final String CERT_FILE_TXT = "/won-signed-messages/certs.txt";

  public static int countTriples(final StmtIterator sti) {
    int countTriples = 0;
    String sigValue = "";
    //StmtIterator sti = testDataset.getNamedModel(NEED_CORE_DATA_SIG_URI).listStatements();
    while (sti.hasNext()) {
      Statement st = sti.next();
      Property prop = st.getPredicate();
      if (prop.getURI().equals("http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#hasSignatureValue")) {
        sigValue = st.getObject().toString();
      }
      countTriples++;
    }
    return countTriples;
  }

  public static String getObjectOfPredAsString(final Model model, String predicateUri) {

    NodeIterator nodes = model.listObjectsOfProperty(model.getProperty(predicateUri));
    String value = "";
    while (nodes.hasNext()) {
      value = nodes.next().asLiteral().toString();
    }
    return value;
  }
  public static Dataset prepareTestDatasetFromNamedGraphs(String resourceFile, final String[] graphNames) throws
    IOException {
    // read dataset with created need
    InputStream is = TestSigningUtils.class.getResourceAsStream(resourceFile);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    Dataset testDataset = DatasetFactory.createMem();
    for (String name : graphNames) {
      testDataset.addNamedModel(name, dataset.getNamedModel(name));
    }
    testDataset.getDefaultModel().setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());
    return testDataset;
  }

  public static Set<String> getSubjects(Model model) {
    Set<String> subjs = new HashSet<String>();
    StmtIterator sti = model.listStatements();
    while (sti.hasNext()) {
      Statement st = sti.next();
      String subj = st.getSubject().toString();
      subjs.add(subj);
    }
    return subjs;
  }

  public static Set<String> getUriResourceObjects(Model model) {
    Set<String> objs = new HashSet<String>();
    StmtIterator sti = model.listStatements();
    while (sti.hasNext()) {
      Statement st = sti.next();
      RDFNode obj = st.getObject();
      if (obj.isURIResource()) {
        objs.add(obj.asResource().toString());
      }
    }
    return objs;
  }

  public void generateKey() throws URISyntaxException {
    URL keyUrl = TestSigningUtils.class.getResource(KEYS_FILE);
    File keysFile = null;
    //if (keyUrl == null) {
      keysFile = new File("test-keys.jks");
    //} else {
    //  keysFile = new File(TestSigningUtils.class.getResource(KEYS_FILE).getFile());
   // }

    System.out.println(keysFile);
    KeyStoreService storeService = new KeyStoreService(keysFile);
    KeyPairService keyPairService = new KeyPairService();
    CertificateService certificateService = new CertificateService();

    addKey(NEED_KEY_NAME, needCertUri, keyPairService, certificateService, storeService);
    addKey(OWNER_KEY_NAME, ownerCertUri, keyPairService, certificateService, storeService);
    addKey(NODE_KEY_NAME, nodeCertUri, keyPairService, certificateService, storeService);

  }


  public void writeCert() throws IOException, CertificateException {
    //load public  keys:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile);



    writeCerificate(storeService, NEED_KEY_NAME, needCertUri);
    writeCerificate(storeService, OWNER_KEY_NAME, ownerCertUri);
    writeCerificate(storeService, NODE_KEY_NAME, nodeCertUri);



  }

  private void writeCerificate(final KeyStoreService storeService, final String keyName, final String certUri)
    throws IOException, CertificateException {

    System.out.println(keyName);
    System.out.println(certUri);

    X509Certificate cert = (X509Certificate) storeService.getCertificate(keyName);

    StringWriter sw = new StringWriter();
    PEMWriter writer = new PEMWriter(sw);
    writer.writeObject(cert);
    writer.close();

    System.out.println(sw.toString());

    PEMParser pemParser = new PEMParser(new StringReader(sw.toString()));
    X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
    X509Certificate certRead = new JcaX509CertificateConverter().setProvider("BC")
                                     .getCertificate(certHolder);
    System.out.println(certRead.toString());

  }

  private static void addKey(final String alias, String certUri, final KeyPairService keyPairService,
                      final CertificateService certificateService, final KeyStoreService storeService) {
    KeyPair keyPair = keyPairService.generateNewKeyPair();
    BigInteger serialNumber = BigInteger.valueOf(1);
    Certificate cert = certificateService.createSelfSignedCertificate(serialNumber, keyPair, certUri);
    storeService.putKey(alias, keyPair.getPrivate(), new Certificate[]{cert});

    System.out.println(cert);
    //KeyInformationExtractorBouncyCastle extractor = new KeyInformationExtractorBouncyCastle();
  }

  public static void writeToTempFile(final Dataset testDataset) throws IOException {
    File outFile = File.createTempFile("won", ".trig");
    System.out.println(outFile);
    OutputStream os = new FileOutputStream(outFile);
    RDFDataMgr.write(os, testDataset, RDFFormat.TRIG.getLang());
    os.close();
  }


  // generate key pair
  //CryptographyService crypService = new CryptographyService();
  //KeyPair keyPair = crypService.createNewNeedKeyPair(URI.create(NEED_URI));

  //KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
  //kpg.initialize(2048);
  //KeyPair keyPair = kpg.genKeyPair();

  //PrivateKey privateKey = keyPair.getPrivate();
  //PublicKey publicKey = keyPair.getPublic();
}
