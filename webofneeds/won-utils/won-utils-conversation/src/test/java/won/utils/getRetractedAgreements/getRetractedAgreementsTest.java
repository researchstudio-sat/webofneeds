package won.utils.getRetractedAgreements;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.FileManager;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import won.protocol.agreement.AgreementProtocol;

public class getRetractedAgreementsTest {
	
    private static final String inputFolder = "/won/utils/getRetractedAgreements/input/";
    private static final String expectedOutputFolder = "/won/utils/getRetractedAgreements/expected/";
    
    @BeforeClass
    public static void setLogLevel() {
    	Logger root = (Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    	root.setLevel(Level.INFO);	
    }
    
    // This is the case where there is no open proposal...(each exist in their own envelope, both are accepted in an agreement)
    @Test @Ignore
    public void noOpenProposal () throws IOException, URISyntaxException {
        Dataset input = loadDataset( inputFolder + "2proposal-2agreements-2cancellationproposalonemsg-1clauses-twoaccepted.trig");
   //     Dataset expectedOutput = loadDataset( expectedOutputFolder + "2proposal-2agreements-2cancellationproposalonemsg-1clauses-twoaccepted.ttl");
        test(input);
    }
    
	
	public void test(Dataset input) {

		  // perform a sparql query to convert input into actual...
		URI acceptsMessageURI = null;
		try {
			acceptsMessageURI = new URI("https://localhost:8443/won/resource/event/uu3ciy3btq6tg90crr3b");
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 List<URI> actual = AgreementProtocol.getRetractedAgreements(input, acceptsMessageURI);
		
	        for(URI uri : actual) {
	        	System.out.println(uri.toString());
	        }
}
	
	private static Model customLoadModel(String path) throws IOException {

		String prefix = "file:///C:/DATA/DEV/workspace/webofneeds/webofneeds/won-utils/won-utils-conversation/src/test/resources";
        FileManager.get().addLocatorClassLoader(getRetractedAgreementsTest.class.getClassLoader());
        Model model = FileManager.get().loadModel(prefix + path);
          
       return model;
   }
    
	
    private static Dataset loadDataset(String path) throws IOException {

        InputStream is = null;
        Dataset dataset = null;
        try {
            is = getRetractedAgreementsTest.class.getResourceAsStream(path);
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
