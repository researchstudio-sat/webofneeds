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
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.impl.SignatureAddingWonMessageProcessor;
import won.protocol.message.processor.impl.SignatureCheckingWonMessageProcessor;
import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko Date: 25.03.2015
 */
public class VerifyAndSignExamples {
    private static final String RESOURCE_FILE = "/won-signed-messages/create-atom-msg.trig";
    private static final String ATOM_URI = "http://localhost:8080/won/resource/atom/3144709509622353000";
    private static final String ATOM_CORE_DATA_URI = "http://localhost:8080/won/resource/atom/3144709509622353000/core/#data";
    private static final String ATOM_CORE_DATA_SIG_URI = "http://localhost:8080/won/resource/atom/3144709509622353000/core/#data-sig";
    private static final String EVENT_ENV1_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data";
    private static final String EVENT_ENV1_SIG_URI = "http://localhost:8080/won/resource/event/7719577021233193000#data-sig";
    private SignatureAddingWonMessageProcessor nodeAddingProcessor;
    private SignatureAddingWonMessageProcessor ownerAddingProcessor;
    private SignatureCheckingWonMessageProcessor checkingProcessor;

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
    /*
     * Owner Server receives create atom message from Owner Client, adds public key
     * (this step is omitted in the below example), and signs the message.
     */
    public void ownerCreateAtomMsg() throws Exception {
        // create dataset that contains atom core data graph
        Dataset inputDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
                        new String[] { ATOM_CORE_DATA_URI });
        // owner adds atom's public key - in this demo this step is omitted and we
        // assume
        // the key is already added - to avoid new key generation each time the demo is
        // run.
        // KeyForNewAtomAddingProcessor processor = new KeyForNewAtomAddingProcessor();
        // WonMessage inputMessage = atomKeyGeneratorAndAdder.process(inputMessage);
        // owner adds envelope
        WonMessage wonMessage = new WonMessageBuilder(URI.create(EVENT_ENV1_URI)).setSenderAtomURI(URI.create(ATOM_URI))
                        .addContent(inputDataset.getNamedModel(ATOM_CORE_DATA_URI))
                        .setWonMessageDirection(WonMessageDirection.FROM_OWNER).build();
        Dataset outputDataset = wonMessage.getCompleteDataset();
        Assert.assertEquals(2, RdfUtils.getModelNames(outputDataset).size());
        // write for debugging
        TestSigningUtils.writeToTempFile(outputDataset);
        // owner signs, - on behalf of atom
        WonMessage signedMessage = ownerAddingProcessor.processOnBehalfOfAtom(wonMessage);
        outputDataset = signedMessage.getCompleteDataset();
        // write for debugging
        // TestSigningUtils.writeToTempFile(outputDataset);
        Assert.assertEquals(4, RdfUtils.getModelNames(outputDataset).size());
        // pretend it was serialized and deserialized
        String datasetString = RdfUtils.writeDatasetToString(outputDataset, Lang.JSONLD);
        outputDataset = RdfUtils.readDatasetFromString(datasetString, Lang.JSONLD);
        WonMessage outputMessage = WonMessage.of(outputDataset);
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
    /*
     * Node receives create atom message, verifies it, if verification succeeds -
     * adds envelope that includes reference to verified signatures, and signs it.
     */
    public void nodeCreateAtomMsg() throws Exception {
        // create dataset that contains atom core data graph, envelope and its
        // signatures.
        // this is what nodes receives when the atom is created
        Dataset inputDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE, new String[] {
                        ATOM_CORE_DATA_URI, ATOM_CORE_DATA_SIG_URI, EVENT_ENV1_URI, EVENT_ENV1_SIG_URI, });
        WonMessage inputMessage = WonMessage.of(inputDataset);
        // node verifies the signature:
        WonMessage verifiedMessage = null;
        try {
            verifiedMessage = checkingProcessor.process(inputMessage);
        } catch (WonMessageProcessingException e) {
            Assert.fail("Signature verification failed");
        }
        // write for debugging
        // TestSigningUtils.writeToTempFile(outputDataset);
        // everyone should be able to verify this message, inculding when it was read
        // from RDF:
        String datasetString = RdfUtils.writeDatasetToString(inputMessage.getCompleteDataset(), Lang.TRIG);
        WonMessage outputMessage = WonMessage.of(RdfUtils.readDatasetFromString(datasetString, Lang.TRIG));
        try {
            checkingProcessor.process(outputMessage);
        } catch (WonMessageProcessingException e) {
            Assert.fail("Signature verification failed");
        }
    }
}
