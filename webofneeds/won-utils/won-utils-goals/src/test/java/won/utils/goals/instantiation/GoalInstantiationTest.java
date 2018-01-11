package won.utils.goals.instantiation;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class GoalInstantiationTest {

    private static final String baseFolder = "/won/utils/goals/instantiation/";

    @Test
    public void example1_allInfoInTwoGoals() throws IOException {

        Dataset need1 = loadDataset(baseFolder + "ex1_need.trig");
        Dataset need2 = loadDataset(baseFolder + "ex1_need_debug.trig");
        Dataset conversation = loadDataset(baseFolder + "ex1_conversation.trig");

        GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation, "http://example.org/", "http://example.org/blended/");
        Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalInstantiationResults();

        Assert.assertEquals(1, results.size());
        System.out.println(results.iterator().next().toString());
        Assert.assertTrue(results.iterator().next().isConform());
    }

    @Test
    public void example2_allInfoInTwoGoalsAndMessage() throws IOException {

        Dataset need1 = loadDataset(baseFolder + "ex2_need.trig");
        Dataset need2 = loadDataset(baseFolder + "ex2_need_debug.trig");
        Dataset conversationWithoutPickupTime = loadDataset(baseFolder + "ex1_conversation.trig");
        Dataset conversationWithPickupTime = loadDataset(baseFolder + "ex2_conversation.trig");

        // this conversation doas not contain the missing pickup time info so the goals cannot be fulfilled
        GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversationWithoutPickupTime, "http://example.org/", "http://example.org/blended/");
        Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalInstantiationResults();
        Assert.assertEquals(1, results.size());
        System.out.println(results.iterator().next().toString());
        Assert.assertFalse(results.iterator().next().isConform());

        // this conversation contains the missing pickup info so the goals can be fulfilled
        goalInstantiation = new GoalInstantiationProducer(need1, need2, conversationWithPickupTime, "http://example.org/", "http://example.org/blended/");
        results = goalInstantiation.createAllGoalInstantiationResults();
        Assert.assertEquals(1, results.size());
        System.out.println(results.iterator().next().toString());
        Assert.assertTrue(results.iterator().next().isConform());
    }

    @Test
    public void example3_multipleGoalsFulfilled() throws IOException {

        Dataset need1 = loadDataset(baseFolder + "ex3_need.trig");
        Dataset need2 = loadDataset(baseFolder + "ex3_need_debug.trig");
        Dataset conversation = loadDataset(baseFolder + "ex3_conversation.trig");

        GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation, "http://example.org/", "http://example.org/blended/");
        Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalInstantiationResults();

        // We have 4 and 2 goals so we expected 8 results
        Assert.assertEquals(8, results.size());

        // We expected three valid results
        Collection<Model> validResults = goalInstantiation.createAllConformGoalInstantiationModels();
        Assert.assertEquals(3, validResults.size());
        for (Model valid : validResults) {
            valid.write(System.out, "TRIG");
        }
    }

    @Test
    public void example4_geoCoordinatesFulfilled() throws IOException {

        Dataset need1 = loadDataset(baseFolder + "ex4_need.trig");
        Dataset need2 = loadDataset(baseFolder + "ex4_need_debug.trig");
        Dataset conversation = loadDataset(baseFolder + "ex4_conversation.trig");

        GoalInstantiationProducer goalInstantiation = new GoalInstantiationProducer(need1, need2, conversation, "http://example.org/", "http://example.org/blended/");
        Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalInstantiationResults();

        // We have only one goal on each side so we expect only one result
        Assert.assertEquals(1, results.size());

        // We expect also one valid result
        Collection<Model> validResults = goalInstantiation.createAllConformGoalInstantiationModels();
        Assert.assertEquals(1, validResults.size());
        for (Model valid : validResults) {
            valid.write(System.out, "TRIG");
        }
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
