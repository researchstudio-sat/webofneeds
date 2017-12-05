package won.utils.goals.extraction;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class DataExtraction {

    String sparqlExtractionQuery;

    public DataExtraction() throws IOException {

        InputStream is  = DataExtraction.class.getResourceAsStream("/won/utils/goals/extraction/sparql-query-for-extraction.sq");
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer, Charsets.UTF_8);
        sparqlExtractionQuery = writer.toString();
    }

    public Dataset extract(Dataset ds) {

        Query query = QueryFactory.create(sparqlExtractionQuery);
        QueryExecution qexec = QueryExecutionFactory.create(query, ds);
        Dataset result = qexec.execConstructDataset();
        return result;
    }
}
