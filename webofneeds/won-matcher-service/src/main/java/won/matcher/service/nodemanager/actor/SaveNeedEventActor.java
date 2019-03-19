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
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.sparql.SparqlService;

/**
 * Actor that listens to the publish subscribe topic and saves the body (rdf graphs) of a need event to the defined
 * sparql endpoint.
 *
 * Created by hfriedrich on 12.10.2015.
 */
@Component
@Scope("prototype")
public class SaveNeedEventActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    private ActorRef pubSubMediator;

    @Autowired
    private SparqlService sparqlService;

    @Override
    public void preStart() {

        // Subscribe for need events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
        pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());
    }

    @Override
    public void onReceive(final Object o) throws Exception {

        if (o instanceof NeedEvent) {
            NeedEvent needEvent = (NeedEvent) o;

            // save the need
            log.debug("Save need event {} to sparql endpoint {}", needEvent, sparqlService.getSparqlEndpoint());
            Dataset ds = needEvent.deserializeNeedDataset();
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
