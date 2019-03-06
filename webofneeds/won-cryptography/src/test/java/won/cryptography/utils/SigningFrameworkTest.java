package won.cryptography.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusReader;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;

/**
 * Created by ypanchenko on 18.06.2014.
 */
public class SigningFrameworkTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String[] SIGNING_FW_TEST_FILES =
            new String[]{
                    "/example_01.trig", "/example_02.trig", "/example_03.trig",
                    // nested examples cannot be read by Jena
                    //"/example_05.trig"
                   // "/example_nested.trig"
            };
    private static final String SIGNING_FW_TEST_1_FILE_SIGNED = "/example_01-signed.trig";

    private static final String RESOURCE_FILE = "/test_12_content_cupboard_45_45_15.ttl";
    private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";


    // read write from signingframework by signingframework produces orig input?
    // yes
    @Test
    public void readWriteGcTest() throws Exception {
        String inFile = SigningFrameworkTest.class.getResource(SIGNING_FW_TEST_1_FILE_SIGNED).getPath();

        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");
        System.out.println(outFile);

        GraphCollection gc = TriGPlusReader.readFile(inFile);
        TriGPlusWriter.writeFile(gc, outFile.getAbsolutePath());

        //outFile.delete();
        //TODO chng to testFolder and assert equal graphs?

    }

    // read of signingframework unsigned examples by jena works? and write produces orig input?
    // yes, but the triples not enclosed in a graph are written back inside unnamed (default) graph
    // (this is in accordance with TriG spec) and blank node is replaced by anonymous object
    // (this is also OK)
    @Test
    public void readWriteDatasetExampleTests() throws Exception {

        for (String testFile : SIGNING_FW_TEST_FILES) {
            InputStream is = SigningFrameworkTest.class.getResourceAsStream(testFile);

            //File outFile = testFolder.newFile();
            File outFile = File.createTempFile("won", ".trig");
            System.out.println(outFile);

            //this creates a default model out of the input graph
            Dataset dataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
            is.close();
            //Dataset dataset = RDFDataMgr.loadDataset(SigningFrameworkTest.class.getResource(SIGNING_FW_TEST_FILE).getPath());

            OutputStream os = new FileOutputStream(outFile);
            RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
            os.close();

            InputStream is2 = new FileInputStream(outFile);
            Dataset dataset2 = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
            is2.close();

            Iterator<String> datasetIterator = dataset.listNames();
            Model dfModel = dataset.getDefaultModel();
            //StmtIterator si = dfModel.listStatements();
            //System.out.println(si.hasNext());

            Assert.assertTrue(dataset.getDefaultModel().isIsomorphicWith(dataset2.getDefaultModel()));

            // in this case there are no named graphs
            while (datasetIterator.hasNext()) {
                String name = datasetIterator.next();
                System.out.println("name=" + name);
                Assert.assertTrue(dataset.getNamedModel(name).isIsomorphicWith(dataset2.getNamedModel(name)));
            }

            //outFile.delete();
            //TODO chng to testFolder and assert equal graphs?
        }
    }


    // read of signingframework signed examples by jena works? and write produces orig input?
    // Yes, but if the graph name is not a blank node (example_05.trig was therefore modified
    // accordingly), and without nested graph (i.e only one iteration of signing can apply).
    @Test
    public void readWriteDatasetSignedTest() throws Exception {
        InputStream is = SigningFrameworkTest.class.getResourceAsStream(SIGNING_FW_TEST_1_FILE_SIGNED);

        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");
        System.out.println(outFile);

        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());

        //Dataset dataset = RDFDataMgr.loadDataset(SigningFrameworkTest.class.getResource(SIGNING_FW_TEST_FILE).getPath());

        RDFDataMgr.write(new FileOutputStream(outFile), dataset, RDFFormat.TRIG.getLang());

        Dataset dataset2 = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset2, new FileInputStream(outFile), RDFFormat.TRIG.getLang());


        Assert.assertTrue(dataset.getDefaultModel().isIsomorphicWith(dataset2.getDefaultModel()));

        Iterator<String> datasetIterator = dataset.listNames();
        while (datasetIterator.hasNext()) {
            String name = datasetIterator.next();
            System.out.println("name=" + name);
            Assert.assertTrue(dataset.getNamedModel(name).isIsomorphicWith(dataset2.getNamedModel(name)));
        }


        //outFile.delete();
        //TODO chng to testFolder and assert equal graphs?
    }


    // read of webofneeds example by signingframework works?

    // read of webofneeds examples converted by jena to trig and read by signingframework works?


}
