package won.utils.agreement;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.junit.Assert;
import org.junit.Test;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.io.InputStream;

public class AgreementProtocolTest {

    private static final String inputFolder = "/won/utils/agreement/input/";
    private static final String expectedOutputFolder = "/won/utils/agreement/expected/";


    @Test
    public void noAgreementsTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "no-agreements.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "no-agreements.trig");
        test(input,expectedOutput);
    }
    
    @Test
    public void oneAgreementTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement.trig");
        test(input,expectedOutput);
    }

    @Test
    public void oneAgreementOneCancellationTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement-one-cancellation.trig");
        test(input,expectedOutput);
    }

    @Test
    public void oneAgreementOneCancellationTestProposalError () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation-proposal-error.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement-one-cancellation-proposal-error.trig");
        test(input,expectedOutput);
    } 
    
    
    @Test
    public void oneAgreementTwoProposalClauses () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement-two-proposal-clauses.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement-two-proposal-clauses.trig");
        test(input,expectedOutput);
    } 
    
    @Test
    public void oneAgreementMissingProposal () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement-missing-proposal.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement-missing-proposal.trig");
        test(input,expectedOutput);
    }
    
    @Test
    public void oneAgreementMissingClause () throws IOException {
        Dataset input = loadDataset( inputFolder + "one-agreement-missing-clause.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "one-agreement-missing-clause.trig");
        test(input,expectedOutput);
    }
    
    @Test
    public void noAgreementOneCancellationError () throws IOException {
        Dataset input = loadDataset( inputFolder + "no-agreement-one-cancellation-error.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "no-agreement-one-cancellation-error.trig");
        test(input,expectedOutput);
    }
    
    @Test
    public void twoProposalOneAgreementOneCancellation () throws IOException {
        Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-one-cancellation.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "2proposal-one-agreement-one-cancellation.trig");
        test(input,expectedOutput);
    }
    
    @Test
    public void twoProposalOneAgreementOneCancellationError () throws IOException {
        Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-errormsg-one-cancellation.trig");
        Dataset expectedOutput = loadDataset( expectedOutputFolder + "2proposal-one-agreement-errormsg-one-cancellation.trig");
        test(input,expectedOutput);
    }
    
    public void test(Dataset input, Dataset expectedOutput) {

        // check that the computed dataset is the expected one
        AgreementFunction agreementFunction = new AgreementFunction();
        Dataset actual = agreementFunction.applyAgreementFunction(input);
        //TODO: remove before checking in
        RdfUtils.Pair<Dataset> diff = RdfUtils.diff(expectedOutput, actual);
        if (diff.getFirst().isEmpty() && diff.getSecond().isEmpty()) {
            System.out.println("expected equals actual");
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
