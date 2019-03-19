package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;

/**
 * Expects a sparql select query that projects ?g ?s ?p ?o and generates a dataset with those results used as as quads.
 * 
 * @author fkleedorfer
 *
 */
public class DynamicDatasetToDatasetBySparqlGSPOSelectFunction extends SparqlFunction<Dataset, Dataset> {

    QuerySolutionMap initialBinding = new QuerySolutionMap();

    public DynamicDatasetToDatasetBySparqlGSPOSelectFunction(String sparqlFile) {
        super(sparqlFile);
    }

    public DynamicDatasetToDatasetBySparqlGSPOSelectFunction(String sparqlFile, QuerySolutionMap initialBinding) {
        super(sparqlFile);
        this.initialBinding = initialBinding;
    }

    @Override
    public Dataset apply(Dataset dataset) {
        dataset.begin(ReadWrite.READ);
        Dataset result = DatasetFactory.createGeneral();
        result.begin(ReadWrite.WRITE);
        Query query = QueryFactory.create(sparql);

        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, dataset, initialBinding)) {
            ResultSet resultSet = queryExecution.execSelect();
            RDFNode currentProposal = null;
            Model currentProposalContent = ModelFactory.createDefaultModel();

            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.next();
                RDFNode proposalNode = solution.get("g");
                if (currentProposal == null) {
                    // first solution: remember uri of first proposal
                    currentProposal = proposalNode;
                } else {
                    // at least 2nd solution: if the proposal uri has changed, our current proposal
                    // has been
                    // processed. add the model containing its triples to the dataset under
                    // the currentProposal URI and prepare a new empty model for the next proposal
                    if (!currentProposal.equals(proposalNode)) {
                        // we have seen all triples of currentProposal
                        result.addNamedModel(currentProposal.asResource().getURI(), currentProposalContent);
                        currentProposalContent = ModelFactory.createDefaultModel();
                    }
                    currentProposal = proposalNode;
                }
                // add current triple into currentAgreementModel
                RDFNode s = solution.get("s");
                RDFNode p = solution.get("p");
                RDFNode o = solution.get("o");
                Statement newStatement = new StatementImpl(s.asResource(), new PropertyImpl(p.asResource().getURI()),
                        o);
                currentProposalContent.add(newStatement);
            }
            // add the last model
            if (currentProposal != null) {
                result.addNamedModel(currentProposal.asResource().getURI(), currentProposalContent);
            }
            return result;
        } finally {
            dataset.commit();
            result.commit();
        }
    }

}
