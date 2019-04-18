package won.owner.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Created by hfriedrich on 04.10.2016. creates events to signal the atom to
 * register at default won nodes. This is done either at context refresh or
 * every X seconds (see xml config)
 */
public class WonNodeRegistrationEventPublisher
                implements ApplicationEventPublisherAware, ApplicationListener<ContextRefreshedEvent> {
    protected ApplicationEventPublisher eventPublisher;

    public void publishScheduledEvent() {
        WonNodeRegistrationEvent event = new WonNodeRegistrationEvent(this);
        eventPublisher.publishEvent(event);
    }

    @Override
    public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
        eventPublisher = applicationEventPublisher;
    }

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent contextRefreshedEvent) {
        WonNodeRegistrationEvent event = new WonNodeRegistrationEvent(this);
        eventPublisher.publishEvent(event);
    }
}
