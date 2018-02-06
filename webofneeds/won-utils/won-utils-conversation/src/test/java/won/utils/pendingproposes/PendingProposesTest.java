package won.utils.pendingproposes;


import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.FileManager;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.util.RdfUtils;
import won.utils.pendingproposes.PendingProposesFunction;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public class PendingProposesTest {
	
    private static final String inputFolder = "/won/utils/pendingproposes/input/";
    private static final String expectedOutputFolder = "/won/utils/pendingproposes/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

 // This is the case where there is no open proposal...(each exist in their own envelope, both are accepted in an agreement)
	@Test
	public void noOpenProposal () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-bothaccepted.trig");	    
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-bothaccepted.ttl");
        test(input,expected);		
	}
	
	 // This is the case where there is one open proposal...(each exist in their own envelope, only one is accepted in an agreement)
	@Test
	public void oneOpenProposal () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-one-accepted.trig");	    
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-one-accepted.ttl");
        test(input,expected);		
	}
	
	// This is the case where there are two open proposals ...(each exist in their own envelope)
	@Test
	public void twoOpenProposals () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-noaccepted.trig");	    
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-noaccepted.ttl");
        test(input,expected);		
	}
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
	//	  OpenProposesToCancelFunction instance = new OpenProposesToCancelFunction();
		  Model actual = PendingProposesFunction.sparqlTest(input);
		  		  
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
        FileManager.get().addLocatorClassLoader(PendingProposesTest.class.getClassLoader());
        Model model = FileManager.get().loadModel(prefix + path);
          
       return model;
   }
    
	
    private static Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = PendingProposesTest.class.getResourceAsStream(path);
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
