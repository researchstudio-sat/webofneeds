package won.utils.goals;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.NotFoundException;
import org.topbraid.shacl.validation.ValidationUtil;

import won.utils.shacl.ShaclReportWrapper;

/**
 * Utils class for handling goals related actions like graph blending and data extraction of goals instantiation.
 */
public class GoalUtils {

    private static final String goalExtractionQuery;

    static {
        goalExtractionQuery = loadSparqlQuery("/won/utils/goals/extraction/goal-extraction-only-referenced-properties.rq");
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
     * referenced in the shapes model and only these properties that were referenced in the shapes model.
     * For details on the extraction refer to the sparql queries:
     * /won/utils/goals/extraction/goal-extraction-only-referenced-properties.rq
     *
     * @param dataModel The data model is expected to contain the rdf data which is usually the merged data of two
     *                  needs and their conversation from which the goal data is gonna be extracted.
     * @param shaclShapesModel the shapes model specifies shacl constraints for the data that should be extracted
     *                         from the data model
     *
     * @return
     */
    public static Model extractGoalData(Model dataModel, Model shaclShapesModel) {
        Resource report = ValidationUtil.validateModel(dataModel, shaclShapesModel, false);
        Model combinedModel = ModelFactory.createDefaultModel();
        combinedModel.add(dataModel);
        combinedModel.add(report.getModel());
        combinedModel.add(shaclShapesModel);
        Query query = QueryFactory.create(goalExtractionQuery);
        try (QueryExecution qexec = QueryExecutionFactory.create(query ,combinedModel)) {
            Model result = qexec.execConstruct();
            return result;
        }
    }

    public static Boolean validateModelShaclConformity(Model dataModel, Model shaclShapesModel) {

        Resource report = ValidationUtil.validateModel(dataModel, shaclShapesModel, false);
        ShaclReportWrapper shaclReportWrapper = new ShaclReportWrapper(report);
        return shaclReportWrapper.isConform();
    }

}
