package won.protocol.util;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.Assert;
import org.junit.Test;

import won.protocol.message.Utils;
import won.protocol.model.AtomGraphType;
import won.protocol.model.AtomState;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WONMATCH;

/**
 * Created by hfriedrich on 16.03.2017.
 */
public class AtomModelWrapperTest {
    private final String ATOM_URI = "https://node.matchat.org/won/resource/atom/3030440624813201400";

    @Test
    public void loadModels() throws IOException {
        // load dataset and if the atom and sysinfo models are there
        Dataset ds = Utils.createTestDataset("/atommodel/atom1.trig");
        Assert.assertTrue(AtomModelWrapper.isAAtom(ds));
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds, false);
        Assert.assertEquals(ATOM_URI, atomModelWrapper.getAtomNode(AtomGraphType.ATOM).getURI());
        Assert.assertEquals(ATOM_URI, atomModelWrapper.getAtomNode(AtomGraphType.SYSINFO).getURI());
        Dataset withoutSysinfo = atomModelWrapper.copyDatasetWithoutSysinfo();
        Assert.assertEquals(4, StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(withoutSysinfo.listNames(), Spliterator.ORDERED), false)
                        .count());
        // load the atom and sysinfo models individually
        Model atomModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model sysInfoModel = atomModelWrapper.copyAtomModel(AtomGraphType.SYSINFO);
        AtomModelWrapper atomModelWrapperNew = new AtomModelWrapper(atomModel, sysInfoModel);
        Assert.assertTrue(atomModelWrapperNew.copyAtomModel(AtomGraphType.ATOM)
                        .isIsomorphicWith(atomModelWrapper.copyAtomModel(AtomGraphType.ATOM)));
        Assert.assertTrue(atomModelWrapperNew.copyAtomModel(AtomGraphType.SYSINFO)
                        .isIsomorphicWith(atomModelWrapper.copyAtomModel(AtomGraphType.SYSINFO)));
        withoutSysinfo = atomModelWrapper.copyDatasetWithoutSysinfo();
        Assert.assertEquals(4, StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(withoutSysinfo.listNames(), Spliterator.ORDERED), false)
                        .count());
        // load only the atom model, the other one is created
        atomModelWrapperNew = new AtomModelWrapper(atomModel, null);
        Assert.assertEquals(ATOM_URI, atomModelWrapperNew.getAtomNode(AtomGraphType.ATOM).getURI());
        Assert.assertEquals(ATOM_URI, atomModelWrapperNew.getAtomNode(AtomGraphType.SYSINFO).getURI());
        withoutSysinfo = atomModelWrapperNew.copyDatasetWithoutSysinfo();
        Assert.assertEquals(1, StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(withoutSysinfo.listNames(), Spliterator.ORDERED), false)
                        .count());
        // load only the sysinfo model, the other one is created
        atomModelWrapperNew = new AtomModelWrapper(null, sysInfoModel);
        Assert.assertEquals(ATOM_URI, atomModelWrapperNew.getAtomNode(AtomGraphType.ATOM).getURI());
        Assert.assertEquals(ATOM_URI, atomModelWrapperNew.getAtomNode(AtomGraphType.SYSINFO).getURI());
        withoutSysinfo = atomModelWrapperNew.copyDatasetWithoutSysinfo();
        Assert.assertEquals(1, StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(withoutSysinfo.listNames(), Spliterator.ORDERED), false)
                        .count());
        // query sysinfo model values
        Assert.assertEquals(AtomState.ACTIVE, atomModelWrapper.getAtomState());
        ZonedDateTime date = ZonedDateTime.parse("2017-02-07T08:46:32.917Z", DateTimeFormatter.ISO_DATE_TIME);
        Assert.assertEquals(date, atomModelWrapper.getCreationDate());
        Assert.assertEquals("https://node.matchat.org/won/resource", atomModelWrapper.getWonNodeUri());
        Assert.assertEquals("https://node.matchat.org/won/resource/atom/3030440624813201400/connections",
                        atomModelWrapper.getConnectionContainerUri());
        // query the atom model values
        Assert.assertTrue(atomModelWrapper.flag(WONMATCH.UsedForTesting));
        Assert.assertEquals(1, atomModelWrapper.getSocketUris().size());
        Assert.assertEquals("https://w3id.org/won/core#ChatSocket", atomModelWrapper.getSocketUris().iterator().next());
        Assert.assertTrue(atomModelWrapper.flag(WONMATCH.NoHintForMe));
        // query the content nodes
        Assert.assertEquals(2, atomModelWrapper.getAllContentNodes().size());
        Assert.assertNotNull(atomModelWrapper.getAtomContentNode());
        Assert.assertEquals(1, atomModelWrapper.getSeeksNodes().size());
        Assert.assertEquals("Offering tennis lessons", atomModelWrapper.getContentPropertyStringValue(DC.title));
        Assert.assertTrue(atomModelWrapper.getSeeksPropertyStringValues(DC.title).contains("tennis students"));
        Assert.assertEquals(2, atomModelWrapper.getAllContentPropertyStringValues(DC.title, null).size());
        Assert.assertEquals(3, atomModelWrapper.getContentPropertyStringValues(WONCON.tag, null).size());
        Assert.assertEquals(2, atomModelWrapper.getSeeksPropertyStringValues(WONCON.tag, null).size());
        Assert.assertEquals(5, atomModelWrapper.getAllContentPropertyStringValues(WONCON.tag, null).size());
        Assert.assertEquals("16.358398",
                        atomModelWrapper.getContentPropertyStringValue("s:location/s:geo/s:longitude"));
        // query the goals
        Assert.assertEquals(2, atomModelWrapper.getGoals().size());
        Assert.assertNotNull(atomModelWrapper.getGoal("https://w3id.org/won/core#NamedGoal"));
        Assert.assertTrue(atomModelWrapper
                        .getShapesGraph(atomModelWrapper.getGoal("https://w3id.org/won/core#NamedGoal")).isEmpty());
        Assert.assertTrue(atomModelWrapper.getDataGraph(atomModelWrapper.getGoal("https://w3id.org/won/core#NamedGoal"))
                        .isEmpty());
        Collection<Resource> goals = atomModelWrapper.getGoals();
        Resource blank = null;
        for (Resource goal : goals) {
            if (!goal.isURIResource()) {
                blank = goal;
            }
        }
        Assert.assertFalse(atomModelWrapper.getShapesGraph(blank).isEmpty());
        Assert.assertFalse(atomModelWrapper.getDataGraph(blank).isEmpty());
        // make sure we don't find a matching context:
        Assert.assertTrue("did not expect to find matching contexts", atomModelWrapper.getMatchingContexts().isEmpty());
    }

    @Test
    public void loadIsAndSeeksModel() throws IOException {
        Dataset ds = Utils.createTestDataset("/atommodel/atom2.trig");
        Assert.assertFalse(AtomModelWrapper.isAAtom(ds));
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Assert.assertNotNull(atomModelWrapper.getAtomContentNode());
        Assert.assertEquals(2, atomModelWrapper.getSeeksNodes().size());
        Assert.assertEquals("title1", atomModelWrapper.getContentPropertyStringValue(DC.title));
        Assert.assertEquals(2, atomModelWrapper.getSeeksPropertyStringValues(DC.title, null).size());
        Assert.assertEquals(3, atomModelWrapper.getAllContentPropertyStringValues(DC.title, null).size());
        // make sure we don't find a matching context:
        Assert.assertTrue("did not expect to find matching contexts", atomModelWrapper.getMatchingContexts().isEmpty());
    }

    @Test
    public void createAtomWithShapesModel() throws IOException {
        Dataset ds = Utils.createTestDataset("/atommodel/atomwithshapes.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds, false);
        Assert.assertNotNull(atomModelWrapper);
    }

    @Test
    public void createAtomModel() {
        // create a empty wrapper with an atom uri, check that the atom and sysinfo
        // models are there
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ATOM_URI);
        Assert.assertNotNull(atomModelWrapper.copyAtomModel(AtomGraphType.ATOM));
        Assert.assertEquals(ATOM_URI, atomModelWrapper.getAtomUri());
        atomModelWrapper.setContentPropertyStringValue(SCHEMA.DESCRIPTION, "description");
        atomModelWrapper.addPropertyStringValue(WONCON.tag, "tag");
        Assert.assertEquals(1, atomModelWrapper.getContentPropertyStringValues(SCHEMA.DESCRIPTION, null).size());
        Assert.assertEquals(1, atomModelWrapper.getContentPropertyStringValues(WONCON.tag, null).size());
        // add different content nodes now and check that they are there
        atomModelWrapper.createSeeksNode("https://seeks_uri1");
        atomModelWrapper.createSeeksNode("https://seeks_uri2");
        Assert.assertNotNull(atomModelWrapper.getAtomContentNode());
        Assert.assertEquals(2, atomModelWrapper.getSeeksNodes().size());
        Assert.assertEquals(3, atomModelWrapper.getAllContentNodes().size());
        Assert.assertNotNull(atomModelWrapper.getAtomContentNode());
        Assert.assertEquals(2, atomModelWrapper.getSeeksNodes().size());
        // add content now and check if it can be queried correctly
        atomModelWrapper.setContentPropertyStringValue(SCHEMA.DESCRIPTION, "description");
        atomModelWrapper.setSeeksPropertyStringValue(SCHEMA.DESCRIPTION, "description1");
        atomModelWrapper.addSeeksPropertyStringValue(SCHEMA.DESCRIPTION, "description2");
        atomModelWrapper.addPropertyStringValue(WONCON.tag, "tag1");
        atomModelWrapper.addSeeksPropertyStringValue(WONCON.tag, "tag2");
        Assert.assertEquals(4, atomModelWrapper.getSeeksPropertyStringValues(SCHEMA.DESCRIPTION, null).size());
        Assert.assertEquals(5, atomModelWrapper.getAllContentPropertyStringValues(SCHEMA.DESCRIPTION, null).size());
        Assert.assertEquals(4, atomModelWrapper.getAllContentPropertyStringValues(WONCON.tag, null).size());
        Assert.assertEquals(2, atomModelWrapper.getContentPropertyStringValues(WONCON.tag, null).size());
        Assert.assertEquals(2, atomModelWrapper.getSeeksPropertyStringValues(WONCON.tag, null).size());
    }

    @Test
    public void normalizeModel_Tree() throws IOException {
        // compare model that is not changed by normalization
        Dataset ds = Utils.createTestDataset("/atommodel/atom1.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds, false);
        Model originalModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model normalizedModel = atomModelWrapper.normalizeAtomModel();
        Assert.assertTrue(originalModel.isIsomorphicWith(normalizedModel));
    }

    @Test
    public void normalizeAtomModel_Cycle1() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/atommodel/atom2.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Model originalModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model normalizedModel = atomModelWrapper.normalizeAtomModel();
        AtomModelWrapper normalizedWrapper = new AtomModelWrapper(normalizedModel, null);
        String isSeeksTitle = atomModelWrapper.getContentPropertyStringValue(DC.title);
        Assert.assertEquals("title1", isSeeksTitle);
        Assert.assertEquals(isSeeksTitle, normalizedWrapper.getContentPropertyStringValue(DC.title));
        Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains(isSeeksTitle));
    }

    @Test
    public void normalizeAtomModel_Cycle2() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/atommodel/atom3.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Model originalModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model normalizedModel = atomModelWrapper.normalizeAtomModel();
        AtomModelWrapper normalizedWrapper = new AtomModelWrapper(normalizedModel, null);
        String isSeeksTitle = atomModelWrapper.getContentPropertyStringValue(DC.title);
        Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains(isSeeksTitle));
    }

    @Test
    public void normalizeAtomModel_Cycle3() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/atommodel/atom4.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Model originalModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model normalizedModel = atomModelWrapper.normalizeAtomModel();
        String isSeeksTitle = atomModelWrapper.getContentPropertyStringValue(DC.title);
        AtomModelWrapper normalizedWrapper = new AtomModelWrapper(normalizedModel, null);
        Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains(isSeeksTitle));
    }

    @Test
    public void normalizeAtomModel_Cycle4() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/atommodel/atom5.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Model originalModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model normalizedModel = atomModelWrapper.normalizeAtomModel();
        AtomModelWrapper normalizedWrapper = new AtomModelWrapper(normalizedModel, null);
        Assert.assertTrue(normalizedWrapper.getSeeksPropertyStringValues(DC.title, null).contains("title3"));
        Assert.assertTrue(normalizedWrapper.getAllContentPropertyStringValues(DC.title, null).contains("title3"));
    }

    @Test
    public void normalizeAtomModel_Cycle5() throws IOException {
        // check case where "is" and "seeks" point to the same blank node
        Dataset ds = Utils.createTestDataset("/atommodel/atom6.trig");
        Assert.assertFalse(AtomModelWrapper.isAAtom(ds));
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Model originalModel = atomModelWrapper.copyAtomModel(AtomGraphType.ATOM);
        Model normalizedModel = atomModelWrapper.normalizeAtomModel();
        AtomModelWrapper normalizedWrapper = new AtomModelWrapper(normalizedModel, null);
    }

    @Test
    public void testMultipleMatchingContexts() throws IOException {
        Dataset ds = Utils.createTestDataset("/atommodel/atom7.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Assert.assertEquals(3, atomModelWrapper.getMatchingContexts().size());
        Assert.assertTrue("expected matching context 'TU_Wien'",
                        atomModelWrapper.getMatchingContexts().contains("TU_Wien"));
        Assert.assertTrue("expected matching context 'Vienna'",
                        atomModelWrapper.getMatchingContexts().contains("Vienna"));
        Assert.assertTrue("expected matching context 'Ball'", atomModelWrapper.getMatchingContexts().contains("Ball"));
    }

    @Test
    public void testSingleMatchingContext() throws IOException {
        Dataset ds = Utils.createTestDataset("/atommodel/atom8.trig");
        AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds);
        Assert.assertEquals(1, atomModelWrapper.getMatchingContexts().size());
        Assert.assertTrue("expected matching context 'TU_Wien'",
                        atomModelWrapper.getMatchingContexts().contains("TU_Wien"));
    }
}
