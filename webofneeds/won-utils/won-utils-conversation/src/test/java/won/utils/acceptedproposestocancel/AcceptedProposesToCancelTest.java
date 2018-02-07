package won.utils.acceptedproposestocancel;


import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.main.Main;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import junit.framework.TestCase;
import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;
import won.utils.proposaltocancel.ProposalToCancelTest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public class AcceptedProposesToCancelTest {
	
    private static final String inputFolder = "/won/utils/acceptedproposestocancel/input/";
    private static final String expectedOutputFolder = "/won/utils/acceptedproposestocancel/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

	@Test
	public void oneValidProposalToCancelOneAgreement () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test
	public void twoProposalOneAgreementOneCancellation () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-one-cancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-one-agreement-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test
	public void oneAgreementTwoCancellationSameAgreement () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-two-cancellation-same-agreement.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-two-cancellation-same-agreement.ttl");
        test(input,expected);		
	}
	
	// This should produce no result since it is an invalid proposaltocancel
	@Test
	public void oneAgreementOneCancellationTestProposalError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation-proposal-error.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "one-agreement-one-cancellation-proposal-error.ttl");
        test(input,expected);		
	}
	
	@Test
	public void cancelledTwoAgreementsSharingEnvelopesforAcceptsPurposes () throws IOException {
	    Dataset input = loadDataset( inputFolder + "cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "cancelled-Two-Agreements-Sharing-Envelopes-for-Accepts-Purposes.ttl");
        test(input,expected);		
	}
	
	// This agreement accepts a message instead of a proposal as well as a valid proposal...is the whole agreement invalid..or 
	// just part of it??
	// well the agreement function did not register it as a valid agreement..so I guess that it cannot be cancelled...
	@Test
	public void twoProposalOneAgreementOneCancellationError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-errormsg-one-cancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-one-agreement-errormsg-one-cancellation.ttl");
        test(input,expected);		
	}
	
	@Test
	public void twoProposalOneAgreementOneCancellationmsgError () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-agreement-one-cancellation-msgerror.trig");
	    Model expected = customLoadModel( expectedOutputFolder + "2proposal-one-agreement-one-cancellation-msgerror.ttl");
        test(input,expected);		
	}
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
		  Model actual = HighlevelFunctionFactory.getAcceptedProposesToCancelFunction().apply(input);
		  		  
	      RdfUtils.Pair<Model> diff = RdfUtils.diff(expectedOutput, actual); 

	       if (!(diff.getFirst().isEmpty() && diff.getSecond().isEmpty())) {
				 System.out.println("diff - only in expected:");
				 RDFDataMgr.write(System.out, diff.getFirst(), Lang.TRIG);
				 System.out.println("diff - only in actual:");
				 RDFDataMgr.write(System.out, diff.getSecond(), Lang.TRIG);

	       }

	       Assert.assertTrue(RdfUtils.areModelsIsomorphic(expectedOutput, actual)); 

}
	
	private static Model customLoadModel(String path) throws IOException {

		String prefix = "file:///C:/DATA/DEV/workspace/webofneeds/webofneeds/won-utils/won-utils-conversation/src/test/resources";
        FileManager.get().addLocatorClassLoader(AcceptedProposesToCancelTest.class.getClassLoader());
        Model model = FileManager.get().loadModel(prefix + path);
          
       return model;
   }
    
	
    private static Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = AcceptedProposesToCancelTest.class.getResourceAsStream(path);
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
