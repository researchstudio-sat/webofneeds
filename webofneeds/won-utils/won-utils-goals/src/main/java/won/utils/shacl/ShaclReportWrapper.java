package won.utils.shacl;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.vocabulary.SH;
import won.protocol.util.RdfUtils;

import java.util.Collection;
import java.util.LinkedList;

public class ShaclReportWrapper {

  private Resource report;
  private Resource reportResource;

  public ShaclReportWrapper(Resource report) {

    this.report = report;
    reportResource = RdfUtils.findOneSubjectResource(report.getModel(), RDF.type, SH.ValidationReport);
  }

  public Resource getReport() {
    return report;
  }

  public boolean isConform() {
    RDFNode node = RdfUtils.findOnePropertyFromResource(report.getModel(), reportResource, SH.conforms);
    if (node != null && node.asLiteral() != null) {
      return node.asLiteral().getBoolean();
    }

    return false;
  }

  public Collection<ValidationResultWrapper> getValidationResults() {
    Collection<ValidationResultWrapper> validationResults = new LinkedList<>();
    for (Statement statement : reportResource.listProperties(SH.result).toList()) {
      if (statement.getResource().getPropertyResourceValue(RDF.type).equals(SH.ValidationResult)) {
        validationResults.add(new ValidationResultWrapper(statement.getResource()));
      }
    }
    return validationResults;
  }
}
