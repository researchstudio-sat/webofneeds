package won.owner.messaging;

import org.springframework.context.ApplicationEvent;

/**
 * Created by hfriedrich on 04.10.2016. Event signals the atom to connect to the
 * default won node.
 */
public class WonNodeRegistrationEvent extends ApplicationEvent {
    public WonNodeRegistrationEvent(final Object source) {
        super(source);
    }
}
