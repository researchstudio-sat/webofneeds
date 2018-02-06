package won.utils.acknowledgement;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import won.protocol.util.RdfUtils;


import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class AcknowledgedSelection {
    private String queryString;
    private static final String queryFile = "/acknowledgement/query.sq";

    // Function that reads in the query file to a string using Apache's IOUtils and java.utils
    public AcknowledgedSelection() {
        InputStream is  = AcknowledgedSelection.class.getResourceAsStream(queryFile);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read queryString file", e);
        }
        this.queryString = writer.toString();
    }

    public Dataset applyAcknowledgedSelection(Dataset conversationDataset){
    	// creates an update request by grabbing the queryString variable, populated by the Acknowledged Section method
        UpdateRequest update = UpdateFactory.create(queryString);
       
        // create a clone of the input datast ... See the RdfUtils class comments
        Dataset copy = RdfUtils.cloneDataset(conversationDataset);
        
        // perform a sparql update with the clone and the acknowledgement/query.sq
        UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update,copy);
        updateProcessor.execute();
        return copy;
    }
}
