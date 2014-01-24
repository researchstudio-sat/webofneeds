package won.node.facet.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 14.1.14.
 * Time: 11.41
 * To change this template use File | Settings | File Templates.
 */
public enum BAParticipantCompletionState {
    SUGGESTED("Suggested"),
    REQUEST_SENT("RequestSent"),
    REQUEST_RECEIVED("RequestReceived"),
    CONNECTED("Connected"),
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

    private static final Logger logger = LoggerFactory.getLogger(BAParticipantCompletionState.class);

    private String name;

    private BAParticipantCompletionState(String name)
    {
        this.name = name;
    }

    public static BAParticipantCompletionState create(BAEventType msg)
    {
        switch (msg) {
            case PARTICIPANT_INBOUND_CANCEL:
                return CANCELING;
            case PARTICIPANT_INBOUND_CLOSE:
                return CLOSING;
            case PARTICIPANT_INBOUND_COMPENSATE:
                return COMPENSATING;
            case PARTICIPANT_INBOUND_FAILED:
                return ENDED;
            case PARTICIPANT_INBOUND_EXITED:
                return ENDED;
            case PARTICIPANT_INBOUND_NOT_COMPLETED:
                return ENDED;
            case PARTICIPANT_OUTBOUND_EXIT:
                return EXITING;
            case PARTICIPANT_OUTBOUND_COMPLETED:
                return COMPLETED;
            case PARTICIPANT_OUTBOUND_FAIL:
                return FAILING_ACTIVE_CANCELING;
            case PARTICIPANT_OUTBOUND_CANNOT_COMPLETE:
                return NOT_COMPLETING;
            case PARTICIPANT_OUTBOUND_CANCELED:
                return ENDED;
            case PARTICIPANT_OUTBOUND_CLOSED:
                return ENDED;
            case PARTICIPANT_OUTBOUND_COMPENSATED:
                return ENDED;
            case COORDINATOR_INBOUND_EXIT:
                return EXITING;
            case COORDINATOR_INBOUND_COMPLETED:
                return COMPLETED;
            case COORDINATOR_INBOUND_FAIL:
                return FAILING_ACTIVE_CANCELING;
            case COORDINATOR_INBOUND_CANNOT_COMPLETE:
                return NOT_COMPLETING;
            case COORDINATOR_INBOUND_CANCELED:
                return ENDED;
            case COORDINATOR_INBOUND_CLOSED:
                return ENDED;
            case COORDINATOR_INBOUND_COMPENSATED:
                return ENDED;
            case COORDINATOR_OUTBOUND_CANCEL:
                return CANCELING;
            case COORDINATOR_OUTBOUND_CLOSE:
                return CLOSING;
            case COORDINATOR_OUTBOUND_COMPENSATE:
                return COMPENSATING;
            case COORDINATOR_OUTBOUND_FAILED:
                return ENDED;
            case COORDINATOR_OUTBOUND_EXITED:
                return ENDED;
            case COORDINATOR_OUTBOUND_NOT_COMPLETED:
                return ENDED;
        }
        throw new IllegalArgumentException("Connection creation failed: Wrong ConnectionEventType");
    }

    public BAParticipantCompletionState transit(BAEventType msg)
    {
        switch (this) {
            case ACTIVE:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return CANCELING;
                    case PARTICIPANT_OUTBOUND_EXIT:
                        return EXITING;
                    case PARTICIPANT_OUTBOUND_COMPLETED:
                        return COMPLETED;
                    case PARTICIPANT_OUTBOUND_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case PARTICIPANT_OUTBOUND_CANNOT_COMPLETE:
                        return NOT_COMPLETING;
                    case COORDINATOR_INBOUND_EXIT:
                        return EXITING;
                    case COORDINATOR_INBOUND_COMPLETED:
                        return COMPLETED;
                    case COORDINATOR_INBOUND_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case COORDINATOR_INBOUND_CANNOT_COMPLETE:
                        return NOT_COMPLETING;
                    case COORDINATOR_OUTBOUND_CANCEL:
                        return CANCELING;
                    default:
                        return ACTIVE;
                }
            case CANCELING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return CANCELING;
                    case PARTICIPANT_OUTBOUND_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case PARTICIPANT_OUTBOUND_CANCELED:
                        return ENDED;
                    case COORDINATOR_INBOUND_EXIT:
                        return EXITING;
                    case COORDINATOR_INBOUND_COMPLETED:
                        return COMPLETED;
                    case COORDINATOR_INBOUND_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case COORDINATOR_INBOUND_CANNOT_COMPLETE:
                        return NOT_COMPLETING;
                    case COORDINATOR_INBOUND_CANCELED:
                        return ENDED;
                    case COORDINATOR_OUTBOUND_CANCEL:
                        return CANCELING;
                    default:
                        return CANCELING;
                }

