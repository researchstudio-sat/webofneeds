package won.utils.goals.blending;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class GraphBlendingTest {

    private static final String baseFolder = "/won/utils/goals/blending/";

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
    public void blendURIs() throws IOException {
        Dataset ds = loadDataset(baseFolder + "/uris.trig");
        test(ds);
    }

    @Test
    public void blendRecursive() throws IOException {
        Dataset ds = loadDataset(baseFolder + "recursive.trig");
        test(ds);
    }

    @Test
    public void blendDifferentLiterals() throws IOException {
        Dataset ds = loadDataset(baseFolder + "differentLiterals.trig");
        test(ds);
    }

    @Test
    public void blendDifferentURIs() throws IOException {
        Dataset ds = loadDataset(baseFolder +  "differentUris.trig");
        test(ds);
    }

    @Test
    public void blendDifferentProperties() throws IOException {
        Dataset ds = loadDataset(baseFolder + "differentProperties.trig");
        test(ds);
    }

    @Test
    public void blendEmpty() throws IOException {
        Dataset ds = loadDataset(baseFolder + "empty.trig");
        test(ds);
    }

    @Test
    public void blendMultiple() throws IOException {
        Dataset ds = loadDataset(baseFolder + "multiple.trig");

        Model m1 = ds.getNamedModel("http://example.org/test#data1");
        Model m2 = ds.getNamedModel("http://example.org/test#data2");
        Model actual = GraphBlending.blendSimple(m1, m2, "http://example.org/test#blended");
        Assert.assertEquals(1, actual.listStatements().toList().size());
    }


    // Not supported by simple graph blending
//    @Test
//    public void blendPreserve() throws IOException {
//        Dataset ds = loadDataset(baseFolder + "preserve.trig");
//        test(ds);
//    }

    public void test(Dataset ds) {

        Model m1 = ds.getNamedModel("http://example.org/test#data1");
        Model m2 = ds.getNamedModel("http://example.org/test#data2");
        Model expected = ds.getNamedModel("http://example.org/test#blended");

        // check that the actual blended graphs is the expected one
        Model actual = GraphBlending.blendSimple(m1, m2, "http://example.org/test#blended");
        actual.write(System.out, "TRIG");

        Assert.assertFalse(containSameStatements(m1, m2));
        Assert.assertTrue(containSameStatements(expected, actual));
    }

    private boolean containSameStatements(Model m1, Model m2) {

        for (Statement stmt1 : m1.listStatements().toList()) {
            if (!m2.contains(stmt1)) {
                return false;
            }
        }

        return (m1.listStatements().toList().size() == m2.listStatements().toList().size());
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
}
