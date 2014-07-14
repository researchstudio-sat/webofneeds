package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.SignatureData;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class WonAssemplerTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    // graph named :#NAME cannot be read (syntax error) by Jena but is read by Signingframework.
    private static final String RESOURCE_FILE = "/test_1_graph.trig";
    private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";


    @Test
    public void readDataWithOneGraphAndAssembleToDataWithSignature() throws Exception {
        String inFile = this.getClass().getResource(RESOURCE_FILE).getPath();

        //TODO change all temp file creation to the one here,
        //so that it is deleted after testing
        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");


        // The reader cannot reproduce the correct graph structure
        // has problems with blank nodes [] parts.
        //GraphCollection gc = TriGPlusReader.readFile(inFile);

        // create dataset with one named graph and convert it into GraphCollection
        InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);
        Dataset dataset = DatasetFactory.createMem();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        Map<String, String> pm = dataset.getDefaultModel().getNsPrefixMap();
      String modelName = dataset.listNames().next();
        Model model1 = dataset.getNamedModel(modelName);
        GraphCollection gc = ModelConverter.modelToGraphCollection(modelName, dataset);

        SignatureData mockSigData = createMockSignature();

        gc.setSignature(mockSigData);

        WonAssembler.assemble(gc, "SIG");

        Assert.assertEquals(2, gc.getGraphs().size());

        Assert.assertTrue(WonAssembler.SIG_GRAPH_NAME_TEMP.equals(gc.getGraphs().get(0).getName())
        || WonAssembler.SIG_GRAPH_NAME_TEMP.equals(gc.getGraphs().get(1).getName()));

        TriGPlusWriter.writeFile(gc, outFile.getAbsolutePath());
        System.out.println(outFile);

    }

    @Test
    public void assembleIntoDatasetTest() throws Exception {

        // create dataset with one named graph and convert it into GraphCollection
        InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);
        Dataset dataset = DatasetFactory.createMem();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        Map<String, String> pm = dataset.getDefaultModel().getNsPrefixMap();
      String modelName = dataset.listNames().next();
        Model model1 = dataset.getNamedModel(modelName);
        GraphCollection gc = ModelConverter.modelToGraphCollection(modelName, dataset);

        // create mock signature for the named graph from that GraphCollection
        SignatureData mockSigData = createMockSignature();
        gc.setSignature(mockSigData);

        // assemble the signature into the original dataset
        WonAssembler.assemble(gc, "no:uri#OWN1", dataset);

        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");
        System.out.println(outFile);
        OutputStream os = new FileOutputStream(outFile);
        RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
        os.close();

        Assert.assertTrue(dataset.getDefaultModel().getProperty("http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#", "hasSignatureValue") != null);

    }

    private SignatureData createMockSignature() throws NoSuchAlgorithmException {

        SignatureData mockSigData = new SignatureData();
        mockSigData.setHash(new BigInteger(new byte[]{1,2,3,4,5,6,7,8}));
        mockSigData.setSignature("\"blahblahSignature\"");
        mockSigData.setDigestGen(MessageDigest.getInstance("sha-256"));
        mockSigData.setCanonicalizationMethod("blahblahCanonicalizationMethod");
        mockSigData.setGraphDigestMethod("blahblahGraphDigestMethod");
        mockSigData.setSerializationMethod("blahblahSerializationmethod");
        mockSigData.setSignatureMethod("blahblahSigMathod");
        mockSigData.setVerificationCertificate("\"blahblahVerificationCertificate\"");

        return mockSigData;
    }
}
