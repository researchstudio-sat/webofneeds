package won.utils.goals.blending;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.utils.goals.GraphBlendingIterator;

public class GraphBlendingTest {

    private static final String baseFolder = "/won/utils/goals/blending/";

    @BeforeClass
    public static void setLogLevel() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
    }

    @Test
    public void blendSameTriples() throws IOException {
        Dataset ds = loadDataset(baseFolder + "same.trig");
        test(ds);
    }

    @Test
    public void blendLiterals() throws IOException {
        Dataset ds = loadDataset(baseFolder + "literals.trig");
        test(ds);
    }

    @Test
    public void blendDouble() throws IOException {
        Dataset ds = loadDataset(baseFolder + "double.trig");
        test(ds);
    }

    @Test
    public void blendURIs() throws IOException {
        Dataset ds = loadDataset(baseFolder + "/uris.trig");
        test(ds);
    }

    // Recursive blending is not implemented yet
    // @Test
    // public void blendRecursive() throws IOException {
    // Dataset ds = loadDataset(baseFolder + "recursive.trig");
    // test(ds);
    // }

    @Test
    public void blendDifferentLiterals() throws IOException {
        Dataset ds = loadDataset(baseFolder + "differentLiterals.trig");
        test(ds);
    }

    @Test
    public void blendDifferentURIs() throws IOException {
        Dataset ds = loadDataset(baseFolder + "differentUris.trig");
        test(ds);
    }

    @Test
    public void blendDifferentProperties() throws IOException {
        Dataset ds = loadDataset(baseFolder + "differentProperties.trig");
        test(ds);
    }

    @Test
    public void blendEmpty() throws IOException {

        GraphBlendingIterator blendingIterator = new GraphBlendingIterator(ModelFactory.createDefaultModel(),
                ModelFactory.createDefaultModel(), "http://example.org/test", "http://example.org/test/blended");

        Assert.assertTrue(blendingIterator.hasNext());
        Assert.assertTrue(ModelFactory.createDefaultModel().isIsomorphicWith(blendingIterator.next()));
        Assert.assertFalse(blendingIterator.hasNext());
    }

    @Test
    public void blendPreserve() throws IOException {
        Dataset ds = loadDataset(baseFolder + "preserve.trig");
        test(ds);
    }

    @Test
    public void blendVariables() throws IOException {
        Dataset ds = loadDataset(baseFolder + "variables.trig");
        test(ds);
    }

    @Test
    public void blendMultiple1() throws IOException {
        Dataset ds = loadDataset(baseFolder + "multiple1.trig");
        test(ds);
    }

    @Test
    public void blendMultiple2() throws IOException {
        Dataset ds = loadDataset(baseFolder + "multiple2.trig");
        test(ds);
    }

    public void test(Dataset ds) {

        // extract the two input models
        BlendingTestModelWrapper m1 = new BlendingTestModelWrapper(ds.getNamedModel("http://example.org/test/data1"));
        BlendingTestModelWrapper m2 = new BlendingTestModelWrapper(ds.getNamedModel("http://example.org/test/data2"));

        // extract all the expected blending result models
        List<BlendingTestModelWrapper> expectedModels = new LinkedList<>();
        Iterator<String> iter = ds.listNames();
        while (iter.hasNext()) {
            String name = iter.next();
            if (name.startsWith("http://example.org/test/blendedGraph")) {
                expectedModels.add(new BlendingTestModelWrapper(ds.getNamedModel(name)));
            }
        }

        // execute the blending
        GraphBlendingIterator blendingIterator = new GraphBlendingIterator(m1.getModel(), m2.getModel(),
                "http://example.org/test", "http://example.org/test/blended");
        Assert.assertTrue(blendingIterator.hasNext());

        // check that all blended graphs exist in the set of expected graphs
        int actualSize = 0;
        while (blendingIterator.hasNext()) {
            BlendingTestModelWrapper actual = new BlendingTestModelWrapper(blendingIterator.next());

            if (!expectedModels.contains(actual)) {
                System.out.println("Model not found in expected models: ");
                actual.getModel().write(System.out, "TTL");
            }

            Assert.assertTrue(expectedModels.contains(actual));
            actualSize++;
        }

        // check that the blended graph set has the same size as the expected graph set
        Assert.assertEquals(expectedModels.size(), actualSize);
    }

    private Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = getClass().getResourceAsStream(path);
            dataset = DatasetFactory.create();
            RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return dataset;
    }

    /**
     * Model wrapper class for easier comparison of expected and actual blending models.
     */
    class BlendingTestModelWrapper {

        private Model model;

        public BlendingTestModelWrapper(Model m) {
            model = m;
        }

        public Model getModel() {
            return model;
        }

        public boolean equals(Object obj) {

            if (!(obj instanceof BlendingTestModelWrapper)) {
                return false;
            }

            BlendingTestModelWrapper other = (BlendingTestModelWrapper) obj;
            return model.isIsomorphicWith(other.getModel());
        }
    }
}
