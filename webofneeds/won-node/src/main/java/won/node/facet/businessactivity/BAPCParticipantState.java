package won.node.facet.businessactivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 24.1.14.
 * Time: 16.01
 * To change this template use File | Settings | File Templates.
 */
public enum BAPCParticipantState {
    ACTIVE("Active"),
    CANCELING("Canceling"),
    COMPLETED("Completed"),
    CLOSING("Closing"),
    COMPENSATING("Compensating"),
    FAILING_ACTIVE_CANCELING("FailingActiveCanceling"),
    FAILING_COMPENSATING("FailingCompensating"),
    NOT_COMPLETING("NotCompleting"),
    EXITING("Exiting"),
    ENDED("Ended"),
    CLOSED("Closed");

    private static final Logger logger = LoggerFactory.getLogger(BAPCParticipantState.class);

    private String name;

    private BAPCParticipantState(String name)
    {
        this.name = name;
    }

    public static BAPCParticipantState create(BAEventType msg)
    {
        switch (msg) {
            case MESSAGE_CANCEL:
                return CANCELING;
            case MESSAGE_CLOSE:
                return CLOSING;
            case MESSAGE_COMPENSATE:
                return COMPENSATING;
            case MESSAGE_FAILED:
                return ENDED;
            case MESSAGE_EXITED:
                return ENDED;
            case MESSAGE_NOTCOMPLETED:
                return ENDED;
            case MESSAGE_EXIT:
                return EXITING;
            case MESSAGE_COMPLETED:
                return COMPLETED;
            case MESSAGE_FAIL:
                return FAILING_ACTIVE_CANCELING;
            case MESSAGE_CANNOTCOMPLETE:
                return NOT_COMPLETING;
            case MESSAGE_CANCELED:
                return ENDED;
            case MESSAGE_CLOSED:
                return ENDED;
            case MESSAGE_COMPENSATED:
                return ENDED;
        }
        throw new IllegalArgumentException("Connection creation failed: Wrong ConnectionEventType");
    }

    public BAPCParticipantState transit(BAEventType msg)
    {
        switch (this) {
            case ACTIVE:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return CANCELING;
                    case MESSAGE_EXIT:
                        return EXITING;
                    case MESSAGE_COMPLETED:
                        return COMPLETED;
                    case MESSAGE_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    default:
                        return ACTIVE;
                }
            case CANCELING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return CANCELING;
                    case MESSAGE_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case MESSAGE_CANCELED:
                        return ENDED;
                    default:
                        return CANCELING;
                }

            case COMPLETED:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return COMPLETED;
                    case MESSAGE_CLOSE:
                        return CLOSING;
                    case MESSAGE_COMPENSATE:
                        return COMPENSATING;
                    case MESSAGE_COMPLETED:
                        return COMPLETED;
                    default:
                        return COMPLETED;
                }

            case CLOSING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return CLOSING;
                    case MESSAGE_CLOSE:
                        return CLOSING;
                    case MESSAGE_CLOSED:
                        return ENDED;
                    default:
                        return CLOSING;
                }

            case COMPENSATING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return COMPENSATING;
                    case MESSAGE_COMPENSATE:
                        return COMPENSATING;
                    case MESSAGE_FAIL:
                        return FAILING_COMPENSATING;
                    case MESSAGE_COMPENSATED:
                        return ENDED;
                    default:
                        return COMPENSATING;
                }

            case FAILING_ACTIVE_CANCELING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return FAILING_ACTIVE_CANCELING;
                    case MESSAGE_FAILED:
                        return ENDED;
                    case MESSAGE_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    default:
                        return FAILING_ACTIVE_CANCELING;
                }

            case FAILING_COMPENSATING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return FAILING_COMPENSATING;
                    case MESSAGE_COMPENSATE:
                        return FAILING_COMPENSATING;
                    case MESSAGE_FAILED:
                        return ENDED;
                    case MESSAGE_FAIL:
                        return FAILING_COMPENSATING;
                    default:
                        return FAILING_COMPENSATING;
                }

            case NOT_COMPLETING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return NOT_COMPLETING;
                    default:
                        return NOT_COMPLETING;
                }

            case EXITING:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        return EXITING;
                    case MESSAGE_EXITED:
                        return ENDED;
                    case MESSAGE_EXIT:
                        return EXITING;
                    default:
                        return EXITING;
                }

            case ENDED:
                switch (msg) {
                    case MESSAGE_CANCEL:
                        logger.info("send MESSAGE_CANCELED");
                        return ENDED;
                    case MESSAGE_CLOSE:
                        logger.info("send MESSAGE_CLOSED");
                        return ENDED;
                    case MESSAGE_COMPENSATE:
                        logger.info("send MESSAGE_COMPENSATED");
                        return ENDED;
                    case MESSAGE_FAILED:
                        logger.info("send Ignore");
                        return ENDED;
                    case MESSAGE_EXITED:
                        logger.info("send Ignore");
                        return ENDED;
                    case MESSAGE_NOTCOMPLETED:
                        logger.info("send Ignore");
                        return ENDED;
                    case MESSAGE_CANCELED:
                        return ENDED;
                    case MESSAGE_CLOSED:
                        return ENDED;
                    case MESSAGE_COMPENSATED:
                        return ENDED;
                    default:
                        logger.info("Invalid State");
                        return ENDED;
                }
        }
        return this;
    }

    public URI getURI()
    {
        return URI.create(WON.BASE_URI + name);
    }


    /**
     * Tries to match the given string against all enum values.
     *
     * @param fragment string to match
     * @return matched enum, null otherwise
     */
    public static BAPCParticipantState parseString(final String fragment)
    {
        for (BAPCParticipantState state : values())
            if (state.name.equals(fragment))
                return state;

        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }
}

