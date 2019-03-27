package won.cryptography.rdfsign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.GraphCollection;
import de.uni_koblenz.aggrimm.icp.crypto.sign.graph.NamedGraph;
import de.uni_koblenz.aggrimm.icp.crypto.sign.trigplus.TriGPlusWriter;

/**
 * Created by ypanchenko on 09.07.2014.
 */
public class ModelConverterTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private static final String[] RESOURCE_FILES = new String[] { "/test_1_cupboard.trig", "/test_1_graph.trig",
                    "/test_1_cupboard_no_pref.trig" };

    @Test
    @Ignore
    /**
     * Reads from TRIG with Jena API into Dataset 1, transforms one named graph from
     * that Dataset into Signingframework's API GraphCollection and writes it with
     * Signingframework's API, reads the result with Jena API into Dataset 2, and
     * checks if the specified named graph model from Dataset 1 is isomorphic with
     * the same named graph model from Dataset 2.
     */
    public void modelToGraphCollectionTest() throws Exception {
        for (String resourceFile : RESOURCE_FILES) {
            // prepare the input Dataset containg the Model to be converted
            InputStream is = this.getClass().getResourceAsStream(resourceFile);
            File outFile = testFolder.newFile();
            // use this when debugging:
            // File outFile = File.createTempFile("won", ".trig");
            // System.out.println(outFile);
            Dataset dataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
            is.close();
            // test the convertion from the Model to the NamedGraph
            String modelName = dataset.listNames().next();
            Model model = dataset.getNamedModel(modelName);
            // the method to be tested
            GraphCollection gc = ModelConverter.modelToGraphCollection(modelName, dataset);
            TriGPlusWriter.writeFile(gc, outFile.getAbsolutePath(), false);
            // check that the resulting graph collection is a representation
            // of the converted model. For this, read the resulting graph collection
            // as a Model with Jena API
            InputStream is2 = new FileInputStream(outFile);
            Dataset dataset2 = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
            is2.close();
            Model model2 = dataset2.getNamedModel(modelName);
            File outFile2 = testFolder.newFile();
            // use this when debugging:
            // File outFile2 = File.createTempFile("won", ".trig");
            // System.out.println(outFile2);
            OutputStream os = new FileOutputStream(outFile2);
            RDFDataMgr.write(os, dataset2, RDFFormat.TRIG.getLang());
            os.close();
            // check that the model obtained from resulting graph collection is
            // a representation of the original converted model.
            Assert.assertTrue(model.listStatements().hasNext() && model2.listStatements().hasNext());
            Assert.assertTrue(model.isIsomorphicWith(model2));
        }
    }

    @Test
    @Ignore
    /**
     * Reads from TRIG with Jena API into Dataset 1, transforms one named Model from
     * that Dataset into Signingframework's API GraphCollection with one NamedGraph,
     * transforms (converts) that NamedGraph into Jena's Model, and checks if the
     * resulting Model is the same as original Model.
     */
    public void namedGraphToModelTest() throws Exception {
        for (String resourceFile : RESOURCE_FILES) {
            // prepare GraphCollection with NamedGraph to be converted:
            InputStream is = this.getClass().getResourceAsStream(resourceFile);
            Dataset dataset = DatasetFactory.createGeneral();
            RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
            is.close();
            String modelName = dataset.listNames().next();
            Model model1 = dataset.getNamedModel(modelName);
            // this method is not tested here and used just for input
            // generation and to make it easier Namedgraph<->Model comparison
            // (but it's tested in other method, see modelToGraphCollectionTest())
            GraphCollection gc = ModelConverter.modelToGraphCollection(modelName, dataset);
            LinkedList<NamedGraph> graphs = gc.getGraphs();
            String graphName = null;
            for (NamedGraph g : graphs) {
                if (!g.getName().isEmpty() && g.getName().contains(modelName)) {
                    graphName = g.getName();
                    break;
                }
            }
            // use this when debugging:
            // File outFile0 = File.createTempFile("won", ".trig");
            // System.out.println(outFile0);
            // OutputStream os0 = new FileOutputStream(outFile0);
            // TriGPlusWriter.writeFile(gc, outFile0.getAbsolutePath(), false);
            // os0.close();
            // test convert from NamedGraph of GraphCollection into Model
            Model model2 = ModelConverter.namedGraphToModel(graphName, gc);
            Dataset dataset2 = DatasetFactory.createGeneral();
            dataset2.addNamedModel(modelName, model2);
            // TODO maybe chng the API so that the prefix map is taken care of in the
            // converter:
            // if it makes sense from the the usage of this in Assembler point of view
            dataset2.getDefaultModel().setNsPrefixes(dataset2.getNamedModel(modelName).getNsPrefixMap());
            File outFile = testFolder.newFile();
            // use this when debugging:
            // File outFile = File.createTempFile("won", ".trig");
            // System.out.println(outFile);
            OutputStream os = new FileOutputStream(outFile);
            RDFDataMgr.write(os, dataset2, RDFFormat.TRIG.getLang());
            os.close();
            // make sure that the original Model that was used to generate test input
            // GraphCollection with NamedGraph is isomorphic with the Model after
            // conversion is applied:
            Assert.assertTrue(model1.listStatements().hasNext() && model2.listStatements().hasNext());
            Assert.assertTrue(model1.isIsomorphicWith(model2));
        }
    }
}
