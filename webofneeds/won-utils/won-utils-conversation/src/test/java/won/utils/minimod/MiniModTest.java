package won.utils.minimod;

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

public class MiniModTest {

    private static final String inputFolder = "/won/utils/minimod/input/";
    private static final String expectedOutputFolder = "/won/utils/minimod/expected/";


    @Test
    public void d13NoModificationTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-no-retraction.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "correct-no-retraction.trig");
        test(input,expectedOutput);
    }

    
    @Test
    public void multipleGraphs () throws IOException {
        Dataset input = loadDataset( inputFolder + "multiple-graphs.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "multiple-graphs.trig");
        test(input,expectedOutput);
    }
    
    @Test
    public void proposesRetracted () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement-proposes-retracted.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement-proposes-retracted.trig");
        test(input,expectedOutput);
    }
   
    public void test(Dataset input, Dataset expectedOutput) {

        // check that the computed dataset is the expected one
        Dataset actual = HighlevelFunctionFactory.getMiniMod().apply(input);
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
