package won.utils.acceptedproposes;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
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
import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.io.InputStream;

public class AcceptedProposesTest {
	
    private static final String inputFolder = "/won/utils/acceptedproposes/input/";
    private static final String expectedOutputFolder = "/won/utils/acceptedproposes/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

	@Test
	public void oneValidProposal () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement.ttl");
        test(input,expected);		
	}
	
	// This considers a proposal from a valid agreement that has been validly cancelled as non-existent
	// Look at the functionality of closedacceptsproposaltocancel if you want agreements containing proposals
	// from cancelled agreements.
	@Test
	public void oneValidProposalwithOneAgreementOneCancellationTest () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test
	public void oneValidProposalWithOneAgreementOneCancellationTestProposalError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation-proposal-error.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-one-cancellation-proposal-error.ttl");
        test(input,expected);		
	}
	
	@Test
	public void twoValidProposalWithoneAgreementTwoProposalClauses () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-two-proposal-clauses.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-two-proposal-clauses.ttl");
        test(input,expected);		
	}
	
	@Test
	public void noValidProposaloneAgreementMissingProposal () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-missing-proposal.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-missing-proposal.ttl");
        test(input,expected);		
	}
	
	@Test
	public void noValidProposaloneAgreementMissingClause () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-missing-clause.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-missing-clause.ttl");
        test(input,expected);		
	}
	
	// a proposal that was unaccepted, so it is an open not closed proposal
	@Test
	public void validProposalNoAcceptanceNoAgreementOneCancellationError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "no-agreement-one-cancellation-error.trig");
   	    Model expected = customLoadModel( expectedOutputFolder + "no-agreement-one-cancellation-error.ttl");
        test(input,expected);		
	}
	
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
		  Model actual = HighlevelFunctionFactory.getAcceptedProposesFunction().apply(input);
		  		  
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
