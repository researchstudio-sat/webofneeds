package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

  public static List<String> getModelNames(Dataset dataset) {
    List<String> modelNames = new ArrayList<String>();
    Iterator<String> names = dataset.listNames();
    while (names.hasNext()) {
      modelNames.add(names.next());
    }
    return modelNames;
  }

  public static void print(Model model) {
    StringWriter sw = new StringWriter();
    model.write(sw, FileUtils.langTurtle);
    System.out.println(sw.toString());
  }

  public static void print(Dataset dataset) {
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, dataset, RDFFormat.TRIG.getLang());
    System.out.println(sw.toString());
  }
}
