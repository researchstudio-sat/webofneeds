package won.cryptography.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

/**
 * User: ypanchenko Date: 15.07.2014
 */
public class JenaBnodeInDatasetTest {
    private static final String RESOURCE_FILE = "/test_2graphs.trig";

    @Test
    public void testReadWriteDatasetWithTwoGraphs() throws Exception {
        InputStream is = this.getClass().getResourceAsStream(RESOURCE_FILE);
        Dataset dataset = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
        is.close();
        assertSameSubjBnode(dataset, "http://www.example.com#", "G1", "G2", "pred1", "pred2");
        File outFile = File.createTempFile("won", ".trig");
        System.out.println(outFile);
        OutputStream os = new FileOutputStream(outFile);
        // Writing in TRIG results in loosing info about shared between graphs blank
        // node.
        // I wrote to Jena mailing list and they opened
        // https://issues.apache.org/jira/browse/JENA-745
        // Solution for the moment is to use TRIG_BLOCKS when writing
        // RDFDataMgr.write(os, dataset, RDFFormat.TRIG);
        RDFDataMgr.write(os, dataset, RDFFormat.TRIG_BLOCKS);
        os.close();
        InputStream is2 = new FileInputStream(outFile);
        ;
        Dataset dataset2 = DatasetFactory.createGeneral();
        RDFDataMgr.read(dataset2, is2, RDFFormat.TRIG.getLang());
        is2.close();
        assertSameSubjBnode(dataset2, "http://www.example.com#", "G1", "G2", "pred1", "pred2");
    }

    private void assertSameSubjBnode(Dataset dataset, final String ns, final String g1, final String g2,
                    final String pred1, final String pred2) {
        Model model1 = dataset.getNamedModel(ns + g1);
        Model model2 = dataset.getNamedModel(ns + g2);
        Property prop1 = model1.createProperty(ns, pred1);
        ResIterator iter1 = model1.listResourcesWithProperty(prop1, null);
        Property prop2 = model2.createProperty(ns, pred2);
        ResIterator iter2 = model2.listResourcesWithProperty(prop2, null);
        Resource r1 = iter1.next();
        Resource r2 = iter2.next();
        Assert.assertTrue(r1 != null && r1.equals(r2));
    }
}
