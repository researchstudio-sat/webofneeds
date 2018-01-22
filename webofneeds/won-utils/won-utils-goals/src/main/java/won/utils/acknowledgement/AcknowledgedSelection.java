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
        UpdateRequest update = UpdateFactory.create(queryString);
        Dataset copy = RdfUtils.cloneDataset(conversationDataset);
        UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update,copy);
        updateProcessor.execute();
        return copy;
    }
}