            case COMPLETED:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return COMPLETED;
                    case PARTICIPANT_INBOUND_CLOSE:
                        return CLOSING;
                    case PARTICIPANT_INBOUND_COMPENSATE:
                        return COMPENSATING;
                    case PARTICIPANT_OUTBOUND_COMPLETED:
                        return COMPLETED;
                    case COORDINATOR_INBOUND_COMPLETED:
                        return COMPLETED;
                    case COORDINATOR_OUTBOUND_CLOSE:
                        return CLOSING;
                    case COORDINATOR_OUTBOUND_COMPENSATE:
                        return COMPENSATING;
                    default:
                        return COMPLETED;
                }

            case CLOSING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return CLOSING;
                    case PARTICIPANT_INBOUND_CLOSE:
                        return CLOSING;
                    case PARTICIPANT_OUTBOUND_CLOSED:
                        return ENDED;
                    case COORDINATOR_INBOUND_COMPLETED:
                        return CLOSING;
                    case COORDINATOR_INBOUND_CLOSED:
                        return ENDED;
                    case COORDINATOR_OUTBOUND_CLOSE:
                        return CLOSING;
                    default:
                        return CLOSING;
                }

            case COMPENSATING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return COMPENSATING;
                    case PARTICIPANT_INBOUND_COMPENSATE:
                        return COMPENSATING;
                    case PARTICIPANT_OUTBOUND_FAIL:
                        return FAILING_COMPENSATING;
                    case PARTICIPANT_OUTBOUND_COMPENSATED:
                        return ENDED;
                    case COORDINATOR_INBOUND_COMPLETED:
                        return COMPENSATING;
                    case COORDINATOR_INBOUND_FAIL:
                        return FAILING_COMPENSATING;
                    case COORDINATOR_INBOUND_COMPENSATED:
                        return ENDED;
                    case COORDINATOR_OUTBOUND_COMPENSATE:
                        return COMPENSATING;
                    default:
                        return COMPENSATING;
                }

            case FAILING_ACTIVE_CANCELING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return FAILING_ACTIVE_CANCELING;
                    case PARTICIPANT_INBOUND_FAILED:
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case COORDINATOR_INBOUND_FAIL:
                        return FAILING_ACTIVE_CANCELING;
                    case COORDINATOR_OUTBOUND_FAILED:
                        return ENDED;
                    default:
                        return FAILING_ACTIVE_CANCELING;
                }

            case FAILING_COMPENSATING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return FAILING_COMPENSATING;
                    case PARTICIPANT_INBOUND_COMPENSATE:
                        return FAILING_COMPENSATING;
                    case PARTICIPANT_INBOUND_FAILED:
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_FAIL:
                        return FAILING_COMPENSATING;
                    case COORDINATOR_INBOUND_COMPLETED:
                        return FAILING_COMPENSATING;
                    case COORDINATOR_INBOUND_FAIL:
                        return FAILING_COMPENSATING;
                    case COORDINATOR_OUTBOUND_FAILED:
                        return ENDED;
                    default:
                        return FAILING_COMPENSATING;
                }

            case NOT_COMPLETING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return NOT_COMPLETING;
                    case PARTICIPANT_INBOUND_NOT_COMPLETED:
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_CANNOT_COMPLETE:
                        return NOT_COMPLETING;
                    case COORDINATOR_INBOUND_CANNOT_COMPLETE:
                        return NOT_COMPLETING;
                    case COORDINATOR_OUTBOUND_NOT_COMPLETED:
                        return ENDED;
                    default:
                        return NOT_COMPLETING;
                }

            case EXITING:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        return EXITING;
                    case PARTICIPANT_INBOUND_EXITED:
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_EXIT:
                        return EXITING;
                    case COORDINATOR_INBOUND_EXIT:
                        return EXITING;
                    case COORDINATOR_OUTBOUND_EXITED:
                        return ENDED;
                    default:
                        return EXITING;
                }

            case ENDED:
                switch (msg) {
                    case PARTICIPANT_INBOUND_CANCEL:
                        logger.info("send PARTICIPANT_OUTBOUND_CANCELED");
                        return ENDED;
                    case PARTICIPANT_INBOUND_CLOSE:
                        logger.info("send PARTICIPANT_OUTBOUND_CLOSED");
                        return ENDED;
                    case PARTICIPANT_INBOUND_COMPENSATE:
                        logger.info("send PARTICIPANT_OUTBOUND_COMPENSATED");
                        return ENDED;
                    case PARTICIPANT_INBOUND_FAILED:
                        logger.info("send Ignore");
                        return ENDED;
                    case PARTICIPANT_INBOUND_EXITED:
                        logger.info("send Ignore");
                        return ENDED;
                    case PARTICIPANT_INBOUND_NOT_COMPLETED:
                        logger.info("send Ignore");
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_CANCELED:
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_CLOSED:
                        return ENDED;
                    case PARTICIPANT_OUTBOUND_COMPENSATED:
                        return ENDED;
                    case COORDINATOR_INBOUND_EXIT:
                        logger.info("resend COORDINATOR_OUTBOUND_EXITED");
                        return ENDED;
                    case COORDINATOR_INBOUND_COMPLETED:
                        logger.info("send Ignore");
                        return ENDED;
                    case COORDINATOR_INBOUND_FAIL:
                        logger.info("resend COORDINATOR_OUTBOUND_FAILED");
                        return ENDED;
                    case COORDINATOR_INBOUND_CANNOT_COMPLETE:
                        logger.info("resend COORDINATOR_OUTBOUND_NOT_COMPLETED");
                        return ENDED;
                    case COORDINATOR_INBOUND_CANCELED:
                        logger.info("send Ignore");
                        return ENDED;
                    case COORDINATOR_INBOUND_CLOSED:
                        logger.info("send Ignore");
                        return ENDED;
                    case COORDINATOR_INBOUND_COMPENSATED:
                        logger.info("send Ignore");
                        return ENDED;
                    case COORDINATOR_OUTBOUND_FAILED:
                        return ENDED;
                    case COORDINATOR_OUTBOUND_EXITED:
                        return ENDED;
                    case COORDINATOR_OUTBOUND_NOT_COMPLETED:
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
    public static BAParticipantCompletionState parseString(final String fragment)
    {
        for (BAParticipantCompletionState state : values())
            if (state.name.equals(fragment))
                return state;

        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }
}

