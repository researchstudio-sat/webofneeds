package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import config.RescalMatcherConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import rescal.HintReader;
import rescal.RescalMatchingData;
import rescal.RescalSparqlService;
import scala.concurrent.duration.FiniteDuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Main actor that controls the rescal matching process. It loads the needs and connection data from the rdf
 * store, preprocess the data and save it to file system for the actual rescal processing in python.
 * After the rescal algorithm finished execution the generated hints are loaded and send back for saving and further
 * processing.
 *
 * Created by hfriedrich on 02.07.2015.
 */
@Component
@Scope("prototype")
public class RescalMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private long lastQueryDate = Long.MIN_VALUE;
  private RescalSparqlService sparqlService;
  private RescalMatchingData rescalInputData = new RescalMatchingData();
  private static final String TICK = "tick";
  private ActorRef pubSubMediator;

  @Autowired
  private RescalMatcherConfig config;


  @Override
  public void preStart() throws IOException {

    // init sparql service
    sparqlService = new RescalSparqlService(config.getSparqlEndpoint());

    // subscribe to need events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();

    // Execute the rescal algorithm regularly
    getContext().system().scheduler().schedule(
      FiniteDuration.Zero(), config.getExecutionDuration(), getSelf(), TICK, getContext().dispatcher(), null);
  }

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o.equals(TICK)) {
      executeRescalAlgorithm();
    } else {
      unhandled(o);
    }
  }

  /**
   * Load the need and connection data from the sparql endpoint, preprocess the data and write it to some directory
   * to be processed by the rescal python algorithm that produces hints. The hints are then loaded and send to
   * the event bus.
   *
   * @throws IOException
   * @throws InterruptedException
   */
  private void executeRescalAlgorithm() throws IOException, InterruptedException {

    // load the needs and connections from the rdf store
    long queryDate = System.currentTimeMillis();
    log.info("query needs and connections from rdf store '{}' from date '{}' to date '{}'", config.getSparqlEndpoint(),
             lastQueryDate, queryDate);
    sparqlService.updateMatchingDataWithActiveNeeds(rescalInputData, lastQueryDate, queryDate);
    sparqlService.updateMatchingDataWithConnections(rescalInputData, lastQueryDate, queryDate);

    // write the files for rescal algorithm
    log.info("write rescal input data to folder: {}", config.getExecutionDirectory());
    rescalInputData.writeCleanedOutputFiles(config.getExecutionDirectory());

    // execute the rescal algorithm in python
    String pythonCall = "python " + config.getPythonScriptDirectory() + "/rescal-matcher.py -folder \"" +
      config.getExecutionDirectory() + "\" -rank " + config.getRescalRank() + " -threshold " + config.getRescalThreshold();
    log.info("execute python script: " + pythonCall);
    Process pythonProcess = Runtime.getRuntime().exec(pythonCall);

    BufferedReader in = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()));
    String line;
    while ((line = in.readLine()) != null) {
      log.info(line);
    }
    in.close();

    BufferedReader err = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()));
    while ((line = err.readLine()) != null) {
      log.warning(line);
    }
    err.close();

    int returnCode = pythonProcess.waitFor();
    if (returnCode != 0) {
      log.error("rescal python call returned error code: " + returnCode);
      return;
    }

    // load the predicted hints and send the to the event bus of the matching service
    BulkHintEvent hintsEvent = HintReader.readHints(config.getExecutionDirectory(), rescalInputData);
    log.info("loaded {} hints into bulk hint event and publish", hintsEvent.getHintEvents().size());
    pubSubMediator.tell(new DistributedPubSubMediator.Publish(hintsEvent.getClass().getName(), hintsEvent), getSelf());
    lastQueryDate = queryDate;
  }
}
