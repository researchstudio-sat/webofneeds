package won.cryptography.message;

import java.io.File;
import java.net.URI;
import java.security.Security;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import won.cryptography.service.CryptographyService;
import won.cryptography.service.keystore.FileBasedKeyStoreService;
import won.cryptography.utils.TestSigningUtils;
import won.cryptography.utils.TestingDataSource;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.message.processor.impl.SignatureCheckingWonMessageProcessor;
import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko Date: 25.03.2015
 */
public class VerifyAndSignExamples {

    private static final String RESOURCE_FILE = "/won-signed-messages/create-need-msg.trig";

    private static final String NEED_URI = "http://localhost:8080/won/resource/need/3144709509622353000";

    private static final String NEED_CORE_DATA_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";
    private static final String NEED_CORE_DATA_SIG_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data-sig";

    private static final String EVENT_ENV1_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data";
    private static final String EVENT_ENV1_SIG_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data-sig";

    private static final String RESOURCE_FILE_NL = "/won-signed-messages/need-with-nl-nosig.trig";

    SignatureAddingWonMessageProcessor nodeAddingProcessor;
    SignatureAddingWonMessageProcessor ownerAddingProcessor;
    SignatureCheckingWonMessageProcessor checkingProcessor;

    @Before
    public void init() throws Exception {

        // initialize signature adding and signature checking processors:

        Security.addProvider(new BouncyCastleProvider());
        File keysFile = new File(this.getClass().getResource(TestSigningUtils.KEYS_FILE).getFile());
        FileBasedKeyStoreService storeService = new FileBasedKeyStoreService(keysFile, "temp");
        storeService.init();

        nodeAddingProcessor = new SignatureAddingWonMessageProcessor();
        CryptographyService cryptographyService = new CryptographyService(storeService, TestSigningUtils.ownerCertUri);

        nodeAddingProcessor.setCryptographyService(cryptographyService);

        ownerAddingProcessor = new SignatureAddingWonMessageProcessor();
        ownerAddingProcessor.setCryptographyService(cryptographyService);

        checkingProcessor = new SignatureCheckingWonMessageProcessor();
        checkingProcessor.setLinkedDataSource(new TestingDataSource());

    }

    @Test
    /**
     * Owner Server receives create need message from Owner Client, adds public key (this step is omitted in the below
     * example), and signs the message.
     */
    public void ownerCreateNeedMsg() throws Exception {

        // create dataset that contains need core data graph
        Dataset inputDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
                new String[] { NEED_CORE_DATA_URI });

        // owner adds need's public key - in this demo this step is omitted and we assume
        // the key is already added - to avoid new key generation each time the demo is run.
        // KeyForNewNeedAddingProcessor processor = new KeyForNewNeedAddingProcessor();
        // WonMessage inputMessage = needKeyGeneratorAndAdder.process(inputMessage);

        // owner adds envelope
        WonMessage wonMessage = new WonMessageBuilder(URI.create(EVENT_ENV1_URI)).setSenderNeedURI(URI.create(NEED_URI))
                .addContent(inputDataset.getNamedModel(NEED_CORE_DATA_URI))
                .setWonMessageDirection(WonMessageDirection.FROM_OWNER).build();
        Dataset outputDataset = wonMessage.getCompleteDataset();
        Assert.assertEquals(2, RdfUtils.getModelNames(outputDataset).size());

        // write for debugging
        TestSigningUtils.writeToTempFile(outputDataset);

        // owner signs, - on behalf of need
        WonMessage signedMessage = ownerAddingProcessor.processOnBehalfOfNeed(wonMessage);
        outputDataset = signedMessage.getCompleteDataset();

        // write for debugging
        // TestSigningUtils.writeToTempFile(outputDataset);
        Assert.assertEquals(4, RdfUtils.getModelNames(outputDataset).size());

        // pretend it was serialized and deserialized
        String datasetString = RdfUtils.writeDatasetToString(outputDataset, Lang.JSONLD);
        outputDataset = RdfUtils.readDatasetFromString(datasetString, Lang.JSONLD);
        WonMessage outputMessage = new WonMessage(outputDataset);

        // write for debugging
        // TestSigningUtils.writeToTempFile(outputDataset);

        // the receiver of this message should be able to verify it
        try {
            checkingProcessor.process(outputMessage);
            // if we got to here without exceptions - we were able to verify the signature
        } catch (WonMessageProcessingException e) {
            Assert.fail("Signature verification failed");
        }

    }

    @Test
    /**
     * Node receives create need message, verifies it, if verification succeeds - adds envelope that includes reference
     * to verified signatures, and signs it.
     */
    public void nodeCreateNeedMsg() throws Exception {

        // create dataset that contains need core data graph, envelope and its signatures.
        // this is what nodes receives when the need is created
        Dataset inputDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
                new String[] { NEED_CORE_DATA_URI, NEED_CORE_DATA_SIG_URI, EVENT_ENV1_URI, EVENT_ENV1_SIG_URI, });
        WonMessage inputMessage = new WonMessage(inputDataset);

        // node verifies the signature:
        WonMessage verifiedMessage = null;
        try {
            verifiedMessage = checkingProcessor.process(inputMessage);
        } catch (WonMessageProcessingException e) {
            Assert.fail("Signature verification failed");
        }

        // node then process the message in some way, and adds its own envelope,
        // the envelope should contain the reference to the verified signatures
        WonMessage nodeWonMessage = WonMessageBuilder.wrap(verifiedMessage)
                .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM).build();

        Dataset outputDataset = nodeWonMessage.getCompleteDataset();
        Assert.assertEquals(5, RdfUtils.getModelNames(outputDataset).size());
        // write for debugging
        // TestSigningUtils.writeToTempFile(outputDataset);

        // node should then sign its envelope
        WonMessage signedMessage = nodeAddingProcessor.process(nodeWonMessage);

        Assert.assertEquals(6, RdfUtils.getModelNames(outputDataset).size());
        // write for debugging
        // TestSigningUtils.writeToTempFile(outputDataset);

        // everyone should be able to verify this message, inculding when it was read from RDF:
        String datasetString = RdfUtils.writeDatasetToString(signedMessage.getCompleteDataset(), Lang.TRIG);
        WonMessage outputMessage = new WonMessage(RdfUtils.readDatasetFromString(datasetString, Lang.TRIG));

        try {
            checkingProcessor.process(outputMessage);
        } catch (WonMessageProcessingException e) {
            Assert.fail("Signature verification failed");
        }

    }

}
