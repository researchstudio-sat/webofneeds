package won.owner.web.service.serversideaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.LoggingUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EventTriggeredActionContainer<E> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private List<EventTriggeredAction<E>> actions = new ArrayList<>();
  private List<EventTriggeredAction<E>> actionsToAdd = new ArrayList<>();
  private Duration maxAge = Duration.ofMinutes(10);
  private ExecutorService executor = Executors.newSingleThreadExecutor();

  public EventTriggeredActionContainer() {
    this(Duration.ofMinutes(10));
  }

  public EventTriggeredActionContainer(Duration maxAge) {
    super();
    this.maxAge = maxAge;
  }

  /**
   * Executes the action once with an empty even and adds the resulting actions to
   * the container.
   */
  public synchronized void addAction(EventTriggeredAction<E> action) {
    try {
      this.actionsToAdd.addAll(action.executeFor(Optional.empty()));
    } catch (Exception e) {
      LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e,
          "Error running server side action {} for empty event", action.getName());
    }
  }

  public void executeFor(Optional<E> event) {
    try {
      Objects.nonNull(event);
      // handle the event in a single-threaded executor
      executor.execute(new Runnable() {
        @Override
        public void run() {
          // add all new actions that may have accumulated during executions
          synchronized (EventTriggeredActionContainer.this) {
            actions.addAll(actionsToAdd);
            actionsToAdd.clear();
          }
          // walk over all actions, filtering them if too old, collecting the actions they
          // spawn
          EventTriggeredActionContainer.this.actions = EventTriggeredActionContainer.this.actions.stream()
              .map((Function<EventTriggeredAction<E>, Collection<EventTriggeredAction<E>>>) action -> {
                try {
                  if (action.getAge().compareTo(maxAge) > 0) {
                    // action is too old, don't execute it, and don't return it
                    return Collections.emptyList();
                  }
                  if (action.isTriggeredBy(event)) {
                    // return whatever action the current action produces
                    return (Collection<EventTriggeredAction<E>>) action.executeFor(event);
                  } else {
                    // return the action so it is kept in the list of actions to be executed
                    return Arrays.asList(action);
                  }
                } catch (Exception e) {
                  LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e, "Error running server side action {}",
                      action.getName());
                }
                // something went wrong: remove action
                return Collections.emptyList();
              }).flatMap(s -> s.stream()).collect(Collectors.toList());
        }
      });
    } catch (Exception e) {
      LoggingUtils.logMessageAsInfoAndStacktraceAsDebug(logger, e, "Error running server side actions for event {}",
          event);
    }
  }
}
