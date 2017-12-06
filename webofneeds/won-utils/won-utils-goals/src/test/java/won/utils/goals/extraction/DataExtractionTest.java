package won.utils.goals.extraction;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class DataExtractionTest {

    private static final String baseFolder = "/won/utils/goals/extraction/";

    @Test
    public void playgroundExample() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset(baseFolder + "playground-example.trig");
        Model actual = ex.extract(ds);
        RDFDataMgr.write(System.out, actual, Lang.TRIG);
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void additionalNodeNotCoveredByShape() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape.trig");
        Model actual = ex.extract(ds);
        RDFDataMgr.write(System.out, actual, Lang.TRIG);
        Assert.assertTrue(actual.isEmpty());
    }

    @Test
    public void additionalNodeNotCoveredByShapeNoErrors() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape-noerrors.trig");
        Model expected =loadModel(baseFolder + "additional-node-not-covered-by-shape-noerrors-expected-result.trig");
        Model actual = ex.extract(ds);
        RDFDataMgr.write(System.out, actual, Lang.TRIG);
        Assert.assertTrue(actual.isIsomorphicWith(expected));
    }

    @Test
    public void additionalNodeNotCoveredByShapePersonClosed() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape-person-closed.trig");
        Model expected =loadModel(baseFolder + "additional-node-not-covered-by-shape-person-closed-expected-result.trig");
        Model actual = ex.extract(ds);
        RDFDataMgr.write(System.out, actual, Lang.TRIG);
        Assert.assertTrue(actual.isIsomorphicWith(expected));
    }

    @Test
    public void additionalNodeNotCoveredByShapePropoertyPath() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape-property-path.trig");
        Model expected =loadModel(baseFolder + "additional-node-not-covered-by-shape-property-path-expected-result.trig");
        Model actual = ex.extract(ds);
        RDFDataMgr.write(System.out, actual, Lang.TRIG);
        Assert.assertTrue(actual.isIsomorphicWith(expected));
    }

    @Test
    public void additionalNodeNotCoveredByShapePropoertyPathWithError() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape-property-path-with-error.trig");
        Model expected =loadModel(baseFolder + "additional-node-not-covered-by-shape-property-path-with-error-expected-result.trig");
        Model actual = ex.extract(ds);
        RDFDataMgr.write(System.out, actual, Lang.TRIG);
        Assert.assertTrue(actual.isIsomorphicWith(expected));
    }

    private Model loadModel(String path) throws IOException {
        InputStream is = null;
        Model model = null;
        try {
            is = getClass().getResourceAsStream(path);
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, is, RDFFormat.TRIG.getLang());
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return model;
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
