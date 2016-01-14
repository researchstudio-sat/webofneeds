package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.cryptography.utils.TestSigningUtils;
import won.cryptography.utils.TestingKeys;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 27.03.2015
 */
public class WonKeysReaderWriterTest
{

  private static final String NEED_URI = "http://localhost:8080/won/resource/need/3144709509622353000";

  private static final String RESOURCE_FILE = "/won-signed-messages/create-need-msg.trig";
  private static final String NEED_FILE = "/won-signed-messages/need-core-nosig.trig";

  private static final String NEED_CORE_DATA_URI =
    "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";

  private TestingKeys keys;
  private WonKeysReaderWriter extractor;

  @Before
  public void init() {

    keys = new TestingKeys(TestSigningUtils.KEYS_FILE);
    extractor = new WonKeysReaderWriter();

  }

  @Test
  public void testReadNeedPublicKey() throws Exception {

    // create dataset
    Dataset tempDataset = TestSigningUtils.prepareTestDataset(RESOURCE_FILE);
    // extract public keys
    Map<String,PublicKey> constructedKeys = extractor.readFromDataset(tempDataset);
    Assert.assertEquals(1, constructedKeys.size());

    // expected public key
    ECPublicKey expectedKey = (ECPublicKey) keys.getPublicKey(TestSigningUtils.needCertUri);
    // reconstructed public key
    ECPublicKey constructedKey = (ECPublicKey) constructedKeys.get(NEED_URI);

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
    extractor.writeToModel(testModel, keySubj, keys.getPublicKey(TestSigningUtils.needCertUri));

    Assert.assertTrue(testModel.isIsomorphicWith(datasetWithExpectedModel.getNamedModel(NEED_CORE_DATA_URI)));

    // write for debugging
    //TestSigningUtils.writeToTempFile(testDataset);

  }

}
