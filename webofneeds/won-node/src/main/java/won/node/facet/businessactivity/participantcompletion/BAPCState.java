package won.node.facet.businessactivity.participantcompletion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 30.1.14.
 * Time: 18.33
 * To change this template use File | Settings | File Templates.
 */
public enum BAPCState {
    SUGGESTED("Suggested"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    REQUEST_SENT("RequestSent"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    REQUEST_RECEIVED("RequestReceived"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    CONNECTED("Connected"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    ACTIVE("Active"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CANCELING;
                case MESSAGE_EXIT:
                    return EXITING;
                case MESSAGE_COMPLETED:
                    return COMPLETED;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING;
                case MESSAGE_CANNOTCOMPLETE:
                    return NOT_COMPLETING;
                default:
                    return ACTIVE;
            }
        }
    },
    CANCELING("Canceling"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CANCELING;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING;
                case MESSAGE_CANCELED:
                    return ENDED;
                case MESSAGE_EXIT:
                    return EXITING;
                case MESSAGE_COMPLETED:
                    return COMPLETED;
                case MESSAGE_CANNOTCOMPLETE:
                    return NOT_COMPLETING;
                default:
                    return CANCELING;
            }
        }
    },
    COMPLETED("Completed"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BAPCEventType.MESSAGE_COMPLETED;
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
        }
    },
    CLOSING("Closing") {
        public BAPCState transit (BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CLOSING;
                case MESSAGE_CLOSE:
                    return CLOSING;
                case MESSAGE_CLOSED:
                    return ENDED;
                case MESSAGE_COMPLETED:
                    resendEvent = BAPCEventType.MESSAGE_CLOSE;
                    return CLOSING;
                default:
                    return CLOSING;
            }
        }
    },
    COMPENSATING("Compensating"){
        public BAPCState transit (BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return COMPENSATING;
                case MESSAGE_COMPENSATE:
                    return COMPENSATING;
                case MESSAGE_FAIL:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPENSATED:
                    return ENDED;
                case MESSAGE_COMPLETED:
                    resendEvent = BAPCEventType.MESSAGE_COMPENSATE;
                    return COMPENSATING;
                default:
                    return COMPENSATING;
            }
        }
    },
    FAILING_ACTIVE_CANCELING("FailingActiveCanceling") {
        public BAPCState transit (BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BAPCEventType.MESSAGE_FAIL;
                    return FAILING_ACTIVE_CANCELING;
                case MESSAGE_FAILED:
                    return ENDED;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING;
                default:
                    return FAILING_ACTIVE_CANCELING;
            }
        }
    },
    FAILING_COMPENSATING("FailingCompensating") {
        public BAPCState transit (BAPCEventType msg) {
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPENSATE:
                    resendEvent = BAPCEventType.MESSAGE_FAIL;
                    return FAILING_COMPENSATING;
                case MESSAGE_FAILED:
                    return ENDED;
                case MESSAGE_FAIL:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPLETED:
                    return FAILING_COMPENSATING;
                default:
                    return FAILING_COMPENSATING;
            }
        }
    },
    NOT_COMPLETING("NotCompleting"){
        public BAPCState transit (BAPCEventType msg) {
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BAPCEventType.MESSAGE_CANNOTCOMPLETE;
                    return NOT_COMPLETING;
                case MESSAGE_NOTCOMPLETED:
                    return ENDED;
                case MESSAGE_CANNOTCOMPLETE:
                    return NOT_COMPLETING;
                default:
                    return NOT_COMPLETING;
            }
        }
    },
    EXITING("Exiting") {
        public BAPCState transit (BAPCEventType msg) {
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BAPCEventType.MESSAGE_EXIT;
                    return EXITING;
                case MESSAGE_EXITED:
                    return ENDED;
                case MESSAGE_EXIT:
                    return EXITING;
                default:
                    return EXITING;
            }
        }
    },
    ENDED("Ended") {
        public BAPCState transit (BAPCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BAPCEventType.MESSAGE_CANCELED;
                    return ENDED;
                case MESSAGE_CLOSE:
                    resendEvent = BAPCEventType.MESSAGE_CLOSED;
                    return ENDED;
                case MESSAGE_COMPENSATE:
                    resendEvent = BAPCEventType.MESSAGE_COMPENSATED;
                    return ENDED;
                case MESSAGE_FAILED:
                    return ENDED;
                case MESSAGE_EXITED:
                    return ENDED;
                case MESSAGE_NOTCOMPLETED:
                    return ENDED;
                case MESSAGE_CANCELED:
                    return ENDED;
                case MESSAGE_CLOSED:
                    return ENDED;
                case MESSAGE_COMPENSATED:
                    return ENDED;
                case MESSAGE_EXIT:
                    resendEvent = BAPCEventType.MESSAGE_EXITED;
                    return ENDED;
                case MESSAGE_COMPLETED:
                    return ENDED;
                case MESSAGE_FAIL:
                    resendEvent = BAPCEventType.MESSAGE_FAILED;
                    return ENDED;
                case MESSAGE_CANNOTCOMPLETE:
                    resendEvent = BAPCEventType.MESSAGE_NOTCOMPLETED;
                    return ENDED;
                default:
                    return ENDED;
            }
        }
    },
    CLOSED("Closed"){
        public BAPCState transit(BAPCEventType msg){
            resendEvent = null;
            return null;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(BAPCState.class);

    private String name;
    private static BAPCEventType resendEvent = null;

    private BAPCState(String name)
    {
        this.name = name;
    }

    public BAPCEventType getResendEvent(){
        return  resendEvent;
    }


    public static BAPCState create(BAPCEventType msg)
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
        throw new IllegalArgumentException("The received message is not allowed.");
    }

    public abstract BAPCState transit (BAPCEventType msg);

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
    public static BAPCState parseString(final String fragment)
    {
        for (BAPCState state : values())
            if (state.name.equals(fragment))
                return state;

        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }



}


