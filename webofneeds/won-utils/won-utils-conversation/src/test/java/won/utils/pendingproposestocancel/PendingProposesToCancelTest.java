package won.utils.pendingproposestocancel;


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

public class PendingProposesToCancelTest {
	
    private static final String inputFolder = "/won/utils/pendingproposestocancel/input/";
    private static final String expectedOutputFolder = "/won/utils/pendingproposestocancel/expected/";
    
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
	public void twoOpenCancellationOneCancellationSameProposal () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-1cancellationproposal-2clauses-noneaccepted.trig");
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-2agreements-1cancellationproposal-2clauses-noneaccepted.ttl");    
        test(input,expected);		
	}
	
	@Test
	public void twoProposaltwoAgreementstwoCancellationProposalClausesOneAccepted () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.trig");
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-2agreements-2cancellationproposal-1clauses-oneaccepted.ttl");    
        test(input,expected);		
	}
	
	@Test
	public void twoProposaltwoAgreementstwoCancellationProposalClausesBothAccepted () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-2cancellationproposal-1clauses-bothaccepted.trig");
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-2agreements-2cancellationproposal-1clauses-bothaccepted.ttl");    
        test(input,expected);		
	}	
	
	@Test
	public void oneClosedCancellationOneCancellationErrorSameProposal () throws IOException {
	    Dataset input = loadDataset( inputFolder + "2proposal-2agreements-1cancellationproposal-2clauses-onefail.trig");
	    Model expected = customLoadModel( expectedOutputFolder  + "2proposal-2agreements-1cancellationproposal-2clauses-onefail.ttl");    
        test(input,expected);		
	}
	
    @Test
    public void twoClosedCancellationOneCancellationSameProposal () throws IOException {
        Dataset input = loadDataset( inputFolder + "2proposal-2agreements-1cancellationproposal-2clauses-bothsucceed.trig");
        Model expected = customLoadModel( expectedOutputFolder + "2proposal-2agreements-1cancellationproposal-2clauses-bothsucceed.ttl");
        test(input,expected);	
    }
		
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
		Model actual = HighlevelFunctionFactory.getPendingProposesToCancelFunction().apply(input);
		  		  
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
