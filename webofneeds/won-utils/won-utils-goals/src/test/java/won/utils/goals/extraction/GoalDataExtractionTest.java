package won.utils.goals.extraction;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.utils.goals.GoalUtils;

public class GoalDataExtractionTest {

  private static final String baseFolder = "/won/utils/goals/extraction/";

  @BeforeClass
  public static void setLogLevel() {
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.INFO);
  }

  @Test
  public void additionalNodeNotCoveredByShape() throws IOException {
    Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape.trig");
    Model dataModel = ds.getNamedModel("http://example.org/ns#data");
    Model shapesModel = ds.getNamedModel("http://example.org/ns#shapes");
    Model actual = GoalUtils.extractGoalData(dataModel, shapesModel);
    Model expected = loadModel(baseFolder + "additional-node-not-covered-by-shape-expected-result.trig");
    RDFDataMgr.write(System.out, actual, Lang.TRIG);
    Assert.assertTrue(actual.isIsomorphicWith(expected));
  }

  @Test
  public void additionalNodeNotCoveredByShapePersonClosed() throws IOException {
    Dataset ds = loadDataset(baseFolder + "additional-node-not-covered-by-shape-person-closed.trig");
    Model dataModel = ds.getNamedModel("http://example.org/ns#data");
    Model shapesModel = ds.getNamedModel("http://example.org/ns#shapes");
    Model actual = GoalUtils.extractGoalData(dataModel, shapesModel);
    Model expected = loadModel(baseFolder + "additional-node-not-covered-by-shape-person-closed-expected-result.trig");
    RDFDataMgr.write(System.out, actual, Lang.TRIG);
    Assert.assertTrue(actual.isIsomorphicWith(expected));
  }

  @Test
  public void sequencePath() throws IOException {
    Dataset ds = loadDataset(baseFolder + "SequencePath.trig");
    Model dataModel = ds.getNamedModel("http://example.org/ns#data");
    Model shapesModel = ds.getNamedModel("http://example.org/ns#shapes");
    Model actual = GoalUtils.extractGoalData(dataModel, shapesModel);
    Model expected = loadModel(baseFolder + "SequencePath-expected-result.trig");
    RDFDataMgr.write(System.out, actual, Lang.TRIG);

    RDFDataMgr.write(System.out, expected, Lang.TRIG);
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
