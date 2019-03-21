package won.utils.shacl;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.vocabulary.SH;

public class ValidationResultWrapper {

    private Resource validationResult;

    public ValidationResultWrapper(Resource validationResult) {

        this.validationResult = validationResult;
        if (!validationResult.getPropertyResourceValue(RDF.type).equals(SH.ValidationResult)) {
            throw new IllegalArgumentException("Resource is not of type " + SH.ValidationResult.toString());
        }
    }

    public Resource getFocusNode() {
        return validationResult.getPropertyResourceValue(SH.focusNode);
    }

    public Resource getResultSeverity() {
        return validationResult.getPropertyResourceValue(SH.resultSeverity);
    }

    public Resource getSourceConstraintComponent() {
        return validationResult.getPropertyResourceValue(SH.sourceConstraintComponent);
    }

    public Resource getResultPath() {
       return validationResult.getPropertyResourceValue(SH.resultPath);
    }

    public String getResultMessage() {
        StmtIterator stmtIterator = validationResult.listProperties(SH.resultMessage);
        return (stmtIterator.hasNext()) ? stmtIterator.nextStatement().getString() : null;
    }

    public Resource getValue() {
        return validationResult.getPropertyResourceValue(SH.value);
    }

    public Resource getSourceShape() {
        return validationResult.getPropertyResourceValue(SH.sourceShape);
    }

    public Resource getDetail() {
        return validationResult.getPropertyResourceValue(SH.detail);
    }
}
