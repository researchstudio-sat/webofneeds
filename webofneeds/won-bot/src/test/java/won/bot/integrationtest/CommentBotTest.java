/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.integrationtest;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import won.bot.PropertyPathConfigurator;
import won.bot.framework.bot.context.CommentBotContextWrapper;
import won.bot.framework.eventbot.event.impl.lifecycle.WorkDoneEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.CommentBot;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * Integration test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring/app/simpleCommentTest.xml" })
public class CommentBotTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // private static final int RUN_ONCE = 1;
    private static final long ACT_LOOP_TIMEOUT_MILLIS = 100;
    private static final long ACT_LOOP_INITIAL_DELAY_MILLIS = 100;
    private static MyBot bot;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SpringAwareBotManagerImpl botManager;
    private static boolean run = false;

    /**
     * This is run before each @TestD method.
     */
    @Before
    public void before() {
        if (!run) {
            // create a bot instance and auto-wire it
            AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
            bot = (MyBot) beanFactory.autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
            Object botBean = beanFactory.initializeBean(bot, "mybot");
            bot = (MyBot) botBean;
            // the bot also atoms a trigger so its act() method is called regularly.
            // (there is no trigger bean in the context)
            PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
            bot.setTrigger(trigger);
            logger.info("starting test case testCommentBot");
            // adding the bot to the bot manager will cause it to be initialized.
            // at that point, the trigger starts.
            botManager.addBot(bot);
            // the bot should now be running. We have to wait for it to finish before we
            // can check the results:
            // Together with the barrier.await() in the bot's listener, this trips the
            // barrier
            // and both threads continue.
            try {
                bot.getBarrier().await();
            } catch (InterruptedException e) {
                e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
            } catch (BrokenBarrierException e) {
                e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
            }
            run = true;
        }
    }

    /**
     * The main test method.
     *
     * @throws Exception
     */
    @Test
    public void testCommentBot() throws Exception {
        // now check the results!
        bot.executeAsserts();
        logger.info("finishing test case testCommentBot");
    }

    /**
     * We create a subclass of the bot we want to test here so that we can add a
     * listener to its internal event bus and to access its listeners, which record
     * information during the run that we later check with asserts.
     */
    public static class MyBot extends CommentBot {
        /**
         * Used for synchronization with the @TestD method: it should wait at the
         * barrier until our bot is done, then execute the asserts.
         */
        CyclicBarrier barrier = new CyclicBarrier(2);
        private static final String sparqlPrefix = "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>"
                + "PREFIX geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#>"
                + "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>"
                + "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                + "PREFIX won:   <https://w3id.org/won/core#>" + "PREFIX gr:    <http://purl.org/goodrelations/v1#>"
                + "PREFIX sioc:  <http://rdfs.org/sioc/ns#>" + "PREFIX ldp:   <http://www.w3.org/ns/ldp#>";

        /**
         * Default constructor is required for instantiation through Spring.
         */
        public MyBot() {
        }

        @Override
        protected void initializeEventListeners() {
            // of course, let the real bot implementation initialize itself
            super.initializeEventListeners();
            // now, add a listener to the WorkDoneEvent.
            // its only purpose is to trip the CyclicBarrier instance that
            // the test method is waiting on
            getEventBus().subscribe(WorkDoneEvent.class, new ActionOnEventListener(getEventListenerContext(),
                    new TripBarrierAction(getEventListenerContext(), barrier)));
        }

        public CyclicBarrier getBarrier() {
            return barrier;
        }

        /**
         * Here we check the results of the bot's execution.
         */
        public void executeAsserts() {
            // 1 act events
            Assert.assertEquals(0, this.atomCreator.getExceptionCount());
            // 1 create atom events
            Assert.assertEquals(1, this.commentSocketCreator.getEventCount());
            Assert.assertEquals(0, this.commentSocketCreator.getExceptionCount());
            // 1 create comment events
            Assert.assertEquals(2, this.atomConnector.getEventCount());
            Assert.assertEquals(0, this.atomConnector.getExceptionCount());
            // 1 connect, 1 open
            Assert.assertEquals(2, this.autoOpener.getEventCount());
            Assert.assertEquals(0, this.autoOpener.getExceptionCount());
            // 10 messages
            Assert.assertEquals(1, this.allAtomsDeactivator.getEventCount());
            Assert.assertEquals(0, this.allAtomsDeactivator.getExceptionCount());
            // 4 AtomDeactivated events
            Assert.assertEquals(2, this.workDoneSignaller.getEventCount());
            Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());
            // TODO: there is more to check:
            // * what does the RDF look like?
            // --> pull it from the atomURI/ConnectionURI and check contents
            // * what does the database look like? */
        }

        @Override
        protected void executeAssertionsForEstablishedConnection() {
            logger.info("starting test case testCommentRDF");
            bot.executeCommentRDFValidationAssert();
        }

        public void executeCommentRDFValidationAssert() {
            List<URI> atoms = ((CommentBotContextWrapper) getBotContextWrapper()).getCommentList();
            LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();
            List<URI> properties = new ArrayList<>();
            properties.add(URI.create(WON.connections.getURI()));
            // properties.add(RDF.type);
            properties.add(URI.create(WON.targetConnection.toString()));
            properties.add(URI.create(WON.targetAtom.toString()));
            properties.add(URI.create(RDFS.member.toString()));
            ((CachingLinkedDataSource) linkedDataSource).clear();
            Dataset dataModel = linkedDataSource.getDataForResourceWithPropertyPath(atoms.get(0),
                    PropertyPathConfigurator.configurePropertyPaths(), 30, 8, true);
            logger.debug("crawled dataset with property path: {}", RdfUtils.toString(dataModel));
            String queryString = sparqlPrefix + "SELECT ?atom ?connection ?atom2 WHERE {" +
            // "GRAPH ?g1 {" +
                    "   ?atom won:connections ?connections ." +
                    // "} ." +
                    // "GRAPH ?g2 {" +
                    "   ?atom sioc:hasReply ?atom2 ." +
                    // "} ." +
                    // "GRAPH ?g3 {" +
                    "?connections rdfs:member ?connection ." +
                    // "} ."+
                    // "Graph ?g4 {" +
                    "?connection won:socket won:CommentSocket." + "?connection won:targetConnection ?connection2."
                    + "?connection2 won:sourceAtom ?atom2 ." +
                    // "} ."+
                    "}";
            Query query = QueryFactory.create(queryString);
            QueryExecution qExec = QueryExecutionFactory.create(query, dataModel);
            ResultSet results = qExec.execSelect();
            List<String> actualList = new ArrayList<>();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
                // RDFNode node = soln.get("?connection");
            }
            Assert.assertEquals("wrong number of results", 1, actualList.size());
            qExec.close();
        }

        public Dataset executeAtomRDFValidationAsserts() {
            List<URI> atoms = getBotContextWrapper().getAtomCreateList();
            Dataset atomModel = getEventListenerContext().getLinkedDataSource().getDataForResource(atoms.get(0));
            System.out.println("executing queries...");
            String queryString = sparqlPrefix + "SELECT ?atom WHERE {" + "?atom a won:Atom" + "}";
            Query query = QueryFactory.create(queryString);
            QueryExecution qExec = QueryExecutionFactory.create(query, atomModel);
            // ResultSet results = executeQuery(queryString,atomModel);
            List<String> actualList = new ArrayList<>();
            ResultSet results = qExec.execSelect();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
            }
            Assert.assertTrue("wrong number of results", actualList.size() >= 1);
            // String expected1 = "( ?event = <" + EXAMPLE_ONTOLOGY_URI + "Open_01_1> ) (
            // ?eventType = <" + WON_ONTOLOGY_URI + "Open> )";
            // assertThat(actualList, hasItems(expected1));
            return atomModel;
        }

        public Dataset executeCommentRDFValidationAsserts() {
            List<URI> atoms = ((CommentBotContextWrapper) getBotContextWrapper()).getCommentList();
            Dataset commentModel = getEventListenerContext().getLinkedDataSource().getDataForResource(atoms.get(0));
            System.out.println("executing queries...");
            String queryString = sparqlPrefix + "SELECT ?atom WHERE {" + "?atom a won:Atom."
                    + "?atom won:socket won:CommentSocket" + "}";
            Query query = QueryFactory.create(queryString);
            QueryExecution qExec = QueryExecutionFactory.create(query, commentModel);
            List<String> actualList = new ArrayList<>();
            try {
                ResultSet results = qExec.execSelect();
                for (; results.hasNext();) {
                    QuerySolution soln = results.nextSolution();
                    actualList.add(soln.toString());
                }
            } finally {
                qExec.close();
            }
            Assert.assertTrue("wrong number of results", actualList.size() >= 1);
            return commentModel;
        }

        public void executeAtomCommentConnectionRDFValidationAsserts(Model atomModel, Model commentModel) {
            System.out.println("executing queries...");
            String queryString = sparqlPrefix + "SELECT ?atom ?connection ?state WHERE {"
                    + "?atom won:connections ?connections." + "?connections rdfs:member ?connection."
                    + "?connection won:connectionState ?state." + "}";
            logger.debug(RdfUtils.toString(atomModel));
            Query query = QueryFactory.create(queryString);
            QueryExecution qExec = QueryExecutionFactory.create(query, commentModel);
            ResultSet results = qExec.execSelect();
            URI atomConnectionCollectionURI = null;
            URI atomConnectionURI = null;
            URI commentConnectionsURI = null;
            URI atomConnectionURICheck = null;
            List<String> actualList = new ArrayList<>();
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                actualList.add(soln.toString());
                RDFNode node = soln.get("?connections");
                String nodeStr = node.toString();
                atomConnectionCollectionURI = URI.create(nodeStr);
            }
            qExec.close();
            Assert.assertTrue("wrong number of results", actualList.size() >= 1);
            Dataset atomConnections = getEventListenerContext().getLinkedDataSource()
                    .getDataForResource(atomConnectionCollectionURI);
            String queryString2 = sparqlPrefix + "SELECT ?connection WHERE {" + "?connections rdfs:member ?connection"
                    + "}";
            logger.debug(RdfUtils.toString(atomConnections));
            Query query2 = QueryFactory.create(queryString2);
            QueryExecution qExec2 = QueryExecutionFactory.create(query2, atomConnections);
            ResultSet results2 = qExec2.execSelect();
            // ResultSet results2 = executeQuery(queryString2, atomConnections);
            List<String> actualList2 = new ArrayList<>();
            for (; results2.hasNext();) {
                QuerySolution soln = results2.nextSolution();
                actualList2.add(soln.toString());
                RDFNode node = soln.get("?connection");
                String nodeStr = node.toString();
                atomConnectionURI = URI.create(nodeStr);
            }
            qExec2.close();
            Assert.assertTrue("wrong number of results", actualList2.size() >= 1);
            Dataset atomConnection = getEventListenerContext().getLinkedDataSource()
                    .getDataForResource(atomConnectionURI);
            String queryString3 = sparqlPrefix + "SELECT ?targetConnection WHERE {"
                    + "?connection won:targetConnection ?targetConnection" + "}";
            logger.debug(RdfUtils.toString(atomConnection));
            Query query3 = QueryFactory.create(queryString3);
            QueryExecution qExec3 = QueryExecutionFactory.create(query3, atomConnection);
            ResultSet results3 = qExec3.execSelect();
            List<String> actualList3 = new ArrayList<>();
            for (; results3.hasNext();) {
                QuerySolution soln = results3.nextSolution();
                actualList3.add(soln.toString());
                RDFNode node = soln.get("?targetConnection");
                String nodeStr = node.toString();
                commentConnectionsURI = URI.create(nodeStr);
            }
            Assert.assertTrue("wrong number of results", actualList3.size() >= 1);
            Dataset targetConnections = getEventListenerContext().getLinkedDataSource()
                    .getDataForResource(commentConnectionsURI);
            String queryString4 = sparqlPrefix + "SELECT ?targetConnection WHERE {"
                    + "?connection won:targetConnection ?targetConnection" + "}";
            logger.debug(RdfUtils.toString(targetConnections));
            // ResultSet results3 = executeQuery(queryString2, atomConnections);
            Query query4 = QueryFactory.create(queryString4);
            QueryExecution qExec4 = QueryExecutionFactory.create(query4, targetConnections);
            ResultSet results4 = qExec4.execSelect();
            List<String> actualList4 = new ArrayList<>();
            for (; results4.hasNext();) {
                QuerySolution soln = results4.nextSolution();
                actualList4.add(soln.toString());
                RDFNode node = soln.get("?targetConnection");
                String nodeStr = node.toString();
                atomConnectionURICheck = URI.create(nodeStr);
            }
            qExec4.close();
            Assert.assertTrue("wrong number of results", actualList4.size() >= 1);
            Assert.assertEquals(atomConnectionURI, atomConnectionURICheck);
        }

        public ResultSet executeQuery(String queryString, Model model) {
            Query query = QueryFactory.create(queryString);
            QueryExecution qExec = QueryExecutionFactory.create(query, model);
            ResultSet results;
            try {
                results = qExec.execSelect();
            } finally {
                qExec.close();
            }
            return results;
        }
    }
}
