package won.utils.goals;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import won.protocol.model.NeedGraphType;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Class supports in producing goal instantiations that can be used to create proposals for instance.
 * Goal instantiations are data models that evaluate successfully against defined shacl shapes taken from
 * two goals of two needs.
 */
public class GoalInstantiationProducer {

    private Dataset need1;
    private Dataset need2;
    private Dataset conversation;
    private Model combinedModelWithoutGoals;
    private String variableUriPrefix;
    private String blendingUriPrefix;

    /**
     * Initialize the GoalInstantiationProducer
     *
     * @param need1 Dataset of need 1
     * @param need2 Dataset of need 2
     * @param conversation Dataset of the conversation between two needs
     * @param variableUriPrefix uri prefix defines which resource URIs are considered for blending
     * @param blendingUriPrefix uri prefix that is used to generate the result URIs of blended resources
     */
    public GoalInstantiationProducer(Dataset need1, Dataset need2, Dataset conversation, String variableUriPrefix, String blendingUriPrefix) {

        this.need1 = need1;
        this.need2 = need2;
        this.conversation = conversation;
        this.variableUriPrefix = variableUriPrefix;
        this.blendingUriPrefix = blendingUriPrefix;

        // first remove all goals data and shapes graphs from needs
        // so that different goals do not get mixed up with each other
        Model strippedNeed1 = getNeedContentModelWithoutGoals(need1);
        Model strippedNeed2 = getNeedContentModelWithoutGoals(need2);

        // create the combined dataset with the rest of the data of the two needs and the whole conversation
        Dataset combinedDataset = DatasetFactory.create();
        combinedDataset.addNamedModel("need1", strippedNeed1);
        combinedDataset.addNamedModel("need2", strippedNeed2);
        RdfUtils.addDatasetToDataset(combinedDataset, conversation, false);
        combinedModelWithoutGoals = RdfUtils.mergeAllDataToSingleModel(combinedDataset);
    }

    private Model getNeedContentModelWithoutGoals(Dataset need) {
        NeedModelWrapper needWrapper = new NeedModelWrapper(need);
        Model need1Model = needWrapper.copyNeedModel(NeedGraphType.NEED);
        for (Resource goal : needWrapper.getGoals()) {
            RdfUtils.removeResource(need1Model, goal);
        }
        return need1Model;
    }

    /**
     * Create a goal instantiation result from the attempt to instantiate two goals of two needs using all
     * the need data, the conversation data and the data and shapes of the two goals.
     *
     * @param goal1 resource referencing goal from need1
     * @param goal2 resource referencing goal from need2
     * @return
     */
    private GoalInstantiationResult findConformInstantiationForGoals(Resource goal1, Resource goal2) {

        NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
        Model shapesModel1 = needWrapper1.getShapesGraph(goal1);
        Model dataModel1 = needWrapper1.getDataGraph(goal1);
        NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
        Model shapesModel2 = needWrapper2.getShapesGraph(goal2);
        Model dataModel2 = needWrapper2.getDataGraph(goal2);

        // create the combined model with need content, conversation data and the data of the two goals
        Model combinedModelWithGoalData = ModelFactory.createDefaultModel();
        combinedModelWithGoalData.add(combinedModelWithoutGoals);
        if (dataModel1 != null) {
            combinedModelWithGoalData.add(dataModel1);
        }
        if (dataModel2 != null) {
            combinedModelWithGoalData.add(dataModel2);
        }

        // validate the two goal shapes against the combined data model
        // and extract specific data for each goal using the shacl results
        Model extractedModel1 = GoalUtils.extractGoalData(combinedModelWithGoalData, shapesModel1);
        Model extractedModel2 = GoalUtils.extractGoalData(combinedModelWithGoalData, shapesModel2);

        Model combinedShapesModel = ModelFactory.createDefaultModel();
        combinedShapesModel.add(shapesModel1);
        combinedShapesModel.add(shapesModel2);

        // blend the two extracted graphs
        GraphBlendingIterator blendingIterator = new GraphBlendingIterator(extractedModel1, extractedModel2, variableUriPrefix, blendingUriPrefix);
        while (blendingIterator.hasNext()) {
            Model blendedModel = blendingIterator.next();
            GoalInstantiationResult instantiationResult = new GoalInstantiationResult(blendedModel, combinedShapesModel);
            if (instantiationResult.isConform()) {
                return instantiationResult;
            }
        }

        return null;
    }

    /**
     * create all possible goal instantiations between two needs and retrieve only those models
     * that conform to the shape models of their goals.
     *
     * @return
     */
    public Collection<Model> createAllConformGoalInstantiationModels() {

        NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
        NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);

        Collection<Model> validInstantiationModels = new LinkedList<>();
        for (Resource goal1 : needWrapper1.getGoals()) {
            for (Resource goal2 : needWrapper2.getGoals()) {
                GoalInstantiationResult instantiationResult = findConformInstantiationForGoals(goal1, goal2);
                if (instantiationResult != null) {
                    validInstantiationModels.add(instantiationResult.getInstanceModel());
                }
            }
        }

        return validInstantiationModels;
    }

}
