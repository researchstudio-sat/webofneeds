package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import won.cryptography.rdfsign.WonKeysExtractor;
import won.cryptography.rdfsign.WonSigner;
import won.cryptography.rdfsign.WonVerifier;
import won.cryptography.service.KeyStoreService;
import won.cryptography.utils.TestSigningUtils;
import won.protocol.message.*;
import won.protocol.util.RdfUtils;

import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 25.03.2015
 */
public class VerifyAndSignExamples
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


  private PrivateKey needKey;
  private String needCertUri;
  private PrivateKey nodeKey;
  private String nodeCertUri;

  Map<String,PublicKey> pubKeysMap = new HashMap<String,PublicKey>();

  @Before
  public void init() {
    Security.addProvider(new BouncyCastleProvider());

    //load private and public keys:
    File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
    KeyStoreService storeService = new KeyStoreService(keysFile);

    this.needKey = (ECPrivateKey) storeService.getKey(TestSigningUtils.NEED_KEY_NAME);
    this.nodeKey = (ECPrivateKey) storeService.getKey(TestSigningUtils.NODE_KEY_NAME);

    // TODO load public keys from certificate referenced from signatures
    needCertUri = "http://localhost:8080/won/resource/need/3144709509622353000";
    nodeCertUri = "http://localhost:8080/node/certificate";

    //pubKeysMap.put(needCertUri, storeService.getCertificate(TestSigningUtils.NEED_KEY_NAME).getPublicKey());
    pubKeysMap.put(nodeCertUri, storeService.getCertificate(TestSigningUtils.NODE_KEY_NAME).getPublicKey());
  }


  @Test
  /**
   * Node receives create need message, verifies it, if verification succeeds -
   * adds envelope that includes reference to verified signatures, and signs it.
   */
  public void nodeCreateNeedMsg() throws Exception {

    // create dataset that contains need core data graph, envelope and its signatures.
    // this is what nodes receives when the need is created
    Dataset inputDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
                                                                             new String[]{NEED_CORE_DATA_URI,
                                                                                          NEED_CORE_DATA_SIG_URI,
                                                                                          EVENT_ENV1_URI,
                                                                                          EVENT_ENV1_SIG_URI,});

    // node extracts need's public keys
    Map<String,PublicKey> extractedKeys = WonKeysExtractor.getPublicKeys(inputDataset);
    for (String key : extractedKeys.keySet()) {
      pubKeysMap.put(key, extractedKeys.get(key));
    }

    // node then verifies the data it receives
    // TODO maybe it should store the public key of the created need to use it for
    // later communication with the same need uri
    WonVerifier verifier = new WonVerifier(inputDataset);
    boolean verified = verifier.verify(pubKeysMap);
    Assert.assertTrue(verified);


    // node then process the message in some way, and adds its own envelope,
    // the envelope should contain the reference to the verified signatures
    WonMessage inputWonMessage = WonMessageDecoder.decodeFromDataset(inputDataset);
    List<SignatureReference> refs = verifier.getVerificationResult().getVerifiedUnreferencedSignaturesAsReferences();
    WonMessage outputWonMessage = new WonMessageBuilder()
      .wrap(inputWonMessage)
      .setWonEnvelopeType(WonEnvelopeType.NodeToNode)
      .setSignatureReferences(refs)
      .build();
    Dataset outputDataset = WonMessageEncoder.encodeAsDataset(outputWonMessage);
    Assert.assertEquals(5, RdfUtils.getModelNames(outputDataset).size());

    // write for debugging
    TestSigningUtils.writeToTempFile(outputDataset);

    // node should then sign its envelope
    WonSigner signer = new WonSigner(outputDataset, new SignatureAlgorithmFisteus2010());
    signer.sign(nodeKey, nodeCertUri, outputWonMessage.getOuterEnvelopeGraphURI().toString());
    Assert.assertEquals(6, RdfUtils.getModelNames(outputDataset).size());

    // write for debugging
    TestSigningUtils.writeToTempFile(outputDataset);

  }
}
