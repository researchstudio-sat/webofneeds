package won.utils.goals.extraction;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class DataExtractionTest {

    @Test
    public void playgroundExample() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset("/won/utils/goals/extraction/playground-example.trig");
        Model result = ex.extract(ds);

        RDFDataMgr.write(System.out, result, Lang.TRIG);
    }

    @Test
    public void additionalNodeNotCoveredByShape() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset("/won/utils/goals/extraction/additional-node-not-covered-by-shape.trig");
        Model result = ex.extract(ds);

        RDFDataMgr.write(System.out, result, Lang.TRIG);
    }

    @Test
    public void additionalNodeNotCoveredByShapeNoErrors() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset("/won/utils/goals/extraction/additional-node-not-covered-by-shape-noerrors.trig");
        Model result = ex.extract(ds);

        RDFDataMgr.write(System.out, result, Lang.TRIG);
    }

    @Test
    public void additionalNodeNotCoveredByShapePersonClosed() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset("/won/utils/goals/extraction/additional-node-not-covered-by-shape-person-closed.trig");
        Model result = ex.extract(ds);

        RDFDataMgr.write(System.out, result, Lang.TRIG);
    }

    @Test
    public void additionalNodeNotCoveredByShapePropoertyPath() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset("/won/utils/goals/extraction/additional-node-not-covered-by-shape-property-path.trig");
        Model result = ex.extract(ds);

        RDFDataMgr.write(System.out, result, Lang.TRIG);
    }

    @Test
    public void additionalNodeNotCoveredByShapePropoertyPathWithError() throws IOException {
        DataExtraction ex = new DataExtraction();
        Dataset ds = loadDataset("/won/utils/goals/extraction/additional-node-not-covered-by-shape-property-path-with-error.trig");
        Model result = ex.extract(ds);

        RDFDataMgr.write(System.out, result, Lang.TRIG);
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
