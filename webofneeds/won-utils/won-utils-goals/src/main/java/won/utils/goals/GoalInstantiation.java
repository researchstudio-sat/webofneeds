package won.utils.goals;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.topbraid.shacl.validation.ValidationUtil;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;

import java.util.Collection;
import java.util.LinkedList;


public class GoalInstantiation {

    private Dataset need1;
    private Dataset need2;
    private Dataset conversation;
    private Model combinedModel;
    private String blendingUriPrefix;

    public GoalInstantiation(Dataset need1, Dataset need2, Dataset conversation, String blendingUriPrefix) {

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

    public GoalInstantiationResult findInstantiationForGoals(Resource goal1, Resource goal2) {

        NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
        NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
        Model shapesModel1 = needWrapper1.getShapesGraph(goal1);
        Model shapesModel2 = needWrapper2.getShapesGraph(goal2);

        // validate the two goal shapes against all data (from the two needs plus conversation)
        // and extract specific data for each goal using the shacl results
        Model extractedModel1 = GoalUtils.extractGoalData(combinedModel, shapesModel1, false);
        Model extractedModel2 = GoalUtils.extractGoalData(combinedModel, shapesModel2, false);

        // blend the two extracted graphs
        Model blendedModel = GoalUtils.blendGraphsSimple(extractedModel1, extractedModel2, blendingUriPrefix);

        // check the blended graph against the shacl shape graphs of both goals
        Model combinedShapesModel = ModelFactory.createDefaultModel();
        combinedShapesModel.add(shapesModel1);
        combinedShapesModel.add(shapesModel2);
        Resource report = ValidationUtil.validateModel(blendedModel, combinedShapesModel, false);

        return new GoalInstantiationResult(blendedModel, report);
    }

    public Collection<GoalInstantiationResult> createAllGoalInstantiationResults() {
        NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
        NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);

        Collection<GoalInstantiationResult> results = new LinkedList<>();
        for (Resource goal1 : needWrapper1.getGoals()) {
            for (Resource goal2 : needWrapper2.getGoals()) {
                GoalInstantiationResult instantiationResult = findInstantiationForGoals(goal1, goal2);
                results.add(instantiationResult);
            }
        }

        return results;
    }

    public Collection<Model> findAllValidGoalInstantiationModels() {

        NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
        NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);

        Collection<Model> validInstantiationModels = new LinkedList<>();
        for (Resource goal1 : needWrapper1.getGoals()) {
            for (Resource goal2 : needWrapper2.getGoals()) {
                GoalInstantiationResult instantiationResult = findInstantiationForGoals(goal1, goal2);
                if (instantiationResult.isConform()) {
                    validInstantiationModels.add(instantiationResult.getInstanceModel());
                }
            }
        }

        return validInstantiationModels;
    }

}
