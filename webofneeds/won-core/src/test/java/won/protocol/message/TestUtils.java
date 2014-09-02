package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * User: ypanchenko
 * Date: 05.08.2014
 */
public class TestUtils
{

  public static Dataset createTestDataset(String resourceName) throws IOException {

    InputStream is = TestUtils.class.getResourceAsStream(resourceName);
    Dataset dataset = DatasetFactory.createMem();
    RDFDataMgr.read(dataset, is, RDFFormat.TRIG.getLang());
    is.close();
    return dataset;

  }

  public static Model createTestModel(String resourceName) throws IOException {

    InputStream is = TestUtils.class.getResourceAsStream(resourceName);
    Model model = ModelFactory.createDefaultModel();
    RDFDataMgr.read(model, is, RDFFormat.TURTLE.getLang());
    //model1.read(new InputStreamReader(is1), RESOURCE_URI, FileUtils.langTurtle);
    is.close();
    return model;

  }

  public static void print(Model model) {
    System.out.println(RdfUtils.writeModelToString(model, Lang.TURTLE));
  }

  public static void print(Dataset dataset) {
    System.out.println(RdfUtils.writeDatasetToString(dataset, Lang.TRIG));
  }
}
