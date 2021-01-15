package won.integrationtest.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CyclicBarrier;

public class TripBarrierAction extends BaseEventBotAction {
    private CyclicBarrier barrier;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TripBarrierAction(final EventListenerContext eventListenerContext, final CyclicBarrier barrier) {
        super(eventListenerContext);
        this.barrier = barrier;
    }

    @Override
    protected void doRun(final Event event, EventListener eventListener) throws Exception {
        try {
            // together with the barrier.await() in the @Test method, this trips the barrier
            // and both threads continue.
            barrier.await();
        } catch (Exception e) {
            logger.warn("caught exception while waiting on barrier", e);
        }
    }
}
