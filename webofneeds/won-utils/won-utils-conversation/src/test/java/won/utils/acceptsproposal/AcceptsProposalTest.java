package won.utils.acceptsproposal;


import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.FileManager;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;

public class AcceptsProposalTest {

    private static final String inputFolder = "/won/utils/acceptsproposal/input/";
    private static final String expectedOutputFolder = "/won/utils/acceptsproposal/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

	@Test
	public void oneValidAccept() throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement.trig");
        Model expected = customLoadModel( expectedOutputFolder + "one-agreement.ttl");
        test(input,expected);		
	}
	
	@Test
	public void twoAccept() throws IOException {
	    Dataset input = loadDataset( inputFolder + "oneproposal-twoaccepts.trig");
	    // commented out because this does not work
//	   Model expected2 = customloadModel( expectedOutputFolder + "one-agreement-one-unacceptedcancellation.ttl");	 

	  FileManager.get().addLocatorClassLoader(AcceptsProposalTest.class.getClassLoader());
      Model expected = FileManager.get().loadModel( expectedOutputFolder + "oneproposal-twoaccepts.ttl");
        test(input,expected);		
	}
	
	@Test
	public void getAcceptsCancelledAgreement() throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement-one-cancellation.trig");
	    // commented out because this does not work
//	   Model expected2 = customloadModel( expectedOutputFolder + "one-agreement-one-unacceptedcancellation.ttl");	 

	  FileManager.get().addLocatorClassLoader(AcceptsProposalTest.class.getClassLoader());
      Model expected = FileManager.get().loadModel( expectedOutputFolder + "one-agreement-one-cancellation.ttl");
        test(input,expected);		
	}
	
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
		  Model actual = HighlevelFunctionFactory.getAcceptsProposesFunction().apply(input);
		  		  
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
