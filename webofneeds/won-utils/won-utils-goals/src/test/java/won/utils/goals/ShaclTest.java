package won.utils.goals;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.topbraid.shacl.util.ModelPrinter;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ShaclTest {

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

    private Dataset p1Ds;
    private Model p1NeedModel;
    private Model p1DataModel;
    private Model p1ShapeModel;

    private Dataset p2Ds;
    private Model p2NeedModel;
    private Model p2DataModel;
    private Model p2ShapeModel;

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

    @Test
    public void validateP1DataWithP1Shape() throws IOException {

        Resource report = ValidationUtil.validateModel(p1DataModel, p1ShapeModel, false);
        ShaclReportWrapper reportWrapper = new ShaclReportWrapper(report);
        Assert.assertFalse(reportWrapper.isConform());

        System.out.println(ModelPrinter.get().print(report.getModel()));
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
