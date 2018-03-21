package won.utils.proposescancelledagreement;


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
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.agreement.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;

public class ProposesCancelledAgreementTest {
	  
    private static final String inputFolder = "/won/utils/proposescancelledagreement/input/";
    private static final String expectedOutputFolder = "/won/utils/proposescancelledagreement/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

	@Test @Ignore
	public void oneValidProposalToCancel() throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test @Ignore
	public void twoProposalOneAgreementOneCancellationError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-errormsg-one-cancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-one-agreement-errormsg-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test @Ignore
	public void twoProposalOneAgreementOneCancellationmsgError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-one-cancellation-msgerror.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-one-agreement-one-cancellation-msgerror.ttl");
        test(input,expected);		
	}
	
	@Test @Ignore
	public void twoProposalOneAgreementOneCancellation () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-one-cancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-one-agreement-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test @Ignore
	public void twoProposaltwoAgreementstwoCancellationProposalClausesOneAccepted () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.ttl");
        test(input,expected);		
	}
	
	// cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes
	@Test @Ignore
	public void cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes () throws IOException {
	    Dataset input = loadDataset( inputFolder + "cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.ttl");
        test(input,expected);		
	}	
	
	@Test @Ignore
	public void oneAgreementOneCancellationTestProposalError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation-proposal-error.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-one-cancellation-proposal-error.ttl");
        test(input,expected);		
	}	
	
	
	@Test @Ignore
	public void oneAgreementTwoCancellationSameAgreement () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-two-cancellation-same-agreement.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-two-cancellation-same-agreement.ttl");
        test(input,expected);		
	}	
	
	// I am not sure why this fails...it should not...it worked fine in BlazeGraph
	@Test @Ignore
	public void twoProposaltwoAgreementstwoCancellationProposalClausesTwoAccepted () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-2cancellationproposal-1clauses-twoaccepted.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-2agreements-2cancellationproposal-1clauses-twoaccepted.ttl");
        test(input,expected);		
	}
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
		  Model actual = HighlevelFunctionFactory.getProposesInCancelledAgreementFunction().apply(input);
		  		  
	      RdfUtils.Pair<Model> diff = RdfUtils.diff(expectedOutput, actual); 

	       if (!(diff.getFirst().isEmpty() && diff.getSecond().isEmpty())) {
				 System.out.println("diff - only in expected:");
				 RDFDataMgr.write(System.out, diff.getFirst(), Lang.TRIG);
				 System.out.println("diff - only in actual:");
				 RDFDataMgr.write(System.out, diff.getSecond(), Lang.TRIG);

	       }

	       Assert.assertTrue(RdfUtils.areModelsIsomorphic(expectedOutput, actual)); 

}


    private Model customLoadModel(String path) throws IOException {
        InputStream is = null;
        Model model = null;
        try {
            is = getClass().getResourceAsStream(path);
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, is, RDFFormat.TTL.getLang());
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
