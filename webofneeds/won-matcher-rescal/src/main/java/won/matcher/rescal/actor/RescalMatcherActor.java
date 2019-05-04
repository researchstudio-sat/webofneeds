package won.matcher.rescal.actor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;
import won.matcher.rescal.config.RescalMatcherConfig;
import won.matcher.rescal.service.HintReader;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.utils.tensor.TensorEntry;
import won.matcher.utils.tensor.TensorEntryAllGenerator;
import won.matcher.utils.tensor.TensorEntryTokenizer;
import won.matcher.utils.tensor.TensorMatchingData;

/**
 * Main actor that controls the rescal matching process. It loads the atoms and
 * connection data from the rdf store, preprocess the data and save it to file
 * system for the actual rescal processing in python. After the rescal algorithm
 * finished execution the generated hints are loaded and send back for saving
 * and further processing.
 * <p>
 * Created by hfriedrich on 02.07.2015.
 */
@Component
@Scope("prototype")
public class RescalMatcherActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private long lastQueryDate = Long.MIN_VALUE;
    private TensorMatchingData rescalInputData = new TensorMatchingData();
    private static final String TICK = "tick";
    private ActorRef pubSubMediator;
    @Autowired
    private HintReader hintReader;
    @Autowired
    private RescalMatcherConfig config;

    @Override
    public void preStart() throws IOException {
        // subscribe to atom events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        // Execute the rescal algorithm regularly
        getContext().system().scheduler().schedule(FiniteDuration.Zero(), config.getExecutionDuration(), getSelf(),
                        TICK, getContext().dispatcher(), null);
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
     * Load the atom and connection data from the sparql endpoint, preprocess the
     * data and write it to some directory to be processed by the rescal python
     * algorithm that produces hints. The hints are then loaded and send to the
     * event bus.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void executeRescalAlgorithm() throws IOException, InterruptedException {
        // load the atoms and connections from the rdf store
        log.info("start processing (every {} minutes) ...", config.getExecutionDuration());
        long queryDate = System.currentTimeMillis();
        log.info("query atoms and connections from rdf store '{}' from date '{}' to date '{}'",
                        config.getSparqlEndpoint(), lastQueryDate, queryDate);
        // add the attributes of the atoms to the rescal tensor
        TensorEntryAllGenerator tensorEntryAllGenerator = new TensorEntryAllGenerator("queries/attribute",
                        config.getSparqlEndpoint(), lastQueryDate, queryDate);
        TensorEntryTokenizer tokenizer = new TensorEntryTokenizer(tensorEntryAllGenerator.generateTensorEntries());
        Collection<TensorEntry> tensorEntries = tokenizer.generateTensorEntries();
        for (TensorEntry entry : tensorEntries) {
            rescalInputData.addAtomAttribute(entry);
        }
        // add the connections between the atoms to the rescal tensor
        tensorEntryAllGenerator = new TensorEntryAllGenerator("queries/connection", config.getSparqlEndpoint(),
                        lastQueryDate, queryDate);
        tensorEntries = tensorEntryAllGenerator.generateTensorEntries();
        for (TensorEntry entry : tensorEntries) {
            rescalInputData.addAtomConnection(entry.getAtomUri(), entry.getValue(), true);
        }
        log.info("number of atoms in tensor: {}", rescalInputData.getAtoms().size());
        log.info("number of attributes in tensor: {}", rescalInputData.getAttributes().size());
        log.info("number of connections in tensor: {}", rescalInputData.getNumberOfConnections());
        log.info("number of slices in tensor: {}", rescalInputData.getSlices().size());
        if (!rescalInputData.isValidTensor()) {
            log.info("not enough tensor data available for execution yet, wait for next execution!");
            return;
        }
        // write the files for rescal algorithm
        log.info("write rescal input data to folder: {}", config.getExecutionDirectory());
        TensorMatchingData cleanedTensorData = rescalInputData.writeCleanedOutputFiles(config.getExecutionDirectory());
        int tensorSize = cleanedTensorData.getTensorDimensions()[0];
        if (rescalInputData.getAtoms().size() + rescalInputData.getAttributes().size() < config.getRescalRank()) {
            log.info("Do not start rescal algorithm since tensor size (number of atoms + number of attributes) = {} is "
                            + "smaller than rank parameter {}.", tensorSize, config.getRescalRank());
            return;
        }
        // execute the rescal algorithm in python
        String pythonCall = "python " + config.getPythonScriptDirectory() + "/rescal-matcher.py -inputfolder "
                        + config.getExecutionDirectory() + " -outputfolder " + config.getExecutionDirectory()
                        + "/output" + " -rank " + config.getRescalRank() + " -threshold " + config.getRescalThreshold();
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
        // load the predicted hints and send the to the event bus of the matching
        // service
        BulkHintEvent hintsEvent = hintReader.readHints(rescalInputData);
        int numHints = (hintsEvent == null || hintsEvent.getHintEvents() == null) ? 0
                        : hintsEvent.getHintEvents().size();
        log.info("loaded {} hints into bulk hint event and publish", numHints);
        if (numHints > 0) {
            StringBuilder builder = new StringBuilder();
            for (HintEvent hint : hintsEvent.getHintEvents()) {
                builder.append("\n- " + hint);
            }
            log.info(builder.toString());
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(hintsEvent.getClass().getName(), hintsEvent),
                            getSelf());
        }
        lastQueryDate = queryDate;
    }
}
