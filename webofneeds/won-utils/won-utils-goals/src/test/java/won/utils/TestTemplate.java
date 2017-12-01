package won.utils;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public abstract class TestTemplate {

    static Map<String, String> prefixes;
    static {
        prefixes = new HashMap<>();
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("s", "http://schema.org/");
        prefixes.put("ex1", "http://example.org/1/");
        prefixes.put("ex2", "http://example.org/2/");
        prefixes.put("voc", "http://example.org/myvocabulary/");
        prefixes.put("taxi", "http://example.org/taxi/");
        prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
    }

    protected Dataset p1Ds;
    protected Model p1NeedModel;
    protected Model p1DataModel;
    protected Model p1ShapeModel;

    protected Dataset p2Ds;
    protected Model p2NeedModel;
    protected Model p2DataModel;
    protected Model p2ShapeModel;

    @Before
    public void init() throws IOException {

        p1Ds = loadDataset("/p1.trig");
        p1NeedModel = p1Ds.getNamedModel("http://example.org/1/p1-data");
        p1NeedModel.setNsPrefixes(prefixes);
        p1DataModel = p1Ds.getNamedModel("http://example.org/1/p1g-data");
        p1DataModel.setNsPrefixes(prefixes);
        p1ShapeModel = p1Ds.getNamedModel("http://example.org/1/p1g-shapes");
        p1ShapeModel.setNsPrefixes(prefixes);

        p2Ds = loadDataset("/p2.trig");
        p2NeedModel = p2Ds.getNamedModel("http://example.org/2/p2-data");
        p2NeedModel.setNsPrefixes(prefixes);
        p2DataModel = p2Ds.getNamedModel("http://example.org/2/p2g-data");
        p2DataModel.setNsPrefixes(prefixes);
        p2ShapeModel = p2Ds.getNamedModel("http://example.org/2/p2g-shapes");
        p2ShapeModel.setNsPrefixes(prefixes);
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
