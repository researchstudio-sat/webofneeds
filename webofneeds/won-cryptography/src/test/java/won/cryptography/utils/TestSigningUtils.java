package won.cryptography.utils;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMWriter;
import org.junit.Test;
import won.cryptography.service.CertificateService;
import won.cryptography.service.KeyPairService;
import won.cryptography.service.KeyStoreService;

import java.io.*;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
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

  //theoretically can be a public key WebID...
  public static String needCertUri = "http://localhost:8080/won/resource/need/3144709509622353000";
  public static String ownerCertUri = "http://localhost:8080/owner/certificate";
  public static String nodeCertUri = "http://localhost:8080/node/certificate";

  public static int countTriples(final StmtIterator sti) {
    int countTriples = 0;
    while (sti.hasNext()) {
      sti.next();
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

  public static Dataset prepareTestDataset(String resourceFile) throws
    IOException {
    // read dataset with created need
    InputStream is = TestSigningUtils.class.getResourceAsStream(resourceFile);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    return dataset;
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


  public void generateTestKeystore() throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    //URL keyUrl = TestSigningUtils.class.getResource(KEYS_FILE);
    File keysFile = null;
    //if (keyUrl == null) {
      keysFile = new File("test-keys2.jks");
    //} else {
    //  keysFile = new File(TestSigningUtils.class.getResource(KEYS_FILE).getFile());
    //}

    KeyStoreService storeService = new KeyStoreService(keysFile, "temp");
    storeService.init();

    KeyPairService keyPairService = new KeyPairService();
    CertificateService certificateService = new CertificateService();

    addKeyByUri(needCertUri, keyPairService, certificateService, storeService);
    addKeyByUri(ownerCertUri, keyPairService, certificateService, storeService);
    addKeyByUri(nodeCertUri, keyPairService, certificateService, storeService);

  }

  @Test
  public void generateKeystoreForNodeAndOwner() throws Exception {

    Security.addProvider(new BouncyCastleProvider());
    //KeyStoreService storeServiceOnNode = new KeyStoreService(new File("node-keys.jks"));
    KeyStoreService storeServiceOnOwner = new KeyStoreService(new File("owner-keys.jks"), "temp");
    storeServiceOnOwner.init();
    //KeyStoreService storeServiceOnMatcher = new KeyStoreService(new File("matcher-keys.jks"));
    KeyPairService keyPairService = new KeyPairService();
    CertificateService certificateService = new CertificateService();

//    addKeyByUris(new String[]{
//                   "http://rsa021.researchstudio.at:8080/won/resource",
//                   "http://sat016.researchstudio.at:8080/won/resource",
//                   "http://localhost:8080/won/resource"},
//                 keyPairService, certificateService, storeServiceOnNode);
    addKeyByUris(new String[]{
                   "http://rsa021.researchstudio.at:8080/owner/rest/keys",
                   "http://sat016.researchstudio.at:8080/owner/rest/keys",
                   "http://localhost:8080/owner/rest/keys"},
                 keyPairService, certificateService, storeServiceOnOwner);
//    addKeyByUris(new String[]{
//                   "http://sat001.researchstudio.at:8080/matcher/resource",
//                   "http://localhost:8080/matcher/resource"},
//                 keyPairService, certificateService, storeServiceOnMatcher);

  }


  public void writeCert() throws IOException, CertificateException {
    //load public  keys:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile, "temp");

    writeCerificate(storeService, needCertUri, needCertUri);
    writeCerificate(storeService, ownerCertUri, ownerCertUri);
    writeCerificate(storeService, ownerCertUri, nodeCertUri);



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

  private static void addKeyByUri(String certUri, final KeyPairService keyPairService,
                      final CertificateService certificateService, final KeyStoreService storeService)
    throws IOException {
    KeyPair keyPair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
    BigInteger serialNumber = BigInteger.valueOf(1);
    Certificate cert = certificateService.createSelfSignedCertificate(serialNumber, keyPair, certUri, certUri);
    storeService.putKey(certUri, keyPair.getPrivate(), new Certificate[]{cert}, false);

    System.out.println(cert);
    //KeyInformationExtractorBouncyCastle extractor = new KeyInformationExtractorBouncyCastle();
  }

  private static void addKeyByUris(final String[] aliasUris, final KeyPairService keyPairService,
                             final CertificateService certificateService, final KeyStoreService storeService)
    throws IOException {
    KeyPair keyPair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
    BigInteger serialNumber = BigInteger.valueOf(1);
    for (String aliasUri : aliasUris) {
      Certificate cert = certificateService.createSelfSignedCertificate(serialNumber, keyPair, aliasUri, aliasUri);
      storeService.putKey(aliasUri, keyPair.getPrivate(), new Certificate[]{cert}, false);
    }
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
