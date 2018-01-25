package won.utils.agreement;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;

public class AgreementFunction {
    private static final String AGREEMENT_SUFFIX = "";
	private String queryString;
    private static final String queryFile = "/agreement/query.sq";

    public AgreementFunction() {
        InputStream is  = AgreementFunction.class.getResourceAsStream(queryFile);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read query file from classpath location " + queryFile, e);
        }
        this.queryString = writer.toString();
    }

    public Dataset applyAgreementFunction(Dataset conversationDataset){
    	conversationDataset.begin(ReadWrite.READ);
        Dataset result = DatasetFactory.createGeneral();
        // adds a write lock so this function has exclusive access tow write..
        result.begin(ReadWrite.WRITE);
        Query query = QueryFactory.create(queryString);
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, conversationDataset)) {
            ResultSet resultSet = queryExecution.execSelect();
            // Sets an object to hold the current agreement
            RDFNode currentAgreement = null;
            Model currentAgreementContent = ModelFactory.createDefaultModel();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.next();
                RDFNode agreementNode = solution.get("acc");
                if (currentAgreement == null) {
                    //first solution: remember uri of first agreement
                    currentAgreement = agreementNode;
                } else {
                    //at least 2nd solution: if the agreement uri has changed, our current agreement has been
                    //processed. add the model containing its triples to the dataset under
                    //the currentAgreement URI and prepare a new empty model for the next agreement
                    if (!currentAgreement.equals(agreementNode)) {
                        //we have seen all triples of currentAgreement
                        result.addNamedModel(currentAgreement.asResource().getURI(), currentAgreementContent);
                        currentAgreementContent = ModelFactory.createDefaultModel();
                    }
                    currentAgreement = agreementNode;
                }
                //add current triple into currentAgreementModel
                RDFNode s = solution.get("s");
                RDFNode p = solution.get("p");
                RDFNode o = solution.get("o");
                // grabs the content of the agreement
                Statement newStatement = new StatementImpl(s.asResource(), new PropertyImpl(p.asResource().getURI()), o);
                // places the contents of the agreement in the agreement graph by adding it the agreement object
                currentAgreementContent.add(newStatement);
            }
            //add the last model
            if (currentAgreement != null) {
            	result.addNamedModel(currentAgreement.asResource().getURI()+AGREEMENT_SUFFIX, currentAgreementContent);
            }
            return result;
        } finally {
        	// this may still fail, and need to be changed... it may need another catch block at least
        	conversationDataset.commit();
        	result.commit();
        }
    }
    
}
