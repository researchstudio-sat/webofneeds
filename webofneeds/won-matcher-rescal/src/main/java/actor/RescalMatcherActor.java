package actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.config.CommonSettings;
import common.config.CommonSettingsImpl;
import common.event.BulkHintEvent;
import config.RescalMatcherSettings;
import config.RescalMatcherSettingsImpl;
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
public class RescalMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private long lastQueryDate;
  private RescalSparqlService sparqlService;
  private RescalMatchingData rescalInputData;
  private CommonSettingsImpl settings;
  private RescalMatcherSettingsImpl rescalSettings;
  private static final String TICK = "tick";

  public RescalMatcherActor() throws IOException {

    settings = CommonSettings.SettingsProvider.get(getContext().system());
    rescalSettings = RescalMatcherSettings.SettingsProvider.get(getContext().system());
    sparqlService = new RescalSparqlService(settings.SPARQL_ENDPOINT, rescalSettings.NLP_RESOURCE_DIRECTORY);
    lastQueryDate = Long.MIN_VALUE;
    rescalInputData = new RescalMatchingData();
  }

  @Override
  public void preStart() {

    // Execute the rescal algorithm regularly
    getContext().system().scheduler().schedule(
      FiniteDuration.Zero(), rescalSettings.EXECUTION_DURATION, getSelf(), TICK, getContext().dispatcher(), null);
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

    BulkHintEvent e = HintReader.readHints(rescalSettings.EXECUTION_DIRECTORY);
    getContext().system().eventStream().publish(e);



    // load the needs and connections from the rdf store
    long queryDate = System.currentTimeMillis();
    log.info("query needs and connections from rdf store '{}' from date '{}' to date '{}'", settings.SPARQL_ENDPOINT,
             lastQueryDate, queryDate);
    sparqlService.updateMatchingDataWithActiveNeeds(rescalInputData, lastQueryDate, queryDate);
    sparqlService.updateMatchingDataWithConnections(rescalInputData, lastQueryDate, queryDate);

    // write the files for rescal algorithm
    log.info("write rescal input data to folder: {}", rescalSettings.EXECUTION_DIRECTORY);
    rescalInputData.writeCleanedOutputFiles(rescalSettings.EXECUTION_DIRECTORY);

    // execute the rescal algorithm in python
    String pythonCall = "python " + rescalSettings.PYTHON_SCRIPT_DIRECTORY + "/rescal-matcher.py -folder \"" +
      rescalSettings.EXECUTION_DIRECTORY + "\" -rank " + rescalSettings.RANK + "-threshold " + rescalSettings.THRESHOLD;
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
    BulkHintEvent hintsEvent = HintReader.readHints(rescalSettings.EXECUTION_DIRECTORY);
    log.info("loaded {} hints into bulk hint event and publish", hintsEvent.getHintEvents().size());
    getContext().system().eventStream().publish(hintsEvent);
    lastQueryDate = queryDate;
  }
}
