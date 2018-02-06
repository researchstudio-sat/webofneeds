package won.utils.pendingproposes;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import won.utils.QueryLoader;

public class PendingProposesFunction {
	
	private static final String queryFile = "/pendingproposes/query.sq";
	
	public static Model sparqlTest(Dataset dataset)
	{
		Model mold = ModelFactory.createDefaultModel();
	
		String queryString = new QueryLoader(queryFile).getQueryAsString();
	
		Query query = QueryFactory.create(queryString);
		      
        QueryExecution qexec = QueryExecutionFactory.create(query, dataset);
        
        mold = qexec.execConstruct();
        
        return mold;
       				   
	}

}