package won.utils.openproposestocancel;


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
import won.utils.proposaltocancel.ProposalToCancelTest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public class OpenProposesToCancelTest {
	
    private static final String inputFolder = "/won/utils/openproposestocancel/input/";
    private static final String expectedOutputFolder = "/won/utils/openproposestocancel/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

	@Test
	public void oneOpenCancellationPropsoal() throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-unacceptedcancellation.trig");
	    Model expected = customLoadModel( expectedOutputFolder  + "one-agreement-one-unacceptedcancellation.ttl");
        test(input,expected);		
	}
	
	@Test
	public void twoOpenCancellationOneCancellationSameProposal() throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-1cancellationproposal-2clauses-noneaccepted.trig");
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-2agreements-1cancellationproposal-2clauses-noneaccepted.ttl");    
        test(input,expected);		
	}
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
	//	  OpenProposesToCancelFunction instance = new OpenProposesToCancelFunction();
		  Model actual = OpenProposesToCancelFunction.sparqlTest(input);
		  		  
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

          String prefix = "file:///C:/DATA/DEV/workspace/webofneeds/webofneeds/won-utils/won-utils-goals/src/test/resources";
          FileManager.get().addLocatorClassLoader(OpenProposesToCancelTest.class.getClassLoader());
          Model model = FileManager.get().loadModel(prefix + path);
		  
          return model;
   }
	
	
    private static Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = ProposalToCancelTest.class.getResourceAsStream(path);
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
