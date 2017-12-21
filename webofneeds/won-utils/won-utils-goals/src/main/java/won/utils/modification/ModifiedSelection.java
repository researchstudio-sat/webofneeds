package won.utils.modification;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.update.*;
import won.protocol.util.RdfUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class ModifiedSelection {

    private String queryString;
    private static String queryFile = "/modification/query.sq";

    public ModifiedSelection() {
        InputStream is  = ModifiedSelection.class.getResourceAsStream(queryFile);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read queryString file", e);
        }
        this.queryString = writer.toString();
    }

    public Dataset applyModificationSelection(Dataset conversationDataset){
        UpdateRequest update = UpdateFactory.create(queryString);
        Dataset copy = RdfUtils.cloneDataset(conversationDataset);
        UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update,copy);
        updateProcessor.execute();
        return copy;
    }
}
