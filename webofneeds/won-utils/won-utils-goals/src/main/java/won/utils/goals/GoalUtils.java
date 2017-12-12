package won.utils.goals;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.shared.NotFoundException;
import org.apache.jena.util.ResourceUtils;
import org.topbraid.shacl.validation.ValidationUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoalUtils {

    private static final String goalExtractionQueryWithFilter;
    private static final String goalExtractionQueryWithoutFilter;

    static {
        goalExtractionQueryWithFilter = loadSparqlQuery("/won/utils/goals/extraction/goal-extraction-with-validation-error-filter.sq");
        goalExtractionQueryWithoutFilter = loadSparqlQuery("/won/utils/goals/extraction/goal-extraction-without-filter.sq");
    }

    private static String loadSparqlQuery(String filePath) {
        InputStream is  = GoalUtils.class.getResourceAsStream(filePath);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer, Charsets.UTF_8);
        } catch (IOException e) {
            throw new NotFoundException("failed to load resource: " + filePath);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        return writer.toString();
    }

    /**
     * Extracts data from a data model while considering shacl shapes expressing constraints on that data model.
     * The method executes shacl validation using the shapes model on the data model. The goal data is extracted
     * from the resulting report as well as the shapes and data model. A specific sparql query loaded from resources
     * specifies which data will be extracted. The extracted data will contain all focus nodes that were actually
     * referenced in the shapes model.
     * For details on the extraction refer to the sparql queries:
     * /won/utils/goals/extraction/goal-extraction-with-validation-error-filter.sq
     * /won/utils/goals/extraction/goal-extraction-without-filter.sq
     *
     * @param dataModel The data model is expected to contain the rdf data which is usually the merged data of two
     *                  needs and their conversation from which the goal data is gonna be extracted.
     * @param shaclShapesModel the shapes model specifies shacl constraints for the data that should be extracted
     *                         from the data model
     * @param withValidationErrorFilters if true extracts only those focus nodes which have no validation errors,
     *                                   if false extracts all focus nodes
     *
     * @return
     */
    public static Model extractGoalData(Model dataModel, Model shaclShapesModel, boolean withValidationErrorFilters) {

        Resource report = ValidationUtil.validateModel(dataModel, shaclShapesModel, false);
        Model combinedModel = ModelFactory.createDefaultModel();
        combinedModel.add(dataModel);
        combinedModel.add(report.getModel());
        combinedModel.add(shaclShapesModel);
        String sparqlQuery = (withValidationErrorFilters) ? goalExtractionQueryWithFilter : goalExtractionQueryWithoutFilter;
        Query query = QueryFactory.create(sparqlQuery);
        QueryExecution qexec = QueryExecutionFactory.create(query ,combinedModel);
        Model result = qexec.execConstruct();
        return result;
    }

    /**
     * Blends two model graphs into a new graph. For a description of blending check the design documents:
     * https://github.com/researchstudio-sat/webofneeds/tree/goal-concept/documentation/
     *
     * This method implements a simple version of blending since it just recursively replaces all
     * subject nodes by new uris if there are 2 sets of triples in the two graphs found that have the same
     * predicate and object (but different subject).
     *
     * @param dataGraph1 model graph 1
     * @param dataGraph2 model graph 2
     * @param blendingUriPrefix must be a unique prefix since blended node URIs receive this prefix
     * @return
    **/
    public static Model blendGraphsSimple(Model dataGraph1, Model dataGraph2, String blendingUriPrefix) {

        Map<String, String> prefixes = new HashMap<>();
        prefixes.putAll(dataGraph1.getNsPrefixMap());
        prefixes.putAll(dataGraph2.getNsPrefixMap());

        Model blendedModel = ModelFactory.createDefaultModel();
        blendedModel.setNsPrefixes(prefixes);

        // add all triples together in one graph
        blendedModel.add(dataGraph1.listStatements());
        blendedModel.add(dataGraph2.listStatements());

        // for every pair of triples check if they have same predicate and object
        // if so, remove one triple pair and rename the subject resources of both to match
        // repeat until we cannot blend nodes anymore
        boolean nodesBlended;
        int numBlended = 0;
        do {
            nodesBlended = false;
            List<Statement> stmts = blendedModel.listStatements().toList();
            for (int i = 0; i < stmts.size(); i++) {
                for (int j = i + 1; j < stmts.size(); j++) {
                    Statement stmt1 = stmts.get(i);
                    Statement stmt2 = stmts.get(j);
                    if (stmt1.getObject().equals(stmt2.getObject()) && stmt1.getPredicate().equals(stmt2.getPredicate())) {
                        blendedModel.remove(stmt2);
                        String blendedResourceUri = blendingUriPrefix + numBlended;
                        ResourceUtils.renameResource(stmt1.getSubject(), blendedResourceUri);
                        ResourceUtils.renameResource(stmt2.getSubject(), blendedResourceUri);
                        numBlended++;
                        nodesBlended = true;

                        // break for loops
                        i = stmts.size();
                        break;
                    }
                }
            }
        } while (nodesBlended);

        return blendedModel;
    }

}
