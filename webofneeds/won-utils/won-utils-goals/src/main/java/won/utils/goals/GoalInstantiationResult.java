package won.utils.goals;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import won.utils.shacl.ShaclReportWrapper;

import java.io.StringWriter;

public class GoalInstantiationResult {

    private Model instanceModel;
    private ShaclReportWrapper shaclReport;

    public GoalInstantiationResult(Model instanceModel, Resource shaclReport) {
        this.instanceModel = instanceModel;
        this.shaclReport = new ShaclReportWrapper(shaclReport);
    }

    public boolean isConform() {
        return shaclReport.isConform();
    }

    public Model getInstanceModel() {
        return instanceModel;
    }

    public ShaclReportWrapper getShaclReportWrapper() {
        return shaclReport;
    }

    public String toString() {
        StringWriter writer = new StringWriter();
        instanceModel.write(writer, "TRIG");
        writer.write("\n");
        shaclReport.getReport().getModel().write(writer, "TRIG");
        return writer.toString();
    }
}
