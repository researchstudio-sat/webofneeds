package won.cryptography.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.service.CertificateService;
import won.cryptography.service.KeyPairService;
import won.cryptography.service.keystore.FileBasedKeyStoreService;
import won.cryptography.service.keystore.KeyStoreService;

/**
 * User: ypanchenko Date: 24.03.2015
 */
public class TestSigningUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String KEYS_FILE = "/won-signed-messages/test-keys.jks";
    // theoretically can be a public key WebID...
    public static final String atomCertUri = "http://localhost:8080/won/resource/need/3144709509622353000";
    public static final String ownerCertUri = "http://localhost:8080/owner/certificate";
    public static final String nodeCertUri = "http://localhost:8080/node/certificate";

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

    public static Dataset prepareTestDatasetFromNamedGraphs(String resourceFile, final String[] graphNames)
                    throws IOException {
        // read dataset with created atom
        InputStream is = TestSigningUtils.class.getResourceAsStream(resourceFile);
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        Dataset testDataset = DatasetFactory.createGeneral();
        for (String name : graphNames) {
            testDataset.addNamedModel(name, dataset.getNamedModel(name));
        }
        testDataset.getDefaultModel().setNsPrefixes(dataset.getDefaultModel().getNsPrefixMap());
        return testDataset;
    }

    public static Dataset prepareTestDataset(String resourceFile) throws IOException {
        // read dataset with created atom
        InputStream is = TestSigningUtils.class.getResourceAsStream(resourceFile);
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        return dataset;
    }

    public static Set<String> getSubjects(Model model) {
        Set<String> subjs = new HashSet<>();
        StmtIterator sti = model.listStatements();
        while (sti.hasNext()) {
            Statement st = sti.next();
            String subj = st.getSubject().toString();
            subjs.add(subj);
        }
        return subjs;
    }

    public static Set<String> getUriResourceObjects(Model model) {
        Set<String> objs = new HashSet<>();
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

    /**
     * Not a test - but sometimes can be useful for generating test keys.
     *
     * @throws Exception
     */
    // @Test
    public void generateTestKeystore() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        File keysFile = new File("test-keys2.jks");
        FileBasedKeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        storeService.init();
        KeyPairService keyPairService = new KeyPairService();
        CertificateService certificateService = new CertificateService();
        addKeyByUri(atomCertUri, keyPairService, certificateService, storeService);
        addKeyByUri(ownerCertUri, keyPairService, certificateService, storeService);
        addKeyByUri(nodeCertUri, keyPairService, certificateService, storeService);
    }

    /**
     * Not a test - but sometimes can be useful for generating test keys.
     *
     * @throws Exception
     */
    // @Test
    public void generateKeystoreForNodeAndOwner() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // KeyStoreService storeServiceOnNode = new KeyStoreService(new
        // File("node-keys.jks"));
        FileBasedKeyStoreService storeServiceOnOwner = new FileBasedKeyStoreService(new File("owner-keys.jks"), "temp");
        storeServiceOnOwner.init();
        // KeyStoreService storeServiceOnMatcher = new KeyStoreService(new
        // File("matcher-keys.jks"));
        KeyPairService keyPairService = new KeyPairService();
        CertificateService certificateService = new CertificateService();
        // addKeyByUris(new String[]{
        // "http://rsa021.researchstudio.at:8080/won/resource",
        // "http://sat016.researchstudio.at:8080/won/resource",
        // "http://localhost:8080/won/resource"},
        // keyPairService, certificateService, storeServiceOnNode);
        addKeyByUris(new String[] { "http://rsa021.researchstudio.at:8080/owner/rest/keys",
                        "http://sat016.researchstudio.at:8080/owner/rest/keys",
                        "http://localhost:8080/owner/rest/keys" }, keyPairService, certificateService,
                        storeServiceOnOwner);
        // addKeyByUris(new String[]{
        // "http://sat001.researchstudio.at:8080/matcher/resource",
        // "http://localhost:8080/matcher/resource"},
        // keyPairService, certificateService, storeServiceOnMatcher);
    }

    public void printCerts() throws IOException, CertificateException {
        // load public keys:
        File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
        KeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        printCerificate(storeService, atomCertUri, atomCertUri);
        printCerificate(storeService, ownerCertUri, ownerCertUri);
        printCerificate(storeService, ownerCertUri, nodeCertUri);
    }

    private void printCerificate(final KeyStoreService storeService, final String keyName, final String certUri)
                    throws IOException, CertificateException {
        System.out.println(keyName);
        System.out.println(certUri);
        X509Certificate cert = (X509Certificate) storeService.getCertificate(keyName);
        StringWriter sw = new StringWriter();
        JcaPEMWriter writer = new JcaPEMWriter(sw);
        writer.writeObject(cert);
        writer.close();
        System.out.println(sw.toString());
        PEMParser pemParser = new PEMParser(new StringReader(sw.toString()));
        X509CertificateHolder certHolder = (X509CertificateHolder) pemParser.readObject();
        X509Certificate certRead = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
        System.out.println(certRead.toString());
    }

    private static void addKeyByUri(String certUri, final KeyPairService keyPairService,
                    final CertificateService certificateService, final KeyStoreService storeService)
                    throws IOException {
        KeyPair keyPair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
        BigInteger serialNumber = BigInteger.valueOf(1);
        Certificate cert = certificateService.createSelfSignedCertificate(serialNumber, keyPair, certUri, certUri);
        storeService.putKey(certUri, keyPair.getPrivate(), new Certificate[] { cert }, false);
        logger.debug("Adding for uri {} certificate {}", certUri, cert);
        // KeyInformationExtractorBouncyCastle extractor = new
        // KeyInformationExtractorBouncyCastle();
    }

    private static void addKeyByUris(final String[] aliasUris, final KeyPairService keyPairService,
                    final CertificateService certificateService, final KeyStoreService storeService)
                    throws IOException {
        KeyPair keyPair = keyPairService.generateNewKeyPairInBrainpoolp384r1();
        BigInteger serialNumber = BigInteger.valueOf(1);
        for (String aliasUri : aliasUris) {
            Certificate cert = certificateService.createSelfSignedCertificate(serialNumber, keyPair, aliasUri,
                            aliasUri);
            storeService.putKey(aliasUri, keyPair.getPrivate(), new Certificate[] { cert }, false);
        }
    }

    public static void writeToTempFile(final Dataset testDataset) throws IOException {
        File outFile = File.createTempFile("won", ".trig");
        logger.debug("Check output in temp file: " + outFile);
        OutputStream os = new FileOutputStream(outFile);
        RDFDataMgr.write(os, testDataset, RDFFormat.TRIG.getLang());
        os.close();
    }
    // generate key pair
    // CryptographyService crypService = new CryptographyService();
    // KeyPair keyPair = crypService.createNewAtomKeyPair(URI.create(ATOM_URI));
    // KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    // kpg.initialize(2048);
    // KeyPair keyPair = kpg.genKeyPair();
    // PrivateKey privateKey = keyPair.getPrivate();
    // PublicKey publicKey = keyPair.getPublic();
}
