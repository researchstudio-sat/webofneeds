package won.utils.shacl;

import java.io.IOException;
import java.io.InputStream;

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
import org.topbraid.shacl.vocabulary.SH;

public class ShaclTest {

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

        p1Ds = loadDataset("/won/utils/shacl/p1.trig");
        p1NeedModel = p1Ds.getNamedModel("http://example.org/1/p1-data");
        p1DataModel = p1Ds.getNamedModel("http://example.org/1/p1g-data");
        p1ShapeModel = p1Ds.getNamedModel("http://example.org/1/p1g-shapes");

        p2Ds = loadDataset("/won/utils/shacl/p2.trig");
        p2NeedModel = p2Ds.getNamedModel("http://example.org/2/p2-data");
        p2DataModel = p2Ds.getNamedModel("http://example.org/2/p2g-data");
        p2ShapeModel = p2Ds.getNamedModel("http://example.org/2/p2g-shapes");
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

    @Test
    public void validateP1DataWithP1Shape() {

        Resource report = ValidationUtil.validateModel(p1DataModel, p1ShapeModel, false);
        System.out.println(ModelPrinter.get().print(report.getModel()));

        ShaclReportWrapper reportWrapper = new ShaclReportWrapper(report);
        Assert.assertFalse(reportWrapper.isConform());
        Assert.assertEquals(3, reportWrapper.getValidationResults().size());

        for (ValidationResultWrapper result : reportWrapper.getValidationResults()) {
            Assert.assertEquals(SH.Violation, result.getResultSeverity());
            Assert.assertEquals("ride1", result.getFocusNode().getLocalName());
        }
    }

    @Test
    public void validateP2DataWithP2Shape() {

        Resource report = ValidationUtil.validateModel(p2DataModel, p2ShapeModel, false);
        System.out.println(ModelPrinter.get().print(report.getModel()));

        ShaclReportWrapper reportWrapper = new ShaclReportWrapper(report);
        Assert.assertFalse(reportWrapper.isConform());
        Assert.assertEquals(1, reportWrapper.getValidationResults().size());

        for (ValidationResultWrapper result : reportWrapper.getValidationResults()) {
            Assert.assertEquals(SH.Violation, result.getResultSeverity());
            Assert.assertEquals("myRide", result.getFocusNode().getLocalName());
            Assert.assertEquals("Less than 1 values", result.getResultMessage());
            Assert.assertEquals(SH.MinCountConstraintComponent, result.getSourceConstraintComponent());
        }
    }
}
