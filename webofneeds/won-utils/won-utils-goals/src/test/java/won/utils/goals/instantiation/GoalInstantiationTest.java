package won.utils.goals.instantiation;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.shared.NotFoundException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.model.Coordinate;
import won.protocol.util.NeedModelWrapper;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

public class GoalInstantiationTest {

  private static final String baseFolder = "/won/utils/goals/instantiation/";

  @BeforeClass
  public static void setLogLevel() {
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.INFO);
  }

  @Test
  public void example1_allInfoInTwoGoals() throws IOException {

    Dataset need1 = loadDataset(baseFolder + "ex1_need.trig");
    Dataset need2 = loadDataset(baseFolder + "ex1_need_debug.trig");
    Dataset conversation = loadDataset(baseFolder + "ex1_conversation.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalCombinationInstantiationResults();

    // instantiation of combined goals must be conform
    Assert.assertEquals(1, results.size());
    System.out.println(results.iterator().next().toString());
    Assert.assertTrue(results.iterator().next().isConform());

    // instantiation of goal of need1 fails cause driver is missing
    NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
    Resource goal = needWrapper1.getGoals().iterator().next();
    GoalInstantiationResult result = goalInstantiation.findInstantiationForGoal(goal);
    System.out.println(result.toString());
    Assert.assertFalse(result.isConform());
    Assert.assertEquals("hasDriver",
        result.getShaclReportWrapper().getValidationResults().iterator().next().getResultPath().getLocalName());

    // instantiation of goal of need2 fails cause 3 attributes are missing:
    // location, time, client
    NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
    goal = needWrapper2.getGoals().iterator().next();
    result = goalInstantiation.findInstantiationForGoal(goal);
    System.out.println(result.toString());
    Assert.assertFalse(result.isConform());
    Assert.assertEquals(3, result.getShaclReportWrapper().getValidationResults().size());
  }

  @Test
  public void example2_allInfoInTwoGoalsAndMessage() throws IOException {

    Dataset need1 = loadDataset(baseFolder + "ex2_need.trig");
    Dataset need2 = loadDataset(baseFolder + "ex2_need_debug.trig");
    Dataset conversationWithoutPickupTime = loadDataset(baseFolder + "ex1_conversation.trig");
    Dataset conversationWithPickupTime = loadDataset(baseFolder + "ex2_conversation.trig");

    // this conversation doas not contain the missing pickup time info so the goals
    // cannot be fulfilled
    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2,
        conversationWithoutPickupTime, "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalCombinationInstantiationResults();
    Assert.assertEquals(1, results.size());
    System.out.println(results.iterator().next().toString());
    Assert.assertFalse(results.iterator().next().isConform());

    // this conversation contains the missing pickup info so the goals can be
    // fulfilled
    goalInstantiation = new GoalInstantiationProducer(need1, need2, conversationWithPickupTime, "http://example.org/",
        "http://example.org/blended/");
    results = goalInstantiation.createAllGoalCombinationInstantiationResults();
    Assert.assertEquals(1, results.size());
    System.out.println(results.iterator().next().toString());
    Assert.assertTrue(results.iterator().next().isConform());

    // instantiation of goal of need1 fails cause driver is missing
    NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
    Resource goal = needWrapper1.getGoals().iterator().next();
    GoalInstantiationResult result = goalInstantiation.findInstantiationForGoal(goal);
    System.out.println(result.toString());
    Assert.assertFalse(result.isConform());
    Assert.assertEquals("hasDriver",
        result.getShaclReportWrapper().getValidationResults().iterator().next().getResultPath().getLocalName());

    // instantiation of goal of need2 fails cause 3 attributes are missing:
    // location, time, client
    NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
    goal = needWrapper2.getGoals().iterator().next();
    result = goalInstantiation.findInstantiationForGoal(goal);
    System.out.println(result.toString());
    Assert.assertFalse(result.isConform());
    Assert.assertEquals(3, result.getShaclReportWrapper().getValidationResults().size());
  }

  @Test
  public void example3_multipleGoalsFulfilled() throws IOException {

    Dataset need1 = loadDataset(baseFolder + "ex3_need.trig");
    Dataset need2 = loadDataset(baseFolder + "ex3_need_debug.trig");
    Dataset conversation = loadDataset(baseFolder + "ex3_conversation.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalCombinationInstantiationResults();

    // We have 4 and 2 goals so we expected 8 results
    Assert.assertEquals(8, results.size());

    // We expected three valid results
    Collection<Model> validResults = new LinkedList<>();
    for (GoalInstantiationResult result : results) {
      if (result.isConform()) {
        validResults.add(result.getInstanceModel());
      }
    }

    for (Model valid : validResults) {
      valid.write(System.out, "TRIG");
    }
    Assert.assertEquals(3, validResults.size());
  }

  @Test
  public void example4_geoCoordinatesFulfilled() throws IOException {

    Dataset need1 = loadDataset(baseFolder + "ex4_need.trig");
    Dataset need2 = loadDataset(baseFolder + "ex4_need_debug.trig");
    Dataset conversation = loadDataset(baseFolder + "ex4_conversation.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalCombinationInstantiationResults();

    // We have only one goal on each side so we expect only one result
    Assert.assertEquals(1, results.size());
    // We expect also one valid result
    Collection<Model> validResults = new LinkedList<>();
    for (GoalInstantiationResult result : results) {
      if (result.isConform()) {
        validResults.add(result.getInstanceModel());
      }
    }

    Assert.assertEquals(1, validResults.size());
    for (Model valid : validResults) {
      valid.write(System.out, "TRIG");
    }
  }

  @Test
  public void example5_singleGoalsValidity() throws IOException {

    // check that the goals from each need can be validated successfully without
    // each other
    Dataset need1 = loadDataset(baseFolder + "ex5_need.trig");
    Dataset need2 = loadDataset(baseFolder + "ex5_need_debug.trig");
    Dataset conversation = loadDataset(baseFolder + "ex5_conversation.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalCombinationInstantiationResults();

    NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
    Resource goal = needWrapper1.getGoals().iterator().next();
    GoalInstantiationResult result = goalInstantiation.findInstantiationForGoal(goal);
    Assert.assertTrue(result.isConform());

    NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
    goal = needWrapper1.getGoals().iterator().next();
    result = goalInstantiation.findInstantiationForGoal(goal);
    Assert.assertTrue(result.isConform());
  }

  private static QuerySolution executeQuery(String queryString, Model payload) {
    Query query = QueryFactory.create(queryString);
    try (QueryExecution qexec = QueryExecutionFactory.create(query, payload)) {
      ResultSet resultSet = qexec.execSelect();
      if (resultSet.hasNext()) {
        QuerySolution solution = resultSet.nextSolution();
        return solution;
      }
    }
    return null;
  }

  @Test
  public void exampleCorrectTaxi_validity() throws IOException {
    Dataset taxiOffer = loadDataset(baseFolder + "exCorrect_taxioffer.trig");
    Dataset taxiDemand = loadDataset(baseFolder + "exCorrect_taxi.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(taxiOffer, taxiDemand, null,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createGoalInstantiationResultsForNeed1();

    for (GoalInstantiationResult res : results) {
      System.out.println("Result::::::::::::::::::::::::::::::" + res.isConform());
      System.out.println(res.toString());
      if (res.isConform()) {
        Coordinate departureAddress = getAddress(
            loadSparqlQuery("/won/utils/goals/extraction/address/fromLocationQuery.rq"), res.getInstanceModel());
        String departureName = getName(loadSparqlQuery("/won/utils/goals/extraction/address/fromLocationQuery.rq"),
            res.getInstanceModel());
        Coordinate destinationAddress = getAddress(
            loadSparqlQuery("/won/utils/goals/extraction/address/toLocationQuery.rq"), res.getInstanceModel());
        String destinationName = getName(loadSparqlQuery("/won/utils/goals/extraction/address/toLocationQuery.rq"),
            res.getInstanceModel());

        // Assert.assertEquals(departureAddress, new Coordinate(10.0f, 11.0f));
        // Assert.assertEquals(destinationAddress, new Coordinate(12.0f, 13.0f));
      }
    }

    NeedModelWrapper needWrapper1 = new NeedModelWrapper(taxiOffer);
    Resource goal = needWrapper1.getGoals().iterator().next();
    GoalInstantiationResult result = goalInstantiation.findInstantiationForGoal(goal);
    Assert.assertTrue(result.isConform());

    GoalInstantiationResult recheckResultModel = GoalInstantiationProducer.findInstantiationForGoalInDataset(taxiOffer,
        goal, result.getInstanceModel());
    Assert.assertTrue(recheckResultModel.isConform());
  }

  @Test
  public void exampleTaxi_validity() throws IOException {
    Dataset taxiOffer = loadDataset(baseFolder + "ex6_taxioffer.trig");
    Dataset taxiDemand = loadDataset(baseFolder + "ex6_taxi.trig");
    Dataset taxiDemandNoLoc = loadDataset(baseFolder + "ex6_taxi_noloc.trig");
    Dataset taxiDemandTwoLoc = loadDataset(baseFolder + "ex6_taxi_twoloc.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(taxiOffer, taxiDemand, null,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createGoalInstantiationResultsForNeed1();

    for (GoalInstantiationResult res : results) {
      System.out.println("Result::::::::::::::::::::::::::::::" + res.isConform());
      System.out.println(res.toString());
      if (res.isConform()) {
        Coordinate departureAddress = getAddress(
            loadSparqlQuery("/won/utils/goals/extraction/address/fromLocationQuery.rq"), res.getInstanceModel());
        Coordinate destinationAddress = getAddress(
            loadSparqlQuery("/won/utils/goals/extraction/address/toLocationQuery.rq"), res.getInstanceModel());

        Assert.assertEquals(new Coordinate(10.0f, 11.0f), departureAddress);
        Assert.assertEquals(new Coordinate(12.0f, 13.0f), destinationAddress);
      }
    }

    NeedModelWrapper needWrapper1 = new NeedModelWrapper(taxiOffer);
    Resource goal = needWrapper1.getGoals().iterator().next();
    GoalInstantiationResult result = goalInstantiation.findInstantiationForGoal(goal);
    Assert.assertTrue(result.isConform());

    GoalInstantiationResult recheckResultModel = GoalInstantiationProducer.findInstantiationForGoalInDataset(taxiOffer,
        goal, result.getInstanceModel());
    Assert.assertTrue(recheckResultModel.isConform());

    goalInstantiation = new GoalInstantiationProducer(taxiOffer, taxiDemandNoLoc, null, "http://example.org/",
        "http://example.org/blended/");
    results = goalInstantiation.createGoalInstantiationResultsForNeed1();

    for (GoalInstantiationResult res : results) {
      Assert.assertFalse(res.isConform());
    }

    goalInstantiation = new GoalInstantiationProducer(taxiOffer, taxiDemandTwoLoc, null, "http://example.org/",
        "http://example.org/blended/");
    results = goalInstantiation.createGoalInstantiationResultsForNeed1();

    for (GoalInstantiationResult res : results) {
      Assert.assertFalse(res.isConform());
    }
  }

  @Test
  public void exampleTaxiFakeLocation_validity() throws IOException {
    Dataset taxiOffer = loadDataset(baseFolder + "ex7_taxioffer.trig");
    Dataset taxiDemand = loadDataset(baseFolder + "ex7_taxi.trig");

    GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(taxiOffer, taxiDemand, null,
        "http://example.org/", "http://example.org/blended/");
    Collection<GoalInstantiationResult> results = goalInstantiation.createGoalInstantiationResultsForNeed1();

    for (GoalInstantiationResult res : results) {
      res.getInstanceModel().write(System.out, "TRIG");
      Assert.assertTrue(res.isConform());

      Coordinate departureAddress = getAddress(
          loadSparqlQuery("/won/utils/goals/extraction/address/northWestCornerQuery.rq"), res.getInstanceModel());
      Coordinate destinationAddress = getAddress(
          loadSparqlQuery("/won/utils/goals/extraction/address/southEastCornerQuery.rq"), res.getInstanceModel());

      Assert.assertEquals(departureAddress, new Coordinate(48.218727f, 16.360141f));
      Assert.assertEquals(destinationAddress, new Coordinate(48.218828f, 16.360241f));
    }

    NeedModelWrapper needWrapper1 = new NeedModelWrapper(taxiOffer);
    Resource goal = needWrapper1.getGoals().iterator().next();
    GoalInstantiationResult result = goalInstantiation.findInstantiationForGoal(goal);
    Assert.assertTrue(result.isConform());

    GoalInstantiationResult recheckResultModel = GoalInstantiationProducer.findInstantiationForGoalInDataset(taxiOffer,
        goal, result.getInstanceModel());
    Assert.assertTrue(recheckResultModel.isConform());
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

  private static String loadSparqlQuery(String filePath) {
    InputStream is = GoalInstantiationTest.class.getResourceAsStream(filePath);
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(is, writer, Charsets.UTF_8);
    } catch (IOException e) {
      throw new NotFoundException("failed to load resource: " + filePath);
    } finally {
      try {
        is.close();
      } catch (Exception e) {
      }
    }
    return writer.toString();
  }

  private static Coordinate getAddress(String query, Model payload) {
    QuerySolution solution = executeQuery(query, payload);

    if (solution != null) {
      float lat = solution.getLiteral("lat").getFloat();
      float lon = solution.getLiteral("lon").getFloat();
      return new Coordinate(lat, lon);
    } else {
      return null;
    }
  }

  private static String getName(String query, Model payload) {
    QuerySolution solution = executeQuery(query, payload);

    if (solution != null) {
      return solution.getLiteral("name").getString();
    } else {
      return null;
    }
  }
}
