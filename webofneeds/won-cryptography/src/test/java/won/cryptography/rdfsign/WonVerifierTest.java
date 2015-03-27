package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.cryptography.service.KeyStoreService;
import won.cryptography.utils.TestSigningUtils;
import won.protocol.message.SignatureReference;

import java.io.File;
import java.security.PublicKey;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 14.07.2014
 */
public class WonVerifierTest
{

  private static final String RESOURCE_FILE = "/won-signed-messages/create-need-msg.trig";

  private static final String NEED_CORE_DATA_URI =
    "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";
  private static final String NEED_CORE_DATA_SIG_URI =
    "http://localhost:8080/won/resource/need/3144709509622353000/core/#data-sig";
  private static final String EVENT_ENV1_URI =
    "http://localhost:8080/won/resource/event/7719577021233193000#data";
  private static final String EVENT_ENV1_SIG_URI =
    "http://localhost:8080/won/resource/event/7719577021233193000#data-sig";
  private static final String EVENT_ENV2_URI =
    "http://localhost:8080/won/resource/event/7719577021233193000#envelope-s7gl";
  private static final String EVENT_ENV2_SIG_URI =
    "http://localhost:8080/won/resource/event/7719577021233193000#envelope-s7gl-sig";


  Map<String,PublicKey> pubKeysMap = new HashMap<String,PublicKey>();

  @Before
  public void init() {
    Security.addProvider(new BouncyCastleProvider());

    //load public  keys:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile);

    // TODO load public keys from certificate referenced from signatures
    String needCertUri = "http://localhost:8080/won/resource/need/3144709509622353000/#certificate";
    String ownerCertUri = "http://localhost:8080/owner/certificate";
    String nodeCertUri = "http://localhost:8080/node/certificate";

    pubKeysMap.put(needCertUri, storeService.getCertificate(TestSigningUtils.NEED_KEY_NAME).getPublicKey());
    pubKeysMap.put(ownerCertUri, storeService.getCertificate(TestSigningUtils.OWNER_KEY_NAME).getPublicKey());
    pubKeysMap.put(nodeCertUri, storeService.getCertificate(TestSigningUtils.NODE_KEY_NAME).getPublicKey());
  }


  @Test
  public void testVerifyCreateNeedData() throws Exception {

    // create dataset that contains need core data graph and its signature graph
    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(
      RESOURCE_FILE, new String[]{NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI});


    // verify
    WonVerifier verifier = new WonVerifier(testDataset);
    // TODO load public keys from certificate referenced from signatures
    boolean verified = verifier.verify(pubKeysMap);
    SignatureVerificationResult result = verifier.getVerificationResult();

    Assert.assertTrue(verified);
    Assert.assertEquals(1, result.getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));
    Assert.assertEquals(1, result.getVerifiedUnreferencedSignaturesAsReferences().size());
    SignatureReference ref = result.getVerifiedUnreferencedSignaturesAsReferences().get(0);
    Assert.assertEquals(null, ref.getReferencerGraphUri());
    Assert.assertEquals(NEED_CORE_DATA_SIG_URI, ref.getSignatureGraphUri());
    Assert.assertEquals(NEED_CORE_DATA_URI, ref.getSignedGraphUri());

    // modify a model and check that it does not verify..
    Model m = testDataset.getNamedModel(NEED_CORE_DATA_URI);
    Statement stmt = m.listStatements().nextStatement();
    m.remove(stmt);

    verifier = new WonVerifier(testDataset);
    verified = verifier.verify(pubKeysMap);
    result = verifier.getVerificationResult();

    Assert.assertTrue(!verified);
    Assert.assertEquals(1, result.getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));

    // add the removed statement back
    m.add(stmt);
    verifier = new WonVerifier(testDataset);
    verified = verifier.verify(pubKeysMap);
    // now it should verify again
    Assert.assertTrue(verified);

  }

  @Test
  public void testVerifyCreatedNeedOwnerEvent() throws Exception {

    // create dataset that contains need core data graph and its signature graph,
    // envelope created by owner and the envelope's signature
    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(
      RESOURCE_FILE, new String[]{
        NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI,
        EVENT_ENV1_URI, EVENT_ENV1_SIG_URI
      });
    // verify
    WonVerifier verifier = new WonVerifier(testDataset);
    // TODO load public keys from certificate referenced from signatures
    boolean verified = verifier.verify(pubKeysMap);
    SignatureVerificationResult result = verifier.getVerificationResult();

    Assert.assertTrue(verified);
    Assert.assertEquals(2, verifier.getVerificationResult().getSignatureGraphNames().size());
    Assert.assertEquals(1, result.getVerifiedUnreferencedSignaturesAsReferences().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, verifier.getVerificationResult().getSignedGraphName(NEED_CORE_DATA_SIG_URI));
    Assert.assertEquals(EVENT_ENV1_URI, verifier.getVerificationResult().getSignedGraphName(EVENT_ENV1_SIG_URI));

  }


  @Test
  public void testVerifyCreatedNeedNodeEvent() throws Exception {

    // create dataset that contains need core data graph and its signature graph,
    // envelope created by owner and the envelope's signature, envelope created
    // by node and its signature
    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(
      RESOURCE_FILE, new String[]{
        NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI,
        EVENT_ENV1_URI, EVENT_ENV1_SIG_URI,
        EVENT_ENV2_URI, EVENT_ENV2_SIG_URI
      });
    // verify
    WonVerifier verifier = new WonVerifier(testDataset);
    // TODO load public keys from certificate referenced from signatures
    boolean verified = verifier.verify(pubKeysMap);

    Assert.assertTrue(verified);
    Assert.assertEquals(3, verifier.getVerificationResult().getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, verifier.getVerificationResult().getSignedGraphName(NEED_CORE_DATA_SIG_URI));
    Assert.assertEquals(EVENT_ENV1_URI, verifier.getVerificationResult().getSignedGraphName(EVENT_ENV1_SIG_URI));
    Assert.assertEquals(EVENT_ENV2_URI, verifier.getVerificationResult().getSignedGraphName(EVENT_ENV2_SIG_URI));

  }

  // TODO test more versions of not valid signatures, e.g. signatures missing, graphs missing,
  // wrong signature value, references signature values are wrong, etc.
}


