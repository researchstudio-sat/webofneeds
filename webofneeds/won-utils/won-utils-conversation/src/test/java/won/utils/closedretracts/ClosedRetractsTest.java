package won.utils.closedretracts;


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
import won.protocol.util.RdfUtils;
import won.utils.openproposestocancel.OpenProposesToCancelTest;
import won.utils.proposaltocancel.ProposalToCancelTest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public class ClosedRetractsTest {
	
    private static final String inputFolder = "/won/utils/closedretracts/input/";
    private static final String expectedOutputFolder = "/won/utils/closedretracts/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

    // One mod:retracts triple
	@Test
	public void correctOneRemoteRetractionTest () throws IOException {
	    Dataset input = loadDataset( inputFolder + "correct-remote-retract.trig"); 
	    Model expected = customLoadModel( expectedOutputFolder  + "correct-remote-retract.ttl");
        test(input,expected);		
	}
	
	   // No Mod:retracts triples
    @Test
    public void noModificationTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-no-retraction.trig");
        Model expected = customLoadModel( expectedOutputFolder + "correct-no-retraction.ttl");
        test(input,expected);
    }
    
    @Test
    public void correctOneLocalRetractionOfDirectlyPreviousMessageTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-local-retract-two-previous.trig");
        Model expected = customLoadModel( expectedOutputFolder + "correct-local-retract-two-previous.ttl");
        test(input,expected);
    } 
    
    @Test
    public void correctOneLocalRetractionOfLastButOneMessageTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-local-retract-directly-previous.trig");
        Model expected = customLoadModel( expectedOutputFolder + "correct-local-retract-directly-previous.ttl");
        test(input,expected);
    }    
   
    @Test
    public void correctRetractRetractOfLastButOneMessageTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "correct-retractRetract-two-previous.trig");
        Model expected = customLoadModel( expectedOutputFolder + "correct-retractRetract-two-previous.ttl");
        test(input,expected);
    }  
    
    @Test
    public void wrongLocalCopyRemoteRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-copyOfRemote-retract-local.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-local-copyOfRemote-retract-local.ttl");
        test(input,expected);
    }
    
    @Test
    public void wrongLocalRetractRemoteRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-retract-remote.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-local-retract-remote.ttl");
        test(input,expected);
    }
    
    @Test
    public void wrongLocalRetractSubsequentRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-retract-subsequent.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-local-retract-subsequent.ttl");
        test(input,expected);
    }
    
    @Test
    public void wrongLocalSelfRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-local-selfretract.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-local-selfretract.ttl");
        test(input,expected);
    }
    
    @Test
    public void wrongRemoteRetractLocalRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-remote-retract-local.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-remote-retract-local.ttl");
        test(input,expected);
    }
	
    @Test
    public void wrongRemoteRetractSubsequentRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-remote-retract-subsequent.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-remote-retract-subsequent.ttl");
        test(input,expected);
    }
    
    @Test
    public void wrongRemoteSelfRetractRetractionTest () throws IOException {
        Dataset input = loadDataset( inputFolder + "wrong-remote-selfretract.trig");
        Model expected = customLoadModel( expectedOutputFolder + "wrong-remote-selfretract.ttl");
        test(input,expected);
    }
    
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
	//	  OpenProposesToCancelFunction instance = new OpenProposesToCancelFunction();
		  Model actual = ClosedRetractsFunction.sparqlTest(input);
		  		  
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
        FileManager.get().addLocatorClassLoader(OpenProposesToCancelTest.class.getClassLoader());
        Model model = FileManager.get().loadModel(prefix + path);
          
       return model;
   }
    
	
    private static Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = ClosedRetractsTest.class.getResourceAsStream(path);
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
