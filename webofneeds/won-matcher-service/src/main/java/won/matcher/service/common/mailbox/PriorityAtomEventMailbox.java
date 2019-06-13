package won.matcher.service.common.mailbox;

import com.typesafe.config.Config;

import akka.actor.ActorSystem;
import akka.dispatch.PriorityGenerator;
import akka.dispatch.UnboundedStablePriorityMailbox;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.event.Cause;

public class PriorityAtomEventMailbox extends UnboundedStablePriorityMailbox {
    // needed for reflective instantiation
    public PriorityAtomEventMailbox(ActorSystem.Settings settings, Config config) {
        // Create a new PriorityGenerator, lower prio means more important
        super(new PriorityGenerator() {
            @Override
            public int gen(Object event) {
                if (event instanceof AtomEvent) {
                    return ((AtomEvent) event).getCause().getPriority();
                }
                return Cause.LOWEST_PRIORTY;
            }
        });
    }
}