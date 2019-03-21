package won.utils.goals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.validation.ValidationUtil;
import won.utils.shacl.ShaclReportWrapper;

import java.io.StringWriter;

/**
 * Class describes the outcome of a goal instantiation attempt. Instance model
 * represents the extracted instance data. Shacl shapes model describes the
 * shapes model that is evaluated against the instance model. And Shacl report
 * wrapper describes the shacl report outcome from the shacl evaluation.
 */
public class GoalInstantiationResult {

  private Model instanceModel;
  private ShaclReportWrapper shaclReportWrapper;
  private Model shaclShapesModel;

  public GoalInstantiationResult(Model instanceModel, Model shaclShapesModel) {
    this.instanceModel = instanceModel;
    this.shaclShapesModel = shaclShapesModel;
    Resource report = ValidationUtil.validateModel(instanceModel, shaclShapesModel, false);
    shaclReportWrapper = new ShaclReportWrapper(report);
  }

  /**
   * tells if the instance model conforms to the shacl shapes model
   *
   * @return true if, there are no shacl validation errors in the evaluation of
   *         the instance model against the shacl shapes model. False otherwise.
   */
  public boolean isConform() {
    return shaclReportWrapper.isConform();
  }

  public Model getInstanceModel() {
    return instanceModel;
  }

  public ShaclReportWrapper getShaclReportWrapper() {
    return shaclReportWrapper;
  }

  public Model getShaclShapesModel() {
    return shaclShapesModel;
  }

  public String toString() {
    StringWriter writer = new StringWriter();
    instanceModel.write(writer, "TRIG");
    writer.write("\n");
    shaclReportWrapper.getReport().getModel().write(writer, "TRIG");
    return writer.toString();
  }
}
