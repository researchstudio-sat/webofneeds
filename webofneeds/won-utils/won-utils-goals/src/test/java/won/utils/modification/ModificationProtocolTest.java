package won.utils.modification;

import com.github.jsonldjava.core.RDFDatasetUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.util.RdfUtils;
import won.utils.goals.blending.GraphBlending;

import java.io.IOException;
import java.io.InputStream;

public class ModificationProtocolTest {

    private static final String inputFolder = "/won/utils/modification/input/";
    private static final String expectedOutputFolder = "/won/utils/modification/expected/";


    @Test
    public void noModificationTest() throws IOException {
        Dataset input = loadDataset(inputFolder + "test_no_modification.trig");
        Dataset expectedOutput = loadDataset(expectedOutputFolder + "test_no_modification.trig");
        test(input, expectedOutput);
    }


    @Test
    public void noBrentsModificationTest() throws IOException {
        Dataset input = loadDataset(inputFolder + "base-conversation.trig");
        Dataset expectedOutput = loadDataset(expectedOutputFolder + "base-conversation.trig");
        test(input, expectedOutput);
    }

    @Test
    public void oneRetractionTest() throws IOException {
        Dataset input = loadDataset(inputFolder + "test_conversation_one_retraction.trig");
        Dataset expectedOutput = loadDataset(expectedOutputFolder + "test_conversation_one_retraction.trig");
        test(input, expectedOutput);
    }

    public void test(Dataset input, Dataset expectedOutput) {

        // check that the computed dataset is the expected one
        ConversationModification mod = new ConversationModification();
        Dataset actual = mod.applyModificationSelection(input);
        //TODO: remove before checking in
        RdfUtils.Pair<Dataset> diff = RdfUtils.diff(expectedOutput, actual);
        System.out.println("diff - only in expected:");
        RDFDataMgr.write(System.out, diff.getFirst(), Lang.TRIG);
        System.out.println("diff - only in actual:");
        RDFDataMgr.write(System.out, diff.getSecond(), Lang.TRIG);
        Assert.assertTrue(RdfUtils.isIsomorphicWith(expectedOutput, actual));
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
