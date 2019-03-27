package won.cryptography.rdfsign;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.junit.Assert;
import org.junit.Test;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import won.cryptography.utils.TestSigningUtils;
import won.protocol.util.RdfUtils;

/**
 * Created by ypanchenko on 23.03.2015.
 */
public class WonAssemblerTest {
    private static final String RESOURCE_FILE = "/won-signed-messages/create-need-msg.trig";
    private static final String NEED_CORE_DATA_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data";
    private static final String NEED_CORE_DATA_SIG_URI = "http://localhost:8080/won/resource/need/3144709509622353000/core/#data-sig";

    @Test
    public void testAssembleOneGraphSignature() throws Exception {
        // The Signingframework reader cannot reproduce the correct graph
        // structure, it has problems with blank nodes [] parts.
        // GraphCollection gc = TriGPlusReader.readFile(inFile);
        // create dataset that contains need core data graph
        Dataset testDataset = TestSigningUtils.prepareTestDatasetFromNamedGraphs(RESOURCE_FILE,
                        new String[] { NEED_CORE_DATA_URI });
        // convert to graph collection
        GraphCollection gc = ModelConverter.modelToGraphCollection(NEED_CORE_DATA_URI, testDataset);
        // create mock signature
        SignatureData mockSigData = createMockSignature();
        gc.setSignature(mockSigData);
        // test assemble()
        WonAssembler.assemble(gc, testDataset, NEED_CORE_DATA_SIG_URI);
        // use for debugging output
        // TestSigningUtils.writeToTempFile(testDataset);
        // extract names of the named graphs
        List<String> namesList = RdfUtils.getModelNames(testDataset);
        // do some checks to make sure there is 1 signed names graph
        Assert.assertEquals("should be one named graph with data and one named graph with signature", 2,
                        namesList.size());
        Assert.assertTrue("should be some triples in signature graph",
                        testDataset.getNamedModel(NEED_CORE_DATA_SIG_URI).listStatements().hasNext());
        Assert.assertTrue("should be no triples in default graph",
                        !testDataset.getDefaultModel().listStatements().hasNext());
        int triplesCounter = TestSigningUtils
                        .countTriples(testDataset.getNamedModel(NEED_CORE_DATA_SIG_URI).listStatements());
        Set<String> subjs = TestSigningUtils.getSubjects(testDataset.getNamedModel(NEED_CORE_DATA_SIG_URI));
        Set<String> objs = TestSigningUtils.getUriResourceObjects(testDataset.getNamedModel(NEED_CORE_DATA_SIG_URI));
        Assert.assertEquals("signature graph should contain 11 triples", 11, triplesCounter);
        Assert.assertTrue("signed graph name should be an object in signature triples",
                        objs.contains(NEED_CORE_DATA_URI));
        Assert.assertTrue("signature graph name should be a subject in signature triples",
                        subjs.contains(NEED_CORE_DATA_SIG_URI));
    }

    private SignatureData createMockSignature() throws NoSuchAlgorithmException {
        SignatureData mockSigData = new SignatureData();
        mockSigData.setHash(new BigInteger(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }));
        mockSigData.setSignature("\"blahblahSignature\"");
        mockSigData.setDigestGen(MessageDigest.getInstance("sha-256"));
        mockSigData.setCanonicalizationMethod("blahblahCanonicalizationMethod");
        mockSigData.setGraphDigestMethod("blahblahGraphDigestMethod");
        mockSigData.setSerializationMethod("blahblahSerializationmethod");
        mockSigData.setSignatureMethod("blahblahSigMathod");
        // mockSigData.setVerificationCertificateUri("\"blahblahVerificationCertificate\"");
        mockSigData.setVerificationCertificate("<http://localhost:8080/blahblah/certificate>");
        return mockSigData;
    }
}
