package won.cryptography.rdfsign;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusReader;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class WonAssemplerTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    // test_1_graph.trig cannot be read (syntax error) by Jena but is read by
    // Signingframework. While test_2_graph.trig cannot be read (syntax error)
    // by Signingframework but is read by Jena. Therefore, for testing purposes,
    // both versions are used depending on what makes more sense for testing case
    private static final String RESOURCE_FILE = "/test_1_graph.trig";
    private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";


    @Test
    public void readDataWithOneGraphAndAssembleToDataWithSignature() throws Exception {
        String inFile = this.getClass().getResource(RESOURCE_FILE).getPath();

        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");


        // The reader cannot reproduce the correct graph structure
        // has problems with blank nodes [] parts.
        GraphCollection gc = TriGPlusReader.readFile(inFile);

        SignatureData mockSigData = new SignatureData();
        mockSigData.setHash(new BigInteger(new byte[]{1,2,3,4,5,6,7,8}));
        mockSigData.setSignature("blahblahSignature");
        mockSigData.setDigestGen(MessageDigest.getInstance("sha-256"));
        mockSigData.setCanonicalizationMethod("blahblahCanonicalizationMethod");
        mockSigData.setGraphDigestMethod("blahblahGraphDigestMethod");
        mockSigData.setSerializationMethod("blahblahSerializationmethod");
        mockSigData.setSignatureMethod("blahblahSigMathod");
        mockSigData.setVerificationCertificate("blahblahVerificationCertificate");

        gc.setSignature(mockSigData);

        WonAssembler.assemble(gc, "SIG");

        Assert.assertEquals(3, gc.getGraphs().size());

        Assert.assertTrue(WonAssembler.SIG_GRAPH_NAME_TEMP.equals(gc.getGraphs().get(0).getName())
        || WonAssembler.SIG_GRAPH_NAME_TEMP.equals(gc.getGraphs().get(1).getName())
        || WonAssembler.SIG_GRAPH_NAME_TEMP.equals(gc.getGraphs().get(2).getName()));

        TriGPlusWriter.writeFile(gc, outFile.getAbsolutePath());
        System.out.println(outFile);

    }
}
