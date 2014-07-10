package won.cryptography.rdfsign;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.LinkedList;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class ModelConverterTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private static final String RESOURCE_FILE = "/test_2_graph.trig";
    private static final String RESOURCE_URI = "http://www.example.com/resource/need/12";


    @Test
    /**
     * Reads from TRIG with Jena API into Dataset 1, transforms one
     * named graph from that Dataset into Signingframework's API
     * GraphCollection and writes it with Signingframework's API,
     * reads the result with Jena API into Dataset 2, and checks
     * if the specified named graph model from Dataset 1 is
     * isomorphic with the same named graph model from Dataset 2.
     */
    public void modelToGraphCollectionTest() throws Exception {
        InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);

        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");
        System.out.println(outFile);

        //this creates a default model out of the input graph
        Dataset dataset = DatasetFactory.createMem();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();

        PrefixMapping pm = dataset.asDatasetGraph().getDefaultGraph().getPrefixMapping();
        Model model = dataset.getNamedModel("no:uri#OWN1");
        GraphCollection gc = ModelConverter.modelToGraphCollection("no:uri#OWN1", model, pm);

        TriGPlusWriter.writeFile(gc, outFile.getAbsolutePath());

        InputStream is2 = new FileInputStream(outFile);
        Dataset dataset2 = DatasetFactory.createMem();
        RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
        is2.close();

        Model model2 = dataset2.getNamedModel("no:uri#OWN1");


        File outFile2 = File.createTempFile("won", ".trig");
        System.out.println(outFile2);
        OutputStream os = new FileOutputStream(outFile2);
        RDFDataMgr.write(os, dataset2, RDFFormat.TRIG.getLang());
        os.close();

        //OutputStream os = new FileOutputStream(outFile);
        //RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
        //os.close();

        Assert.assertTrue(model.isIsomorphicWith(model2));
    }


    @Test
    public void namedGraphToModelTest() throws Exception {

        // create GraphCollection
        InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);

        //this creates a default model out of the input graph
        Dataset dataset = DatasetFactory.createMem();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();

        PrefixMapping pm = dataset.asDatasetGraph().getDefaultGraph().getPrefixMapping();
        Model model1 = dataset.getNamedModel("no:uri#OWN1");
        GraphCollection gc = ModelConverter.modelToGraphCollection("no:uri#OWN1", model1, pm);
        LinkedList<NamedGraph> graphs = gc.getGraphs();
        NamedGraph graph = null;
        for (NamedGraph g : graphs) {
            if (g.getName().equals(":OWN1")) {
                graph = g;
                break;
            }
        }
        File outFile0 = File.createTempFile("won", ".trig");
        System.out.println(outFile0);
        OutputStream os0 = new FileOutputStream(outFile0);

        TriGPlusWriter.writeFile(gc, outFile0.getAbsolutePath());
        os0.close();


        // test transformation
        Model model2 = ModelConverter.namedGraphToModel(graph, gc.getPrefixes());
        Dataset dataset2 = DatasetFactory.createMem();
        dataset2.addNamedModel("no:uri#OWN1", model2);
        //TODO chng the API so that the prefix map is taken care of in the converter
        dataset2.getDefaultModel().setNsPrefixes(dataset2.getNamedModel("no:uri#OWN1").getNsPrefixMap());

        //File outFile = testFolder.newFile();
        File outFile = File.createTempFile("won", ".trig");
        System.out.println(outFile);
        OutputStream os = new FileOutputStream(outFile);
        RDFDataMgr.write(os, dataset2, RDFFormat.TRIG.getLang());
        os.close();

        //OutputStream os = new FileOutputStream(outFile);
        //RDFDataMgr.write(os, dataset, RDFFormat.TRIG.getLang());
        //os.close();

        Assert.assertTrue(model1.isIsomorphicWith(model2));
    }
}
