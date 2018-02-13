package won.utils.modification;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;

import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.io.InputStream;

public class ModificationProtocolTest {

    private static final String inputFolder = "/won/utils/modification/input/";
    private static final String expectedOutputFolder = "/won/utils/modification/expected/";



    // Add new tests for modification
    // file name key for the tests below ...
    // {d13-base-conversation} : BC , {correct} : c , {wrong} : w , {local} : l , {remote} : r , {retract} : R ,
    // {copvOfRemote} : CR ,  {selfretract} : R , {subsequent}

     // BC.trig
     // No Mod:retracts triples
    @Test
    public void d13NoModificationTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-no-retraction.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "correct-no-retraction.trig");
        test(input,expectedOutput);
    }

    // correct-remote-retract.trig
    // One mod:retracts triple
    @Test
    public void d13CorrectOneRemoteRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-remote-retract.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "correct-remote-retract.trig");
        test(input,expectedOutput);
    }

    // correct-local-retract-directly-previous.trig
    @Test
    public void d13CorrectOneLocalRetractionOfDirectlyPreviousMessageTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-local-retract-directly-previous.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "correct-local-retract-directly-previous.trig");
        test(input,expectedOutput);
    }

    // correct-local-retract-two-previous.trig
    @Test
    public void d13CorrectOneLocalRetractionOfLastButOneMessageTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-local-retract-two-previous.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "correct-local-retract-two-previous.trig");
        test(input,expectedOutput);
    }

    // correct-local-retract-two-previous.trig
    @Test
    public void d13CorrectRetractRetractOfLastButOneMessageTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-retractRetract-two-previous.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "correct-retractRetract-two-previous.trig");
        test(input,expectedOutput);
    }

    // all of the wrong test cases...
    // d13-base-conversation-wrong-local-copyOfRemote-retract-local
    @Test
    public void d13WrongLocalCopyRemoteRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-copyOfRemote-retract-local.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-local-copyOfRemote-retract-local.trig");
        test(input,expectedOutput);
    }

    // all of the wrong test cases...
    // wrong-local-retract-remote.trig
    @Test
    public void d13WrongLocalRetractRemoteRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-retract-remote.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-local-retract-remote.trig");
        test(input,expectedOutput);
    }

    // d13-base-conversation-wrong-local-retract-subsequent
    @Test
    public void d13WrongLocalRetractSubsequentRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-retract-subsequent.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-local-retract-subsequent.trig");
        test(input,expectedOutput);
    }

    // wrong-local-selfretract.trig
    @Test
    public void d13WrongLocalSelfRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-selfretract.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-local-selfretract.trig");
        test(input,expectedOutput);
    }

    // wrong-remote-retract-local.trig
    @Test
    public void d13WrongRemoteRetractLocalRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-remote-retract-local.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-remote-retract-local.trig");
        test(input,expectedOutput);
    }

    // wrong-remote-retract-subsequent.trig
    @Test
    public void d13WrongRemoteRetractSubsequentRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-remote-retract-subsequent.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-remote-retract-subsequent.trig");
        test(input,expectedOutput);
    }

    // wrong-remote-selfretract.trig
    @Test
    public void d13WrongRemoteSelfRetractRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-remote-selfretract.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "wrong-remote-selfretract.trig");
        test(input,expectedOutput);
    }

    public void test(Dataset input, Dataset expectedOutput) {

        // check that the computed dataset is the expected one
   //     ModifiedSelection mod = new ModifiedSelection();
  //      Dataset actual = mod.applyModificationSelection(input);
        Dataset actual = HighlevelFunctionFactory.getModifiedSelection().apply(input);
        //TODO: remove before checking in
        RdfUtils.Pair<Dataset> diff = RdfUtils.diff(expectedOutput, actual);
        if (diff.getFirst().isEmpty() && diff.getSecond().isEmpty()) {
        } else {
            System.out.println("diff - only in expected:");
            RDFDataMgr.write(System.out, diff.getFirst(), Lang.TRIG);
            System.out.println("diff - only in actual:");
            RDFDataMgr.write(System.out, diff.getSecond(), Lang.TRIG);
        }
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
