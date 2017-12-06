package won.utils.goals.extraction;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Iterator;

public class DataExtraction {

    String sparqlExtractionQuery;

    public DataExtraction() throws IOException {

        InputStream is  = DataExtraction.class.getResourceAsStream("/won/utils/goals/extraction/sparql-query-for-extraction.sq");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, Charsets.UTF_8);
        sparqlExtractionQuery = writer.toString();
    }

    public Model extract(Dataset ds) {

        // merge default graph and all named graph data into the default graph to be able to query all of it at once
        Model mergedModel = ModelFactory.createDefaultModel();
        Iterator<String> nameIter = ds.listNames();
        mergedModel.add(ds.getDefaultModel());
        while (nameIter.hasNext()) {
            mergedModel.add(ds.getNamedModel(nameIter.next()));
        }
        return extract(mergedModel);
    }

    public Model extract(Model model) {
        Query query = QueryFactory.create(sparqlExtractionQuery);
        QueryExecution qexec = QueryExecutionFactory.create(query ,model);
        Model result = qexec.execConstruct();
        return result;
    }
}
