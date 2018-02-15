package won.utils.allaccepts;


import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.FileManager;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.highlevel.HighlevelFunctionFactory;
import won.protocol.util.RdfUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;

public class AllAcceptsTest {
	
    private static final String inputFolder = "/won/utils/allaccepts/input/";
    private static final String expectedOutputFolder = "file:///C:/DATA/DEV/workspace/webofneeds/webofneeds/won-utils/won-utils-conversation/src/test/resources/won/utils/allaccepts/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }

	@Test
	public void oneAccept() throws IOException {
	    Dataset input = loadDataset( inputFolder + "one-agreement.trig");
	    // commented out because this does not work
//	   Model expected2 = customloadModel( expectedOutputFolder + "one-agreement-one-unacceptedcancellation.ttl");	 

	  FileManager.get().addLocatorClassLoader(AllAcceptsTest.class.getClassLoader());
      Model expected = FileManager.get().loadModel( expectedOutputFolder + "one-agreement.ttl");
        test(input,expected);		
	}
	
	@Test
	public void twoAccepts() throws IOException {
	    Dataset input = loadDataset( inputFolder + "oneproposal-twoaccepts.trig");
	    // commented out because this does not work
//	   Model expected2 = customloadModel( expectedOutputFolder + "one-agreement-one-unacceptedcancellation.ttl");	 

	  FileManager.get().addLocatorClassLoader(AllAcceptsTest.class.getClassLoader());
      Model expected = FileManager.get().loadModel( expectedOutputFolder + "oneproposal-twoaccepts.trig");
        test(input,expected);		
	}
	
	public void test(Dataset input, Model expectedOutput) {

		  // perform a sparql query to convert input into actual...
		  Model actual = HighlevelFunctionFactory.getAllAccepts().apply(input);
		  
		  RDFList list = actual.createList(actual.listSubjects());
		  
		//  Iterator listiterator = list.iterator();
		  
		  ExtendedIterator<RDFNode> listiterator = list.iterator();
		  
		  while(listiterator.hasNext()) {
			    Object object = listiterator.next();
			    System.out.println(object.toString());
		  }
		  
		//  System.out.println(list.size());
		
		  /*
		 
		  NodeIterator listobjects = actual.listObjects();
		  while(listobjects.hasNext()) {
			  Object object = listobjects.next();
			  System.out.print(object.toString());
		  }
		  */
		  		  
	      RdfUtils.Pair<Model> diff = RdfUtils.diff(expectedOutput, actual); 

	       if (!(diff.getFirst().isEmpty() && diff.getSecond().isEmpty())) {
				 System.out.println("diff - only in expected:");
				 RDFDataMgr.write(System.out, diff.getFirst(), Lang.TRIG);
				 System.out.println("diff - only in actual:");
				 RDFDataMgr.write(System.out, diff.getSecond(), Lang.TRIG);

	       }

	       Assert.assertTrue(RdfUtils.areModelsIsomorphic(expectedOutput, actual)); 

}

	
    private static Model customloadModel(String path) throws IOException {

        InputStream is = null;
        Model model = null;
        try {
            is = AllAcceptsTest.class.getResourceAsStream(path);
            model = ModelFactory.createDefaultModel();
            RDFDataMgr.read(model, is, RDFFormat.TTL.getLang());      	
        } finally {
            if (is != null) {
                is.close();
            }
        }

        return model;
    }
    
	
    private static Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = AllAcceptsTest.class.getResourceAsStream(path);
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
