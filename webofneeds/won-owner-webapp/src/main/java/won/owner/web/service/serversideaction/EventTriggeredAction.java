package won.owner.web.service.serversideaction;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class EventTriggeredAction<E> {
    private Predicate<Optional<E>> triggerPredicate;
    // a function accepting one event of type T, and produces zero or more
    // EventTriggeredActions
    private Function<Optional<E>, Collection<EventTriggeredAction<E>>> action;
    private String name;
    private Date created = new Date();

    /**
     * Constructor to use if the action is to be executed when the event is an empty
     * optional, which is the case when first adding the action to the container.
     */
    public EventTriggeredAction(String name, Function<Optional<E>, Collection<EventTriggeredAction<E>>> action) {
        this(name, e -> !e.isPresent(), action);
    }

    public EventTriggeredAction(String name, Predicate<Optional<E>> triggerPredicate,
                    Function<Optional<E>, Collection<EventTriggeredAction<E>>> action) {
        super();
        this.name = name;
        this.triggerPredicate = triggerPredicate;
        this.action = action;
    }

    public Collection<EventTriggeredAction<E>> executeFor(Optional<E> event) {
        return this.action.apply(event);
    }

    /**
     * Returns the duration since this action was created.
     **/
    public Duration getAge() {
        return Duration.ofMillis(System.currentTimeMillis() - created.getTime());
    }

    public boolean isTriggeredBy(Optional<E> event) {
        return this.triggerPredicate.test(event);
    }

    public String getName() {
        return name;
    }
}
