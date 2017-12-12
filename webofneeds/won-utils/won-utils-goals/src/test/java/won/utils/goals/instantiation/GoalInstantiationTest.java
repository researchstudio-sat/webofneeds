package won.utils.goals.instantiation;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Test;
import won.utils.goals.GoalInstantiation;
import won.utils.goals.GoalInstantiationResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class GoalInstantiationTest {

    private static final String baseFolder = "/won/utils/goals/instantiation/";

    @Test
    public void test() throws IOException {

        Dataset need1 = loadDataset(baseFolder + "need_seeks_tennis_lessons.trig");
        Dataset need2 = loadDataset(baseFolder + "need_debug_tennis_lessons.trig");
        Dataset conversation = loadDataset(baseFolder + "conversation.trig");

        GoalInstantiation goalInstantiation = new GoalInstantiation(need1, need2, conversation, "http://example.org/blended/");

        Collection<GoalInstantiationResult> results = goalInstantiation.createAllGoalInstantiationResults();

        for (GoalInstantiationResult result : results) {
            System.out.println(result.toString());
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
