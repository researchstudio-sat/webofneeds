package won.bot.framework.eventbot.action.impl.trigger;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import won.bot.framework.eventbot.EventListenerContext;

/**
 * A BotTrigger that will fire in specified intervals until its fireCount is
 * reached. Additional firings can be added using addFirings(). The trigger
 * tries to speed up and slow down in order to fire as often as possible, but
 * only when it is allowed to.
 * 
 * @author fkleedorfer
 */
public class FireCountLimitedBotTrigger extends BotTrigger {
    private AtomicInteger firings;

    public FireCountLimitedBotTrigger(EventListenerContext context, Duration interval, int initialFireCount) {
        super(context, interval);
        this.firings = new AtomicInteger(initialFireCount);
    }

    public void addFirings(int delta) {
        this.firings.addAndGet(delta);
    }

    @Override
    protected void fire() {
        if (this.firings.getAndDecrement() > 0) {
            super.fire();
            changeIntervalByFactor(0.99);
        } else {
            this.firings.incrementAndGet();
            changeIntervalByFactor(1.01);
        }
    }
}
