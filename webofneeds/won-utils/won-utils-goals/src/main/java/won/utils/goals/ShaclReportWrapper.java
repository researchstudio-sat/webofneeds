package won.utils.goals;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import won.protocol.util.RdfUtils;

public class ShaclReportWrapper {

    private Resource report;
    private Resource reportResource;

    private static final String SHACL_BASE_URI = "http://www.w3.org/ns/shacl#";
    private static Model m = ModelFactory.createDefaultModel();
    private static final Resource SHACL_VALIDATION_REPORT = m.createProperty(SHACL_BASE_URI, "ValidationReport");
    private static final Property SHACL_CONFORMS = m.createProperty(SHACL_BASE_URI, "conforms");

    public ShaclReportWrapper(Resource report) {

        this.report = report;
        reportResource = RdfUtils.findOneSubjectResource(report.getModel(), RDF.type, SHACL_VALIDATION_REPORT);
    }

    public boolean isConform() {
        RDFNode node = RdfUtils.findOnePropertyFromResource(report.getModel(), reportResource, SHACL_CONFORMS);
        if (node != null && node.asLiteral() != null) {
            return node.asLiteral().getBoolean();
        }

        return false;
    }
}
