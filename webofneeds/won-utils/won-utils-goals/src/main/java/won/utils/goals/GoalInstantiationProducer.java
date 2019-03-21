package won.utils.goals;

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
import won.protocol.model.NeedGraphType;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.utils.shacl.ShaclReportWrapper;

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Function;

/**
 * Class supports in producing goal instantiations that can be used to create
 * proposals for instance. Goal instantiations are data models that evaluate
 * successfully against defined shacl shapes taken from two goals of two needs.
 */
public class GoalInstantiationProducer {

  private Dataset need1;
  private Dataset need2;
  private Model combinedModelWithoutGoals;
  private String variableUriPrefix;
  private String blendingUriPrefix;

  /**
   * Initialize the GoalInstantiationProducer
   *
   * @param need1             Dataset of need 1
   * @param need2             Dataset of need 2
   * @param conversation      Dataset of the conversation between two needs
   * @param variableUriPrefix uri prefix defines which resource URIs are
   *                          considered for blending
   * @param blendingUriPrefix uri prefix that is used to generate the result URIs
   *                          of blended resources
   */
  public GoalInstantiationProducer(Dataset need1, Dataset need2, Dataset conversation, String variableUriPrefix,
      String blendingUriPrefix) {

    this.need1 = need1;
    this.need2 = need2;
    this.variableUriPrefix = variableUriPrefix;
    this.blendingUriPrefix = blendingUriPrefix;

    // first remove all goals data and shapes graphs from needs
    // so that different goals do not get mixed up with each other
    Model strippedNeed1 = getNeedContentModelWithoutGoals(need1);
    Model strippedNeed2 = getNeedContentModelWithoutGoals(need2);

    // create the combined dataset with the rest of the data of the two needs and
    // the whole conversation
    Dataset combinedDataset = DatasetFactory.create();
    combinedDataset.addNamedModel("need1", strippedNeed1);
    combinedDataset.addNamedModel("need2", strippedNeed2);
    if (conversation != null) {
      String sparqlQuery = "PREFIX msg: <http://purl.org/webofneeds/message#> \n" + "DELETE { \n"
          + "    GRAPH ?g {?s ?p ?o} \n" + "} \n" + "WHERE { \n" + "    GRAPH ?g { ?s ?p ?o } \n"
          + "    { SELECT (GROUP_CONCAT(?content; separator=\" \") as ?contentGraphs) \n"
          + "            WHERE { GRAPH <urn:x-arq:UnionGraph> { ?msg msg:hasContent ?content } \n" + "    }\n" + "} \n"
          + "FILTER (!contains(?contentGraphs,str(?g))) \n" + "} \n";

      UpdateRequest update = UpdateFactory.create(sparqlQuery);

      UpdateProcessor updateProcessor = UpdateExecutionFactory.create(update, conversation);
      updateProcessor.execute();

      RdfUtils.addDatasetToDataset(combinedDataset, conversation, false);
    }
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
   * Create a goal instantiation result from the attempt to instantiate two goals
   * of two needs using all the need data, the conversation data and the data and
   * shapes of the two goals. If a model can be found that conforms to the shacl
   * shapes of both needs this model is chosen to be returned in the
   * GoalInstantiationResult. If no conforming model can be found, the model with
   * the least shacl validation results (validation errors) is used.
   *
   * @param goal1 resource referencing goal from need1
   * @param goal2 resource referencing goal from need2
   * @return a goal instantiation result whose input model can either conform to
   *         its shacl shapes or not
   */
  private GoalInstantiationResult findInstantiationForGoals(Resource goal1, Resource goal2) {

    NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
    Model shapesModel1 = needWrapper1.getShapesGraph(goal1);
    Model dataModel1 = needWrapper1.getDataGraph(goal1);
    NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
    Model shapesModel2 = needWrapper2.getShapesGraph(goal2);
    Model dataModel2 = needWrapper2.getDataGraph(goal2);

    if (shapesModel1 == null || shapesModel2 == null) {
      throw new IllegalArgumentException("shapes model for goal not found");
    }

    // create the combined model with need content, conversation data and the data
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
      // needs
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
   * with data of two needs using all the need data, the conversation data and the
   * shapes data of the goal. The data is extracted and validated against the
   * shacl shape of the goal.
   *
   * @param goal resource referencing goal from need1 or need2
   * @return a goal instantiation result whose input model can either conform to
   *         its shacl shapes or not
   */
  public GoalInstantiationResult findInstantiationForGoal(Resource goal) {

    NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
    NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);
    Model goalShapesModel = null;
    Model goalDataModel = null;

    if (needWrapper1.getGoals().contains(goal) && !needWrapper2.getGoals().contains(goal)) {
      goalShapesModel = needWrapper1.getShapesGraph(goal);
      goalDataModel = needWrapper1.getDataGraph(goal);
    } else if (needWrapper2.getGoals().contains(goal) && !needWrapper1.getGoals().contains(goal)) {
      goalShapesModel = needWrapper2.getShapesGraph(goal);
      goalDataModel = needWrapper2.getDataGraph(goal);
    } else {
      throw new IllegalArgumentException("problem to identify goal resource in one of the two need models");
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
   * @param need  Dataset of the need to retrieve the goalShapesModel from
   * @param goal  resource referencing goal from need1 or need2
   * @param model Model that should be checked for goal validity
   * @return a goal instantiation result whose input model can either conform to
   *         its shacl shapes or not
   */
  public static GoalInstantiationResult findInstantiationForGoalInDataset(Dataset need, Resource goal, Model model) {
    NeedModelWrapper needWrapper = new NeedModelWrapper(need);
    Model goalShapesModel;

    if (needWrapper.getGoals().contains(goal)) {
      goalShapesModel = needWrapper.getShapesGraph(goal);
    } else {
      throw new IllegalArgumentException("problem to identify goal resource in the need model");
    }

    if (goalShapesModel == null) {
      throw new IllegalArgumentException("shapes model for goal not found");
    }

    Model extractedModel = GoalUtils.extractGoalData(model, goalShapesModel);
    return new GoalInstantiationResult(extractedModel, goalShapesModel);
  }

  /**
   * create all possible goal instantiations between two needs. That means trying
   * to combine each two goals of the two needs.
   *
   * @return
   */
  public Collection<GoalInstantiationResult> createAllGoalCombinationInstantiationResults() {
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

  /**
   * create all possible goal instantiations between two needs. Including the
   * separate goals of each need as well.
   *
   * @return
   */
  public Collection<GoalInstantiationResult> createAllGoalInstantiationResults() {
    NeedModelWrapper needWrapper1 = new NeedModelWrapper(need1);
    NeedModelWrapper needWrapper2 = new NeedModelWrapper(need2);

    Collection<GoalInstantiationResult> results = new LinkedList<>();

    boolean addedGoal2s = false;

    for (Resource goal1 : needWrapper1.getGoals()) {
      results.add(findInstantiationForGoal(goal1));
      for (Resource goal2 : needWrapper2.getGoals()) {
        GoalInstantiationResult instantiationResult = findInstantiationForGoals(goal1, goal2);
        results.add(instantiationResult);

        if (!addedGoal2s) {
          results.add(findInstantiationForGoal(goal2));
        }
      }
      addedGoal2s = true;
    }

    if (!addedGoal2s) {
      for (Resource goal2 : needWrapper2.getGoals()) {
        results.add(findInstantiationForGoal(goal2));
      }
    }

    return results;
  }

  public Collection<GoalInstantiationResult> createGoalInstantiationResultsForNeed1() {
    return createGoalInstantiationResults(need1);
  }

  public Collection<GoalInstantiationResult> createGoalInstantiationResultsForNeed2() {
    return createGoalInstantiationResults(need2);
  }

  private Collection<GoalInstantiationResult> createGoalInstantiationResults(Dataset need) {
    NeedModelWrapper needWrapper = new NeedModelWrapper(need);

    Collection<GoalInstantiationResult> results = new LinkedList<>();

    for (Resource goal : needWrapper.getGoals()) {
      results.add(findInstantiationForGoal(goal));
    }

    return results;
  }
}
