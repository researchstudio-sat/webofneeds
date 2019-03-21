package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

public class DatasetSelectionBySparqlFunction extends SparqlFunction<Dataset, Dataset> {

	public DatasetSelectionBySparqlFunction(String sparqlFile) {
		super(sparqlFile);
	}

	@Override
	public Dataset apply(Dataset dataset) {
		// creates an update request by grabbing the queryString variable, populated by the Acknowledged Section method
        UpdateRequest update = UpdateFactory.create(sparql);
       
        // create a clone of the input datast ... See the RdfUtils class comments
        Dataset copy = RdfUtils.cloneDataset(dataset);
        
        // perform a sparql update with the clone and the acknowledgement/query.sq
        UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update,copy);
        updateProcessor.execute();
        return copy;
	}

}
