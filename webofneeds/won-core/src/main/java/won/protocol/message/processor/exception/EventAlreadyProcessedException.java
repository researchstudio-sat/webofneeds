package won.protocol.message.processor.exception;

/**
 * Indicates that the event has already been processed.
 *
 * User: ypanchenko Date: 27.04.2015
 */
public class EventAlreadyProcessedException extends WonMessageProcessingException {

    public EventAlreadyProcessedException(final String uri) {
        super(uri);
    }

}
