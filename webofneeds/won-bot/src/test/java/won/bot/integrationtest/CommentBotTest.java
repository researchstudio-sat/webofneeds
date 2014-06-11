/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.integrationtest;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;
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
import won.bot.framework.events.event.impl.WorkDoneEvent;
import won.bot.framework.events.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.CommentBot;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Integration test.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/app/botRunner.xml"})
public class CommentBotTest
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private static final int RUN_ONCE = 1;
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
  public void before(){
    if (!run)
    {
      AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
      bot = (MyBot) beanFactory.autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
      Object botBean = beanFactory.initializeBean(bot, "mybot");
      bot = (MyBot) botBean;
      //the bot also needs a trigger so its act() method is called regularly.
      // (there is no trigger bean in the context)
      PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
      bot.setTrigger(trigger);
      logger.info("starting test case testCommentBot");
      //adding the bot to the bot manager will cause it to be initialized.
      //at that point, the trigger starts.
      botManager.addBot(bot);

      //the bot should now be running. We have to wait for it to finish before we
      //can check the results:
      //Together with the barrier.await() in the bot's listener, this trips the barrier
      //and both threads continue.
      try {
        bot.getBarrier().await();
      } catch (InterruptedException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (BrokenBarrierException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }
      run = true;
    }
  }

  /**
   * The main test method.
   * @throws Exception
   */
  @Test
  public void testCommentBot() throws Exception
  {

    //now check the results!
    bot.executeAsserts();

    logger.info("finishing test case testCommentBot");
  }

  @Test
  public void testCommentRDF(){
    logger.info("starting test case testCommentRDF");
    bot.executeCommentRDFValidationAssert();
    //Model commentModel = bot.executeCommentRDFValidationAsserts();
    //Model needModel = bot.executeNeedRDFValidationAsserts();
    //bot.executeNeedCommentConnectionRDFValidationAsserts(needModel,commentModel);
  }

  /**
   * We create a subclass of the bot we want to test here so that we can
   * add a listener to its internal event bus and to access its listeners, which
   * record information during the run that we later check with asserts.
   */
  public static class MyBot extends CommentBot
  {
    /**
     * Used for synchronization with the @TestD method: it should wait at the
     * barrier until our bot is done, then execute the asserts.
     */
    CyclicBarrier barrier = new CyclicBarrier(2);

    private static final String sparqlPrefix =
      "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>"+
        "PREFIX geo:   <http://www.w3.org/2003/01/geo/wgs84_pos#>"+
        "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>"+
        "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"+
        "PREFIX won:   <http://purl.org/webofneeds/model#>"+
        "PREFIX gr:    <http://purl.org/goodrelations/v1#>"+
        "PREFIX sioc:  <http://rdfs.org/sioc/ns#>"+
        "PREFIX ldp:   <http://www.w3.org/ns/ldp#>";

    /**
     * Default constructor is required for instantiation through Spring.
     */
    public MyBot(){
    }

    @Override
    protected void initializeEventListeners()
    {
      //of course, let the real bot implementation initialize itself
      super.initializeEventListeners();
      //now, add a listener to the WorkDoneEvent.
      //its only purpose is to trip the CyclicBarrier instance that
      // the test method is waiting on
      getEventBus().subscribe(WorkDoneEvent.class,
        new ActionOnEventListener(
          getEventListenerContext(),
          new TripBarrierAction(getEventListenerContext(), barrier)));
    }

    public CyclicBarrier getBarrier()
    {
      return barrier;
    }

    /**
     * Here we check the results of the bot's execution.
     */
    public void executeAsserts()
    {
      //1 act events
      Assert.assertEquals(1, this.needCreator.getEventCount());
      Assert.assertEquals(0, this.needCreator.getExceptionCount());
      //1 create need events
      Assert.assertEquals(1, this.commentFacetCreator.getEventCount());
      Assert.assertEquals(0, this.commentFacetCreator.getExceptionCount());
      //1 create comment events
      Assert.assertEquals(2, this.needConnector.getEventCount());
      Assert.assertEquals(0, this.needConnector.getExceptionCount());
      //1 connect, 1 open
      Assert.assertEquals(2, this.autoOpener.getEventCount());
      Assert.assertEquals(0, this.autoOpener.getExceptionCount());
      //10 messages
      Assert.assertEquals(2, this.connectionCloser.getEventCount());
      Assert.assertEquals(0, this.connectionCloser.getExceptionCount());
      //2 close (one sent, one received - but for sending we create no event)

      Assert.assertEquals(1,this.allNeedsDeactivator.getEventCount());
      Assert.assertEquals(0, this.allNeedsDeactivator.getExceptionCount());


      //4 NeedDeactivated events
      Assert.assertEquals(2, this.workDoneSignaller.getEventCount());
      Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());

      //TODO: there is more to check:
      //* what does the RDF look like?
      // --> pull it from the needURI/ConnectionURI and check contents
      //* what does the database look like?      */
    }
    public void  executeCommentRDFValidationAssert(){

      List<URI> needs = getEventListenerContext().getBotContext().getNamedNeedUriList(NAME_COMMENTS);

      LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();

      List<URI> properties = new ArrayList<>();
      List<URI> objects = new ArrayList<>();

      properties.add(URI.create(WON.HAS_CONNECTIONS.getURI()));
      //properties.add(RDF.type);
      properties.add(URI.create(WON.HAS_REMOTE_CONNECTION.toString()));
      properties.add(URI.create(WON.HAS_REMOTE_NEED.toString()));
      properties.add(URI.create(RDFS.member.toString()));

      List<URI> crawled = new ArrayList<>();

      Model dataModel = linkedDataSource.getModelForResource(needs.get(0),properties,objects,30,8);

      logger.debug("crawled dataset: {}",RdfUtils.toString(dataModel));

      String queryString = sparqlPrefix +
        "SELECT ?need ?connection ?need2 WHERE {" +
        "?need won:hasConnections ?connections."+
        "?connections rdfs:member ?connection."+
        "?connection won:hasFacet won:CommentFacet."+
        "?connection won:hasRemoteConnection ?connection2."+
        "?connection2 won:belongsToNeed ?need2."+
        "?need sioc:hasReply ?need2."+
        "}";

      Query query = QueryFactory.create(queryString);
      QueryExecution qExec = QueryExecutionFactory.create(query, dataModel);
      ResultSet results = qExec.execSelect();

      List<String> actualList = new ArrayList<>();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
        RDFNode node = soln.get("?connection");
      }
      assertTrue("wrong number of results", actualList.size() >= 1);
      qExec.close();

    }

    public Model executeNeedRDFValidationAsserts(){

      List<URI> needs = getEventListenerContext().getBotContext().getNamedNeedUriList(NAME_NEEDS);
      Model needModel = getEventListenerContext().getLinkedDataSource().getModelForResource(needs.get(0));
      System.out.println("executing queries...");
      String queryString = sparqlPrefix +
        "SELECT ?need WHERE {" +
        "?need a won:Need"+
        "}";
      Query query = QueryFactory.create(queryString);
      QueryExecution qExec = QueryExecutionFactory.create(query, needModel);
      //ResultSet results = executeQuery(queryString,needModel);
      List<String> actualList = new ArrayList<>();
      ResultSet results = qExec.execSelect();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
      }

      assertTrue("wrong number of results", actualList.size() >= 1);
      //String expected1 = "( ?event = <" + EXAMPLE_ONTOLOGY_URI + "Open_01_1> ) ( ?eventType = <" + WON_ONTOLOGY_URI + "Open> )";
      //assertThat(actualList, hasItems(expected1));
      return needModel;
    }
    public Model executeCommentRDFValidationAsserts(){
      List<URI> needs = getEventListenerContext().getBotContext().getNamedNeedUriList(NAME_COMMENTS);
      Model commentModel = getEventListenerContext().getLinkedDataSource().getModelForResource(needs.get(0));
      System.out.println("executing queries...");
      String queryString = sparqlPrefix +
        "SELECT ?need WHERE {" +
        "?need a won:Need."+
        "?need won:hasFacet won:CommentFacet" +
        "}";
      Query query = QueryFactory.create(queryString);
      QueryExecution qExec = QueryExecutionFactory.create(query, commentModel);
      List<String> actualList = new ArrayList<>();
      try {
        ResultSet results = qExec.execSelect();
        for (; results.hasNext(); ) {
          QuerySolution soln = results.nextSolution();
          actualList.add(soln.toString());
        }
      } finally {
        qExec.close();
      }
      assertTrue("wrong number of results", actualList.size() >= 1);
      return commentModel;
    }
    public void executeNeedCommentConnectionRDFValidationAsserts(Model needModel, Model commentModel){

      System.out.println("executing queries...");
      String queryString = sparqlPrefix +
        "SELECT ?need ?connection ?state WHERE {" +
        "?need won:hasConnections ?connections."+
        "?connections rdfs:member ?connection."+
        "?connection won:hasConnectionState ?state."+
        "}";
      logger.debug(RdfUtils.toString(needModel));
      Dataset dataset = null;
      //Query query = queryString.asQuery();
      //QueryExecution qExec = QueryExecutionFactory.create(query, dataset);
      Query query = QueryFactory.create(queryString);
      QueryExecution qExec = QueryExecutionFactory.create(query, commentModel);
     // Query query = QueryFactory.create(queryString);
    //  QueryExecution qExec = QueryExecutionFactory.create(query, dsARQ);
      ResultSet results = qExec.execSelect();

      // ResultSet results = executeQuery(queryString,needModel);
      URI needConnectionCollectionURI = null;
      URI needConnectionURI = null;
      URI commentConnectionsURI = null;
      URI needConnectionURICheck = null;
      List<String> actualList = new ArrayList<>();
      for (; results.hasNext(); ) {
        QuerySolution soln = results.nextSolution();
        actualList.add(soln.toString());
        RDFNode node =  soln.get("?connections");
        String nodeStr = node.toString();
        needConnectionCollectionURI = URI.create(nodeStr);
      }
      qExec.close();
      assertTrue("wrong number of results", actualList.size() >= 1);

      Model needConnections = getEventListenerContext().getLinkedDataSource().getModelForResource(needConnectionCollectionURI);
      String queryString2 = sparqlPrefix+
        "SELECT ?connection WHERE {" +
        "?connections rdfs:member ?connection"+
        "}";

      logger.debug(RdfUtils.toString(needConnections));
      Query query2 = QueryFactory.create(queryString2);
      QueryExecution qExec2 = QueryExecutionFactory.create(query2, needConnections);
      ResultSet results2 = qExec2.execSelect();

      //ResultSet results2 = executeQuery(queryString2, needConnections);
      List<String> actualList2 = new ArrayList<>();
      for (; results2.hasNext(); ) {
        QuerySolution soln = results2.nextSolution();
        actualList2.add(soln.toString());
        RDFNode node =  soln.get("?connection");
        String nodeStr = node.toString();
        needConnectionURI = URI.create(nodeStr);
      }
      qExec2.close();
      assertTrue("wrong number of results", actualList2.size() >= 1);

      Model needConnection = getEventListenerContext().getLinkedDataSource().getModelForResource(needConnectionURI);
      String queryString3 = sparqlPrefix +
        "SELECT ?remoteConnection WHERE {" +
        "?connection won:hasRemoteConnection ?remoteConnection"+
        "}";
      logger.debug(RdfUtils.toString(needConnection));
      Query query3 = QueryFactory.create(queryString3);
      QueryExecution qExec3 = QueryExecutionFactory.create(query3, needConnection);
      ResultSet results3 = qExec3.execSelect();
      List<String> actualList3 = new ArrayList<>();
      for (;results3.hasNext();){
        QuerySolution soln = results3.nextSolution();
        actualList3.add(soln.toString());
        RDFNode node = soln.get("?remoteConnection");
        String nodeStr = node.toString();
        commentConnectionsURI = URI.create(nodeStr);

      }
      assertTrue("wrong number of results", actualList3.size() >= 1);

      Model remoteConnections = getEventListenerContext().getLinkedDataSource().getModelForResource(commentConnectionsURI);
      String queryString4 = sparqlPrefix +
        "SELECT ?remoteConnection WHERE {" +
        "?connection won:hasRemoteConnection ?remoteConnection"+
        "}";
      logger.debug(RdfUtils.toString(remoteConnections));
      // ResultSet results3 = executeQuery(queryString2, needConnections);
      Query query4 = QueryFactory.create(queryString4);
      QueryExecution qExec4 = QueryExecutionFactory.create(query4, remoteConnections);
      ResultSet results4 = qExec4.execSelect();


      List<String> actualList4 = new ArrayList<>();
      for (; results4.hasNext(); ) {
        QuerySolution soln = results4.nextSolution();
        actualList4.add(soln.toString());
        RDFNode node =  soln.get("?remoteConnection");
        String nodeStr = node.toString();
        needConnectionURICheck = URI.create(nodeStr);
      }
      qExec4.close();
      assertTrue("wrong number of results", actualList4.size() >= 1);
      assertEquals(needConnectionURI,needConnectionURICheck);

    }
    public ResultSet executeQuery(String queryString, Model model){
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
