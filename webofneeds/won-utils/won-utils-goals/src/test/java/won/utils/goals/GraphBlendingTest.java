package won.utils.goals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Test;
import won.utils.TestTemplate;

public class GraphBlendingTest extends TestTemplate {

    @Test
    public void blendSameTriples() {

        Model m1 = ModelFactory.createDefaultModel();
        Model m2 = ModelFactory.createDefaultModel();

        m1.createResource("testResource").addProperty(RDF.type, "testType");
        m2.createResource("testResource").addProperty(RDF.type, "testType");

        Model m3 = GraphBlending.blendSimple(m1, m2, "http://example.org/blended#");
        Assert.assertEquals(1, m3.listStatements().toList().size());
    }

    @Test
    public void blendDataGraphs() {

        Model blended = GraphBlending.blendSimple(p1DataModel, p2DataModel, "http://example.org/blended#");
        blended.write(System.out, "TRIG");
    }
}
