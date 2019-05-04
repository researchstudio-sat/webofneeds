package won.utils.goals;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.topbraid.shacl.validation.ValidationUtil;

import won.protocol.model.AtomGraphType;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.utils.shacl.ShaclReportWrapper;

/**
 * Class supports in producing goal instantiations that can be used to create
 * proposals for instance. Goal instantiations are data models that evaluate
 * successfully against defined shacl shapes taken from two goals of two atoms.
 */
public class GoalInstantiationProducer {
    private Dataset atom1;
    private Dataset atom2;
    private Model combinedModelWithoutGoals;
    private String variableUriPrefix;
    private String blendingUriPrefix;

    /**
     * Initialize the GoalInstantiationProducer
     *
     * @param atom1 Dataset of atom 1
     * @param atom2 Dataset of atom 2
     * @param conversation Dataset of the conversation between two atoms
     * @param variableUriPrefix uri prefix defines which resource URIs are
     * considered for blending
     * @param blendingUriPrefix uri prefix that is used to generate the result URIs
     * of blended resources
     */
    public GoalInstantiationProducer(Dataset atom1, Dataset atom2, Dataset conversation, String variableUriPrefix,
                    String blendingUriPrefix) {
        this.atom1 = atom1;
        this.atom2 = atom2;
        this.variableUriPrefix = variableUriPrefix;
        this.blendingUriPrefix = blendingUriPrefix;
        // first remove all goals data and shapes graphs from atoms
        // so that different goals do not get mixed up with each other
        Model strippedAtom1 = getAtomContentModelWithoutGoals(atom1);
        Model strippedAtom2 = getAtomContentModelWithoutGoals(atom2);
        // create the combined dataset with the rest of the data of the two atoms and
        // the whole conversation
        Dataset combinedDataset = DatasetFactory.create();
        combinedDataset.addNamedModel("atom1", strippedAtom1);
        combinedDataset.addNamedModel("atom2", strippedAtom2);
        if (conversation != null) {
            String sparqlQuery = "PREFIX msg: <https://w3id.org/won/message#> \n" + "DELETE { \n"
                            + "    GRAPH ?g {?s ?p ?o} \n" + "} \n" + "WHERE { \n" + "    GRAPH ?g { ?s ?p ?o } \n"
                            + "    { SELECT (GROUP_CONCAT(?content; separator=\" \") as ?contentGraphs) \n"
                            + "            WHERE { GRAPH <urn:x-arq:UnionGraph> { ?msg msg:content ?content } \n"
                            + "    }\n" + "} \n" + "FILTER (!contains(?contentGraphs,str(?g))) \n" + "} \n";
            UpdateRequest update = UpdateFactory.create(sparqlQuery);
            UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update, conversation);
            updateProcessor.execute();
            RdfUtils.addDatasetToDataset(combinedDataset, conversation, false);
        }
        combinedModelWithoutGoals = RdfUtils.mergeAllDataToSingleModel(combinedDataset);
    }

    private Model getAtomContentModelWithoutGoals(Dataset atom) {
        AtomModelWrapper atomWrapper = new AtomModelWrapper(atom);
        Model atom1Model = atomWrapper.copyAtomModel(AtomGraphType.ATOM);
        for (Resource goal : atomWrapper.getGoals()) {
            RdfUtils.removeResource(atom1Model, goal);
        }
        return atom1Model;
    }

    /**
     * Create a goal instantiation result from the attempt to instantiate two goals
     * of two atoms using all the atom data, the conversation data and the data and
     * shapes of the two goals. If a model can be found that conforms to the shacl
     * shapes of both atoms this model is chosen to be returned in the
     * GoalInstantiationResult. If no conforming model can be found, the model with
     * the least shacl validation results (validation errors) is used.
     *
     * @param goal1 resource referencing goal from atom1
     * @param goal2 resource referencing goal from atom2
     * @return a goal instantiation result whose input model can either conform to
     * its shacl shapes or not
     */
    private GoalInstantiationResult findInstantiationForGoals(Resource goal1, Resource goal2) {
        AtomModelWrapper atomWrapper1 = new AtomModelWrapper(atom1);
        Model shapesModel1 = atomWrapper1.getShapesGraph(goal1);
        Model dataModel1 = atomWrapper1.getDataGraph(goal1);
        AtomModelWrapper atomWrapper2 = new AtomModelWrapper(atom2);
        Model shapesModel2 = atomWrapper2.getShapesGraph(goal2);
        Model dataModel2 = atomWrapper2.getDataGraph(goal2);
        if (shapesModel1 == null || shapesModel2 == null) {
            throw new IllegalArgumentException("shapes model for goal not found");
        }
        // create the combined model with atom content, conversation data and the data
        // of the two goals
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
        int minValidationResults = Integer.MAX_VALUE;
        GoalInstantiationResult bestGoalInstantiationResult = null;
        // blend the two extracted graphs
        GraphBlendingIterator blendingIterator = new GraphBlendingIterator(extractedModel1, extractedModel2,
                        variableUriPrefix, blendingUriPrefix);
        while (blendingIterator.hasNext()) {
            Model blendedModel = blendingIterator.next();
            // check if the blended model conforms to the combined shacl shapes of both
            // atoms
            Resource report = ValidationUtil.validateModel(blendedModel, combinedShapesModel, false);
            ShaclReportWrapper shaclReportWrapper = new ShaclReportWrapper(report);
            if (shaclReportWrapper.isConform()) {
                // if we found a blended model that is conform to the shacl shapes lets try to
                // condense it
                // as far as possible to get the minimum model that is still conform to the
                // shapes
                Function<Model, Boolean> modelTestingFunction = param -> GoalUtils.validateModelShaclConformity(param,
                                combinedShapesModel);
                Model condensedModel = RdfUtils.condenseModelByIterativeTesting(blendedModel, modelTestingFunction);
                bestGoalInstantiationResult = new GoalInstantiationResult(condensedModel, combinedShapesModel);
            } else {
                // if the model is not conform save it if it has the least validation results
                // found so far
                if (shaclReportWrapper.getValidationResults().size() < minValidationResults) {
                    minValidationResults = shaclReportWrapper.getValidationResults().size();
                    bestGoalInstantiationResult = new GoalInstantiationResult(blendedModel, combinedShapesModel);
                }
            }
        }
        return bestGoalInstantiationResult;
    }

    /**
     * Create a goal instantiation result from the attempt to instantiate one goal
     * with data of two atoms using all the atom data, the conversation data and the
     * shapes data of the goal. The data is extracted and validated against the
     * shacl shape of the goal.
     *
     * @param goal resource referencing goal from atom1 or atom2
     * @return a goal instantiation result whose input model can either conform to
     * its shacl shapes or not
     */
    public GoalInstantiationResult findInstantiationForGoal(Resource goal) {
        AtomModelWrapper atomWrapper1 = new AtomModelWrapper(atom1);
        AtomModelWrapper atomWrapper2 = new AtomModelWrapper(atom2);
        Model goalShapesModel = null;
        Model goalDataModel = null;
        if (atomWrapper1.getGoals().contains(goal) && !atomWrapper2.getGoals().contains(goal)) {
            goalShapesModel = atomWrapper1.getShapesGraph(goal);
            goalDataModel = atomWrapper1.getDataGraph(goal);
        } else if (atomWrapper2.getGoals().contains(goal) && !atomWrapper1.getGoals().contains(goal)) {
            goalShapesModel = atomWrapper2.getShapesGraph(goal);
            goalDataModel = atomWrapper2.getDataGraph(goal);
        } else {
            throw new IllegalArgumentException("problem to identify goal resource in one of the two atom models");
        }
        if (goalShapesModel == null) {
            throw new IllegalArgumentException("shapes model for goal not found");
        }
        Model combinedModelWithGoalData = ModelFactory.createDefaultModel();
        combinedModelWithGoalData.add(combinedModelWithoutGoals);
        if (goalDataModel != null) {
            combinedModelWithGoalData.add(goalDataModel);
        }
        Model extractedModel = GoalUtils.extractGoalData(combinedModelWithGoalData, goalShapesModel);
        return new GoalInstantiationResult(extractedModel, goalShapesModel);
    }

    /**
     * Create a goal instantiation result from the attempt to instantiate one goal
     * with data given in the dataset The data is extracted and validated against
     * the shacl shape of the goal.
     *
     * @param atom Dataset of the atom to retrieve the goalShapesModel from
     * @param goal resource referencing goal from atom1 or atom2
     * @param model Model that should be checked for goal validity
     * @return a goal instantiation result whose input model can either conform to
     * its shacl shapes or not
     */
    public static GoalInstantiationResult findInstantiationForGoalInDataset(Dataset atom, Resource goal, Model model) {
        AtomModelWrapper atomWrapper = new AtomModelWrapper(atom);
        Model goalShapesModel;
        if (atomWrapper.getGoals().contains(goal)) {
            goalShapesModel = atomWrapper.getShapesGraph(goal);
        } else {
            throw new IllegalArgumentException("problem to identify goal resource in the atom model");
        }
        if (goalShapesModel == null) {
            throw new IllegalArgumentException("shapes model for goal not found");
        }
        Model extractedModel = GoalUtils.extractGoalData(model, goalShapesModel);
        return new GoalInstantiationResult(extractedModel, goalShapesModel);
    }

    /**
     * create all possible goal instantiations between two atoms. That means trying
     * to combine each two goals of the two atoms.
     *
     * @return
     */
    public Collection<GoalInstantiationResult> createAllGoalCombinationInstantiationResults() {
        AtomModelWrapper atomWrapper1 = new AtomModelWrapper(atom1);
        AtomModelWrapper atomWrapper2 = new AtomModelWrapper(atom2);
        Collection<GoalInstantiationResult> results = new LinkedList<>();
        for (Resource goal1 : atomWrapper1.getGoals()) {
            for (Resource goal2 : atomWrapper2.getGoals()) {
                GoalInstantiationResult instantiationResult = findInstantiationForGoals(goal1, goal2);
                results.add(instantiationResult);
            }
        }
        return results;
    }

    /**
     * create all possible goal instantiations between two atoms. Including the
     * separate goals of each atom as well.
     *
     * @return
     */
    public Collection<GoalInstantiationResult> createAllGoalInstantiationResults() {
        AtomModelWrapper atomWrapper1 = new AtomModelWrapper(atom1);
        AtomModelWrapper atomWrapper2 = new AtomModelWrapper(atom2);
        Collection<GoalInstantiationResult> results = new LinkedList<>();
        boolean addedGoal2s = false;
        for (Resource goal1 : atomWrapper1.getGoals()) {
            results.add(findInstantiationForGoal(goal1));
            for (Resource goal2 : atomWrapper2.getGoals()) {
                GoalInstantiationResult instantiationResult = findInstantiationForGoals(goal1, goal2);
                results.add(instantiationResult);
                if (!addedGoal2s) {
                    results.add(findInstantiationForGoal(goal2));
                }
            }
            addedGoal2s = true;
        }
        if (!addedGoal2s) {
            for (Resource goal2 : atomWrapper2.getGoals()) {
                results.add(findInstantiationForGoal(goal2));
            }
        }
        return results;
    }

    public Collection<GoalInstantiationResult> createGoalInstantiationResultsForAtom1() {
        return createGoalInstantiationResults(atom1);
    }

    public Collection<GoalInstantiationResult> createGoalInstantiationResultsForAtom2() {
        return createGoalInstantiationResults(atom2);
    }

    private Collection<GoalInstantiationResult> createGoalInstantiationResults(Dataset atom) {
        AtomModelWrapper atomWrapper = new AtomModelWrapper(atom);
        Collection<GoalInstantiationResult> results = new LinkedList<>();
        for (Resource goal : atomWrapper.getGoals()) {
            results.add(findInstantiationForGoal(goal));
        }
        return results;
    }
}
