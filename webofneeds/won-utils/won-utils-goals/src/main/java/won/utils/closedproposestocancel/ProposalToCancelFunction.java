package won.utils.closedproposestocancel;

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

public class ProposalToCancelFunction {
    private static final String PROPOSAL_TO_CANCEL_SUFFIX = "";
	private String queryString;
    private static final String queryFile = "/proposaltocancel/query.sq";

    public ProposalToCancelFunction() {
        InputStream is  = ProposalToCancelFunction.class.getResourceAsStream(queryFile);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read query file from classpath location " + queryFile, e);
        }
        this.queryString = writer.toString();
    }

    public Dataset applyProposalToCancelFunction(Dataset conversationDataset){
    	conversationDataset.begin(ReadWrite.READ);
        Dataset result = DatasetFactory.createGeneral();
        result.begin(ReadWrite.WRITE);
        Query query = QueryFactory.create(queryString);
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, conversationDataset)) {
            ResultSet resultSet = queryExecution.execSelect();
            RDFNode currentProposal = null;
            Model currentProposalContent = ModelFactory.createDefaultModel();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.next();
                RDFNode proposalNode = solution.get("openprop");
                if (currentProposal == null) {
                    //first solution: remember uri of first proposal
                    currentProposal = proposalNode;
                } else {
                    //at least 2nd solution: if the proposal uri has changed, our current proposal has been
                    //processed. add the model containing its triples to the dataset under
                    //the currentProposal URI and prepare a new empty model for the next proposal
                    if (!currentProposal.equals(proposalNode)) {
                        //we have seen all triples of currentProposal
                        result.addNamedModel(currentProposal.asResource().getURI(), currentProposalContent);
                        currentProposalContent = ModelFactory.createDefaultModel();
                    }
                    currentProposal = proposalNode;
                }
                //add current triple into currentAgreementModel
                RDFNode s = solution.get("openclause");
                RDFNode p = solution.get("openp");
                RDFNode o = solution.get("openo");
                Statement newStatement = new StatementImpl(s.asResource(), new PropertyImpl(p.asResource().getURI()), o);
                currentProposalContent.add(newStatement);
            }
            //add the last model
            if (currentProposal != null) {
            	result.addNamedModel(currentProposal.asResource().getURI()+PROPOSAL_TO_CANCEL_SUFFIX, currentProposalContent);
            }
            return result;
        } finally {
        	conversationDataset.commit();
        	result.commit();
        }
    }
    
}
