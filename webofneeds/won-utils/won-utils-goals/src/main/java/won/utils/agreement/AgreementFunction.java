package won.utils.agreement;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import won.protocol.util.RdfUtils;
import won.utils.goals.extraction.DataExtraction;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class AgreementFunction {
    private String queryString;
    private static String queryFile = "/agreement/query.sq";

    public AgreementFunction() {
        InputStream is  = DataExtraction.class.getResourceAsStream(queryFile);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read queryString file", e);
        }
        this.queryString = writer.toString();
    }

    public Dataset applyAgreementFunction(Dataset conversationDataset){
        Dataset result = DatasetFactory.createGeneral();
        Query query = QueryFactory.create(queryString);
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, conversationDataset)) {
            ResultSet resultSet = queryExecution.execSelect();
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
                currentAgreementContent.add(new StatementImpl(s.asResource(), new PropertyImpl(p.asResource().getURI()), o));
            }
            //add the last model
            result.addNamedModel(currentAgreement.asResource().getURI(), currentAgreementContent);
            return result;
        }
    }
}
