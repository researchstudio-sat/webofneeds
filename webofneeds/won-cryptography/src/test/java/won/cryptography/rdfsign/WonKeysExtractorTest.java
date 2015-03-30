package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.cryptography.service.KeyStoreService;
import won.cryptography.utils.TestSigningUtils;

import java.io.File;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 27.03.2015
 */
public class WonKeysExtractorTest
{

  private static final String NEED_URI = "http://localhost:8080/won/resource/need/3144709509622353000";

  private static final String RESOURCE_FILE = "/won-signed-messages/create-need-msg.trig";
  private static final String NEED_FILE = "/won-signed-messages/need-core-nosig.trig";

  private static final String NEED_CORE_DATA_URI =
    "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";

  private ECPublicKey needKey;

  @Before
  public void init() {

    // TODO this should be inside the extractor??
    Security.addProvider(new BouncyCastleProvider());

    //load public key:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile);
    this.needKey = (ECPublicKey) storeService.getCertificate(TestSigningUtils.NEED_KEY_NAME).getPublicKey();

  }

  @Test
  public void testReadNeedPublicKey() throws Exception {

    // create dataset
    Dataset tempDataset = TestSigningUtils.prepareTestDataset(RESOURCE_FILE);
    // extract public keys
    Map<String,PublicKey> keys = WonKeysExtractor.getPublicKeys(tempDataset);
    Assert.assertEquals(1, keys.size());

//    // create dataset that contains need core data graph and its signature graph
//    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(
//      RESOURCE_FILE, new String[]{NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI});
//    // verify
//    WonVerifier verifier = new WonVerifier(testDataset);
//    boolean verified = verifier.verify(keys);
//    Assert.assertTrue(verified);

    // expected public key
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile);
    ECPublicKey expectedKey = (ECPublicKey) storeService.getCertificate(TestSigningUtils.NEED_KEY_NAME).getPublicKey();
    ECPublicKey constructedKey = (ECPublicKey) keys.get(NEED_URI);

    //KeyInformationExtractor info = new KeyInformationExtractorBouncyCastle();
    //Assert.assertTrue(info.getQX(expectedKey).equals(info.getQX(constructedKey)));
    //Assert.assertTrue(info.getQY(expectedKey).equals(info.getQY(constructedKey)));
    //Assert.assertTrue(info.getAlgorithm(expectedKey).equals(info.getAlgorithm(constructedKey)));

    ECParameterSpec specConsr = constructedKey.getParams();
    ECParameterSpec specExpec = expectedKey.getParams();
    Assert.assertEquals(specExpec.getCurve(), specConsr.getCurve());
    Assert.assertEquals(expectedKey.getW().getAffineX(), constructedKey.getW().getAffineX());
    Assert.assertEquals(expectedKey.getW().getAffineY(), constructedKey.getW().getAffineY());
    Assert.assertEquals(expectedKey.getAlgorithm(), constructedKey.getAlgorithm());

  }

  @Test
  public void testWriteNeedPublicKey() throws Exception {

    Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(NEED_FILE,
                                                                             new String[]{NEED_CORE_DATA_URI});

    Dataset datasetWithExpectedModel = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
                                                                             new String[]{NEED_CORE_DATA_URI});

    Model testModel = testDataset.getNamedModel(NEED_CORE_DATA_URI);
    Resource keySubj = testModel.createResource(NEED_URI);
    WonKeysExtractor.addPublicKeys(testModel, keySubj, needKey);

    Assert.assertTrue(testModel.isIsomorphicWith(datasetWithExpectedModel.getNamedModel(NEED_CORE_DATA_URI)));

    // write for debugging
    //TestSigningUtils.writeToTempFile(testDataset);

  }

}
