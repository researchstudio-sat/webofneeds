package won.protocol.util;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.Assert;
import org.junit.Test;

import won.protocol.message.Utils;
import won.protocol.model.NeedGraphType;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 16.03.2017.
 */
public class NeedModelWrapperTest {
  private final String NEED_URI = "https://node.matchat.org/won/resource/need/3030440624813201400";

  @Test
  public void loadModels() throws IOException {

    // load dataset and if the need and sysinfo models are there
    Dataset ds = Utils.createTestDataset("/needmodel/need1.trig");
    Assert.assertTrue(NeedModelWrapper.isANeed(ds));
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds, false);
    Assert.assertEquals(NEED_URI, needModelWrapper.getNeedNode(NeedGraphType.NEED).getURI());
    Assert.assertEquals(NEED_URI, needModelWrapper.getNeedNode(NeedGraphType.SYSINFO).getURI());

    // load the need and sysinfo models individually
    Model needModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model sysInfoModel = needModelWrapper.copyNeedModel(NeedGraphType.SYSINFO);
    NeedModelWrapper needModelWrapperNew = new NeedModelWrapper(needModel, sysInfoModel);
    Assert.assertTrue(needModelWrapperNew.copyNeedModel(NeedGraphType.NEED)
        .isIsomorphicWith(needModelWrapper.copyNeedModel(NeedGraphType.NEED)));
    Assert.assertTrue(needModelWrapperNew.copyNeedModel(NeedGraphType.SYSINFO)
        .isIsomorphicWith(needModelWrapper.copyNeedModel(NeedGraphType.SYSINFO)));

