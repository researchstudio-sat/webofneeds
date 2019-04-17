package won.matcher.service.nodemanager.actor;

import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.service.sparql.SparqlService;

/**
 * Actor that listens to the publish subscribe topic and saves the body (rdf
 * graphs) of an atom event to the defined sparql endpoint. Created by
 * hfriedrich on 12.10.2015.
 */
@Component
@Scope("prototype")
public class SaveAtomEventActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef pubSubMediator;
    @Autowired
    private SparqlService sparqlService;

    @Override
    public void preStart() {
        // Subscribe for atom events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(AtomEvent.class.getName(), getSelf()), getSelf());
    }

    @Override
    public void onReceive(final Object o) throws Exception {
        if (o instanceof AtomEvent) {
            AtomEvent atomEvent = (AtomEvent) o;
            // save the atom
            log.debug("Save atom event {} to sparql endpoint {}", atomEvent, sparqlService.getSparqlEndpoint());
            Dataset ds = atomEvent.deserializeAtomDataset();
            sparqlService.updateNamedGraphsOfDataset(ds);
        } else {
            unhandled(o);
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        SupervisorStrategy supervisorStrategy = new OneForOneStrategy(0, Duration.Zero(),
                        new Function<Throwable, SupervisorStrategy.Directive>() {
                            @Override
                            public SupervisorStrategy.Directive apply(Throwable t) throws Exception {
                                log.warning("Actor encountered error: {}", t);
                                // default behaviour
                                return SupervisorStrategy.escalate();
                            }
                        });
        return supervisorStrategy;
    }
}
