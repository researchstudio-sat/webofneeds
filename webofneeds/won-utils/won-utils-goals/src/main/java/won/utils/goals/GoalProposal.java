package won.utils.goals;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.validation.ValidationUtil;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.utils.shacl.ShaclReportWrapper;

import java.io.IOException;

public class GoalProposal {

    private Dataset need1;
    private Dataset need2;
    private Dataset conversation;
    private Model combinedModel;
    private String blendingUriPrefix;

    public GoalProposal(Dataset need1, Dataset need2, Dataset conversation, String blendingUriPrefix) {

        this.need1 = need1;
        this.need2 = need2;
        this.conversation = conversation;
        this.blendingUriPrefix = blendingUriPrefix;

        // create the combined dataset with all data of the two needs and the whole conversation
        Dataset combinedDataset = DatasetFactory.create();
        RdfUtils.addDatasetToDataset(combinedDataset, need1, false);
        RdfUtils.addDatasetToDataset(combinedDataset, need2, false);
        RdfUtils.addDatasetToDataset(combinedDataset, conversation, false);
        combinedModel = RdfUtils.mergeAllDataToSingleModel(combinedDataset);
    }

    public Model findValidProposalForGoals(String need1GoalUri, String need2GoalUri) throws IOException {

        NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
        NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
        Resource goal1 = needWrapper1.getGoal(need1GoalUri);
        Resource goal2 = needWrapper2.getGoal(need2GoalUri);
        Model shapesModel1 = needWrapper1.getShapesGraph(goal1);
        Model shapesModel2 = needWrapper2.getShapesGraph(goal2);

        // validate the two goal shapes against all data (from the two needs plus conversation)
        // and extract specific data for each goal using the shacl results
        Model extractedModel1 = GoalUtils.extractGoalData(combinedModel, shapesModel1);
        Model extractedModel2 = GoalUtils.extractGoalData(combinedModel, shapesModel2);

        // blend the two extracted graphs
        Model blendedModel = GoalUtils.blendGraphsSimple(extractedModel1, extractedModel2, blendingUriPrefix);

        // check the blended graph against the shacl shape graphs of both goals
        Resource report1 = ValidationUtil.validateModel(blendedModel, shapesModel1, false);
        Resource report2 = ValidationUtil.validateModel(blendedModel, shapesModel2, false);
        ShaclReportWrapper r1 = new ShaclReportWrapper(report1);
        ShaclReportWrapper r2 = new ShaclReportWrapper(report2);

        if (r1.isConform() && r2.isConform()) {
            return blendedModel;
        }

        return null;
    }

}
