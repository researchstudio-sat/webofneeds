package won.bot.integrationtest;

import static junit.framework.TestCase.assertTrue;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDFS;
import org.junit.AfterClass;
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
import won.bot.framework.bot.context.ParticipantCoordinatorBotContextWrapper;
import won.bot.framework.eventbot.event.impl.lifecycle.WorkDoneEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.manager.impl.SpringAwareBotManagerImpl;
import won.bot.impl.BAAtomicCCActiveNotCompletingBot;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

/**
 * User: Danijel
 * Date: 16.4.14.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:/spring/app/baAtomicCCActiveNotComletingTest.xml"})

public class BAAtomicCCActiveNotCompletingBotTest
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

  private static SpringAwareBotManagerImpl staticBotManager;


  private static boolean run = false;

  @Before
  public void before(){
    if (!run)
    {
      //create a bot instance and auto-wire it
      AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
      bot = (MyBot) beanFactory.autowire(MyBot.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
      Object botBean = beanFactory.initializeBean(bot, "mybot");
      bot = (MyBot) botBean;
      //the bot also needs a trigger so its act() method is called regularly.
      // (there is no trigger bean in the context)
      PeriodicTrigger trigger = new PeriodicTrigger(ACT_LOOP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
      trigger.setInitialDelay(ACT_LOOP_INITIAL_DELAY_MILLIS);
      bot.setTrigger(trigger);
      logger.info("starting test case testBAAtomicCCActiveNotCompletingBot");
      //adding the bot to the bot manager will cause it to be initialized.
      //at that point, the trigger starts.
      botManager.setShutdownApplicationContextIfWorkDone(false);
      botManager.addBot(bot);
      staticBotManager = botManager;
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

  @AfterClass
  public static void shutdown(){

    //staticBotManager.setShutdownApplicationContextIfWorkDone(true);
  }

  /**
   * The main test method.
   * @throws Exception
   */
  @Test
  public void testBAAtomicCCActiveNotCompletingBot() throws Exception
  {
    //now check the results!
    logger.info("start test case testBAAtomicCCActiveNotCompletingBot");
    bot.executeAsserts();
    logger.info("finishing test case testBAAtomicCCActiveNotCompletingBot");
  }

  @Test
  public void testBAAtomicCCActiveNotCompletingRDF(){
    logger.info("starting test case testBAAtomicCCActiveNotCompletingRDF");
    bot.executeRDFValidationAssert();
    logger.info("finishing test case testBAAtomicCCActiveNotCompletingRDF");
  }


  @Test
  public void testBAAtomicCCActiveNotCompletingBAStateRDF(){
    logger.info("starting test case testBAAtomicCCActiveNotCompletingBAStateRDF");
    bot.executeBAStateRDFValidationAssert();
    logger.info("finishing test case testBAAtomicCCActiveNotCompletingBAStateRDF");
  }


  /**
   * We create a subclass of the bot we want to test here so that we can
   * add a listener to its internal event bus and to access its listeners, which
   * record information during the run that we later check with asserts.
   */
  public static class MyBot extends BAAtomicCCActiveNotCompletingBot
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
        "PREFIX wontx:   <http://purl.org/webofneeds/tx/model#>"+
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
      //Coordinator creator
      Assert.assertEquals(1, this.coordinatorNeedCreator.getEventCount());
      Assert.assertEquals(0, this.coordinatorNeedCreator.getExceptionCount());
      //28 Participants creator
      Assert.assertEquals(noOfNeeds-1, this.participantNeedCreator.getEventCount());
      Assert.assertEquals(0, this.participantNeedCreator.getExceptionCount());
      //Coordinator - Participants connector
      Assert.assertEquals(noOfNeeds, this.needConnector.getEventCount());
      Assert.assertEquals(0, this.needConnector.getExceptionCount());

      Assert.assertEquals(noOfNeeds-1, this.scriptsDoneListener.getEventCount());
      Assert.assertEquals(0, this.scriptsDoneListener.getExceptionCount());

      //29 needs deactivated
      Assert.assertEquals(noOfNeeds-1, this.workDoneSignaller.getEventCount());
      Assert.assertEquals(0, this.workDoneSignaller.getExceptionCount());

      //TODO: there is more to check:
      //* what does the RDF look like?
      // --> pull it from the needURI/ConnectionURI and check contents
      //* what does the database look like?

    }

    public void  executeRDFValidationAssert(){

      List<URI> needs = ((ParticipantCoordinatorBotContextWrapper) getBotContextWrapper()).getCoordinators();

      LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();

      if (linkedDataSource instanceof CachingLinkedDataSource) {
        ((CachingLinkedDataSource)linkedDataSource).clear();
      }

      List<URI> properties = new ArrayList<>();
      List<URI> objects = new ArrayList<>();

      properties.add(URI.create(WON.HAS_CONNECTIONS.getURI()));
      //properties.add(RDF.type);
      properties.add(URI.create(WON.HAS_REMOTE_CONNECTION.toString()));
      properties.add(URI.create(WON.HAS_REMOTE_NEED.toString()));
      properties.add(URI.create(RDFS.member.toString()));

      List<URI> crawled = new ArrayList<>();

      Dataset dataModel = linkedDataSource.getDataForResourceWithPropertyPath(needs.get(0),
        PropertyPathConfigurator
          .configurePropertyPaths
            (), 300, 4, true);
      logger.debug("crawled dataset: {}", RdfUtils.toString(dataModel));

      String queryString = sparqlPrefix +
        "SELECT ?need ?connection ?need2 WHERE {" +
        "?need won:hasConnections ?connections."+
        "?connections rdfs:member ?connection."+
        "?connection won:hasFacet won:BAAtomicCCCoordinatorFacet."+
        "?connection won:hasRemoteFacet won:BACCParticipantFacet."+
        "?connection won:hasRemoteConnection ?connection2."+
        "?connection2 won:belongsToNeed ?need2."+
        "?connection2 won:hasFacet won:BACCParticipantFacet. "+
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
      Assert.assertEquals(noOfNeeds - 1, actualList.size());
      qExec.close();
    }

    public void  executeBAStateRDFValidationAssert(){

      List<URI> needs = ((ParticipantCoordinatorBotContextWrapper) getBotContextWrapper()).getCoordinators();

      LinkedDataSource linkedDataSource = getEventListenerContext().getLinkedDataSource();

      if (linkedDataSource instanceof CachingLinkedDataSource) {
        ((CachingLinkedDataSource)linkedDataSource).clear();
      }

      List<URI> properties = new ArrayList<>();
      List<URI> objects = new ArrayList<>();

      properties.add(URI.create(WON.HAS_CONNECTIONS.getURI()));
      //properties.add(RDF.type);
      properties.add(URI.create(WON.HAS_REMOTE_CONNECTION.toString()));
      properties.add(URI.create(WON.HAS_REMOTE_NEED.toString()));
      properties.add(URI.create(RDFS.member.toString()));

      List<URI> crawled = new ArrayList<>();


      Dataset dataModel = linkedDataSource.getDataForResourceWithPropertyPath(needs.get(0),
        PropertyPathConfigurator
          .configurePropertyPaths
            (), 300, 4, true);

      logger.debug("crawled dataset: {}", RdfUtils.toString(dataModel));

      String queryString = sparqlPrefix +
        "SELECT ?need ?connection ?need2 WHERE {" +
        "?need won:hasConnections ?connections."+
        "?connections rdfs:member ?connection."+
        "?connection won:hasFacet won:BAAtomicCCCoordinatorFacet."+
        "?connection  wontx:hasBAState wontx:Ended."+
        "?connection won:hasRemoteConnection ?connection2."+
        "?connection2 won:belongsToNeed ?need2."+
        "?connection2 won:hasFacet won:BACCParticipantFacet. "+
        "?connection2  wontx:hasBAState wontx:Ended."+
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
      Assert.assertEquals(noOfNeeds-1, actualList.size());
      qExec.close();
    }
  }
}