    // load only the need model, the other one is created
    needModelWrapperNew = new NeedModelWrapper(needModel, null);
    Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.NEED).getURI());
    Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.SYSINFO).getURI());

    // load only the sysinfo model, the other one is created
    needModelWrapperNew = new NeedModelWrapper(null, sysInfoModel);
    Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.NEED).getURI());
    Assert.assertEquals(NEED_URI, needModelWrapperNew.getNeedNode(NeedGraphType.SYSINFO).getURI());

    // query sysinfo model values
    Assert.assertEquals(NeedState.ACTIVE, needModelWrapper.getNeedState());
    ZonedDateTime date = ZonedDateTime.parse("2017-02-07T08:46:32.917Z", DateTimeFormatter.ISO_DATE_TIME);
    Assert.assertEquals(date, needModelWrapper.getCreationDate());
    Assert.assertEquals("https://node.matchat.org/won/resource", needModelWrapper.getWonNodeUri());
    Assert.assertEquals("https://node.matchat.org/won/resource/need/3030440624813201400/connections",
        needModelWrapper.getConnectionContainerUri());

    // query the need model values
    Assert.assertTrue(needModelWrapper.hasFlag(WON.USED_FOR_TESTING));
    Assert.assertEquals(1, needModelWrapper.getFacetUris().size());
    Assert.assertEquals("http://purl.org/webofneeds/model#ChatFacet",
        needModelWrapper.getFacetUris().iterator().next());
    Assert.assertTrue(needModelWrapper.hasFlag(WON.NO_HINT_FOR_ME));

    // query the content nodes
    Assert.assertEquals(2, needModelWrapper.getAllContentNodes().size());
    Assert.assertNotNull(needModelWrapper.getNeedContentNode());
    Assert.assertEquals(1, needModelWrapper.getSeeksNodes().size());
    Assert.assertEquals("Offering tennis lessons", needModelWrapper.getContentPropertyStringValue(DC.title));
    Assert.assertTrue(needModelWrapper.getSeeksPropertyStringValues(DC.title).contains("tennis students"));
    Assert.assertEquals(2, needModelWrapper.getAllContentPropertyStringValues(DC.title, null).size());
    Assert.assertEquals(3, needModelWrapper.getContentPropertyStringValues(WON.HAS_TAG, null).size());
    Assert.assertEquals(2, needModelWrapper.getSeeksPropertyStringValues(WON.HAS_TAG, null).size());
    Assert.assertEquals(5, needModelWrapper.getAllContentPropertyStringValues(WON.HAS_TAG, null).size());
    Assert.assertEquals("16.358398", needModelWrapper.getContentPropertyStringValue("s:location/s:geo/s:longitude"));

    // query the goals
    Assert.assertEquals(2, needModelWrapper.getGoals().size());
    Assert.assertNotNull(needModelWrapper.getGoal("http://purl.org/webofneeds/model#NamedGoal"));
    Assert.assertTrue(needModelWrapper
        .getShapesGraph(needModelWrapper.getGoal("http://purl.org/webofneeds/model#NamedGoal")).isEmpty());
    Assert.assertTrue(needModelWrapper
        .getDataGraph(needModelWrapper.getGoal("http://purl.org/webofneeds/model#NamedGoal")).isEmpty());
    Collection<Resource> goals = needModelWrapper.getGoals();
    Resource blank = null;
    for (Resource goal : goals) {
      if (!goal.isURIResource()) {
        blank = goal;
      }
    }
    Assert.assertFalse(needModelWrapper.getShapesGraph(blank).isEmpty());
    Assert.assertFalse(needModelWrapper.getDataGraph(blank).isEmpty());

    // make sure we don't find a matching context:
    Assert.assertTrue("did not expect to find matching contexts", needModelWrapper.getMatchingContexts().isEmpty());
  }

  @Test
  public void loadIsAndSeeksModel() throws IOException {

    Dataset ds = Utils.createTestDataset("/needmodel/need2.trig");
    Assert.assertFalse(NeedModelWrapper.isANeed(ds));
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);

    Assert.assertNotNull(needModelWrapper.getNeedContentNode());
    Assert.assertEquals(2, needModelWrapper.getSeeksNodes().size());
    Assert.assertEquals("title1", needModelWrapper.getContentPropertyStringValue(DC.title));
    Assert.assertEquals(2, needModelWrapper.getSeeksPropertyStringValues(DC.title, null).size());
    Assert.assertEquals(3, needModelWrapper.getAllContentPropertyStringValues(DC.title, null).size());

    // make sure we don't find a matching context:
    Assert.assertTrue("did not expect to find matching contexts", needModelWrapper.getMatchingContexts().isEmpty());
  }

  @Test
  public void createNeedWithShapesModel() throws IOException {
    Dataset ds = Utils.createTestDataset("/needmodel/needwithshapes.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds, false);
    Assert.assertNotNull(needModelWrapper);
  }

  @Test
  public void createNeedModel() {

    // create a empty wrapper with a need uri, check that the need and sysinfo
    // models are there
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(NEED_URI);
    Assert.assertNotNull(needModelWrapper.copyNeedModel(NeedGraphType.NEED));
    Assert.assertEquals(NEED_URI, needModelWrapper.getNeedUri());

    needModelWrapper.setContentPropertyStringValue(DC.description, "description");
    needModelWrapper.addPropertyStringValue(WON.HAS_TAG, "tag");
    Assert.assertEquals(1, needModelWrapper.getContentPropertyStringValues(DC.description, null).size());
    Assert.assertEquals(1, needModelWrapper.getContentPropertyStringValues(WON.HAS_TAG, null).size());

    // add different content nodes now and check that they are there
    needModelWrapper.createSeeksNode("https://seeks_uri1");
    needModelWrapper.createSeeksNode("https://seeks_uri2");
    Assert.assertNotNull(needModelWrapper.getNeedContentNode());
    Assert.assertEquals(2, needModelWrapper.getSeeksNodes().size());
    Assert.assertEquals(3, needModelWrapper.getAllContentNodes().size());
    Assert.assertNotNull(needModelWrapper.getNeedContentNode());
    Assert.assertEquals(2, needModelWrapper.getSeeksNodes().size());

    // add content now and check if it can be queried correctly
    needModelWrapper.setContentPropertyStringValue(DC.description, "description");
    needModelWrapper.setSeeksPropertyStringValue(DC.description, "description1");
    needModelWrapper.addSeeksPropertyStringValue(DC.description, "description2");
    needModelWrapper.addPropertyStringValue(WON.HAS_TAG, "tag1");
    needModelWrapper.addSeeksPropertyStringValue(WON.HAS_TAG, "tag2");
    Assert.assertEquals(4, needModelWrapper.getSeeksPropertyStringValues(DC.description, null).size());
    Assert.assertEquals(5, needModelWrapper.getAllContentPropertyStringValues(DC.description, null).size());
    Assert.assertEquals(4, needModelWrapper.getAllContentPropertyStringValues(WON.HAS_TAG, null).size());
    Assert.assertEquals(2, needModelWrapper.getContentPropertyStringValues(WON.HAS_TAG, null).size());
    Assert.assertEquals(2, needModelWrapper.getSeeksPropertyStringValues(WON.HAS_TAG, null).size());
  }

  @Test
  public void normalizeModel_Tree() throws IOException {

    // compare model that is not changed by normalization
    Dataset ds = Utils.createTestDataset("/needmodel/need1.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds, false);
    Model originalModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model normalizedModel = needModelWrapper.normalizeNeedModel();
    Assert.assertTrue(originalModel.isIsomorphicWith(normalizedModel));

  }

  @Test
  public void normalizeNeedModel_Cycle1() throws IOException {
    // check case where "is" and "seeks" point to the same blank node
    Dataset ds = Utils.createTestDataset("/needmodel/need2.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Model originalModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model normalizedModel = needModelWrapper.normalizeNeedModel();
    NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
    String isSeeksTitle = needModelWrapper.getContentPropertyStringValue(DC.title);
    Assert.assertEquals("title1", isSeeksTitle);
    Assert.assertEquals(isSeeksTitle, normalizedWrapper.getContentPropertyStringValue(DC.title));
    Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains(isSeeksTitle));
  }

  @Test
  public void normalizeNeedModel_Cycle2() throws IOException {
    // check case where "is" and "seeks" point to the same blank node
    Dataset ds = Utils.createTestDataset("/needmodel/need3.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Model originalModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model normalizedModel = needModelWrapper.normalizeNeedModel();
    NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
    String isSeeksTitle = needModelWrapper.getContentPropertyStringValue(DC.title);
    Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains(isSeeksTitle));
  }

  @Test
  public void normalizeNeedModel_Cycle3() throws IOException {
    // check case where "is" and "seeks" point to the same blank node
    Dataset ds = Utils.createTestDataset("/needmodel/need4.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Model originalModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model normalizedModel = needModelWrapper.normalizeNeedModel();
    String isSeeksTitle = needModelWrapper.getContentPropertyStringValue(DC.title);
    NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
    Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains(isSeeksTitle));
  }

  @Test
  public void normalizeNeedModel_Cycle4() throws IOException {
    // check case where "is" and "seeks" point to the same blank node
    Dataset ds = Utils.createTestDataset("/needmodel/need5.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Model originalModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model normalizedModel = needModelWrapper.normalizeNeedModel();
    NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
    Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains("title3"));
    Assert.assertTrue(normalizedWrapper.getAllContentPropertyStringValues(DC.title, null).contains("title3"));
  }

  @Test
  public void normalizeNeedModel_Cycle5() throws IOException {
    // check case where "is" and "seeks" point to the same blank node
    Dataset ds = Utils.createTestDataset("/needmodel/need6.trig");
    Assert.assertFalse(NeedModelWrapper.isANeed(ds));
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Model originalModel = needModelWrapper.copyNeedModel(NeedGraphType.NEED);
    Model normalizedModel = needModelWrapper.normalizeNeedModel();
    NeedModelWrapper normalizedWrapper = new NeedModelWrapper(normalizedModel, null);
  }

  @Test
  public void testMultipleMatchingContexts() throws IOException {
    Dataset ds = Utils.createTestDataset("/needmodel/need7.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Assert.assertEquals(3, needModelWrapper.getMatchingContexts().size());
    Assert.assertTrue("expected matching context 'TU_Wien'",
        needModelWrapper.getMatchingContexts().contains("TU_Wien"));
    Assert.assertTrue("expected matching context 'Vienna'", needModelWrapper.getMatchingContexts().contains("Vienna"));
    Assert.assertTrue("expected matching context 'Ball'", needModelWrapper.getMatchingContexts().contains("Ball"));
  }

  @Test
  public void testSingleMatchingContext() throws IOException {
    Dataset ds = Utils.createTestDataset("/needmodel/need8.trig");
    NeedModelWrapper needModelWrapper = new NeedModelWrapper(ds);
    Assert.assertEquals(1, needModelWrapper.getMatchingContexts().size());
    Assert.assertTrue("expected matching context 'TU_Wien'",
        needModelWrapper.getMatchingContexts().contains("TU_Wien"));
  }
}
