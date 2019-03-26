package won.cryptography.rdfsign;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import won.cryptography.utils.TestSigningUtils;
import won.cryptography.utils.TestingKeys;

/**
 * User: ypanchenko Date: 14.07.2014
 */
@Ignore
public class WonVerifierTest {

  private static final String RESOURCE_FILE = "/won-signed-messages/create-need-msg.trig";

  private static final String NEED_CORE_DATA_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";
  private static final String NEED_CORE_DATA_SIG_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data-sig";
  private static final String EVENT_ENV1_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data";
  private static final String EVENT_ENV1_SIG_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data-sig";
  private static final String EVENT_ENV2_URI = "http://localhost:8080/won/resource/event/7719577021233193000#envelope-s7gl";
  private static final String EVENT_ENV2_SIG_URI = "http://localhost:8080/won/resource/event/7719577021233193000#envelope-s7gl-sig";

  TestingKeys keys;

  @Before
  public void init() throws Exception {

    keys = new TestingKeys(TestSigningUtils.KEYS_FILE);
  }

  @Test
  public void testVerifyCreateNeedData() throws Exception {

    // create dataset that contains need core data graph and its signature graph
    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
        new String[] { NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI });

    // verify
    WonVerifier verifier = new WonVerifier(testDataset);
    // TODO load public keys from certificate referenced from signatures
    boolean verified = verifier.verify(keys.getPublicKeys());
    SignatureVerificationState result = verifier.getVerificationResult();

    Assert.assertTrue(result.getMessage(), verified);
    Assert.assertEquals(1, result.getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));

    // modify a model and check that it does not verify..
    Model m = testDataset.getNamedModel(NEED_CORE_DATA_URI);
    Statement stmt = m.listStatements().nextStatement();
    m.remove(stmt);

    verifier = new WonVerifier(testDataset);
    verified = verifier.verify(keys.getPublicKeys());
    result = verifier.getVerificationResult();

    Assert.assertTrue(!verified);
    Assert.assertEquals(1, result.getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));

    // add the removed statement back
    m.add(stmt);
    verifier = new WonVerifier(testDataset);
    verified = verifier.verify(keys.getPublicKeys());
    // now it should verify again
    Assert.assertTrue(verified);

  }

  @Test
  public void testVerifyCreatedNeedOwnerEvent() throws Exception {

    // create dataset that contains need core data graph and its signature graph,
    // envelope created by owner and the envelope's signature
    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
        new String[] { NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI, EVENT_ENV1_URI, EVENT_ENV1_SIG_URI });
    // verify
    WonVerifier verifier = new WonVerifier(testDataset);
    // TODO load public keys from certificate referenced from signatures
    boolean verified = verifier.verify(keys.getPublicKeys());
    SignatureVerificationState result = verifier.getVerificationResult();

    Assert.assertTrue(result.getMessage(), verified);
    Assert.assertEquals(2, result.getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));
    Assert.assertEquals(EVENT_ENV1_URI, result.getSignedGraphName(EVENT_ENV1_SIG_URI));

  }

  @Test
  public void testVerifyCreatedNeedNodeEvent() throws Exception {

    // create dataset that contains need core data graph and its signature graph,
    // envelope created by owner and the envelope's signature, envelope created
    // by node and its signature
    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
        new String[] { NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI, EVENT_ENV1_URI, EVENT_ENV1_SIG_URI, EVENT_ENV2_URI,
            EVENT_ENV2_SIG_URI });
    // verify
    WonVerifier verifier = new WonVerifier(testDataset);
    boolean verified = verifier.verify(keys.getPublicKeys());
    SignatureVerificationState result = verifier.getVerificationResult();

    Assert.assertTrue(result.getMessage(), verified);
    Assert.assertEquals(3, result.getSignatureGraphNames().size());
    Assert.assertEquals(NEED_CORE_DATA_URI, result.getSignedGraphName(NEED_CORE_DATA_SIG_URI));
    Assert.assertEquals(EVENT_ENV1_URI, result.getSignedGraphName(EVENT_ENV1_SIG_URI));
    Assert.assertEquals(EVENT_ENV2_URI, result.getSignedGraphName(EVENT_ENV2_SIG_URI));

  }

  // TODO test more versions of not valid signatures, e.g. signatures missing,
  // graphs missing,
  // wrong signature value, references signature values are wrong, etc.
}
