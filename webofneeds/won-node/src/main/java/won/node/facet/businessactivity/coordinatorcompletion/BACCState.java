package won.node.facet.businessactivity.coordinatorcompletion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 6.2.14.
 * Time: 15.53
 * To change this template use File | Settings | File Templates.
 */
public enum BACCState {
    SUGGESTED("Suggested"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    REQUEST_SENT("RequestSent"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    REQUEST_RECEIVED("RequestReceived"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    CONNECTED("Connected"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            return null;
        }
    },
    ACTIVE("Active"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CANCELING_ACTIVE;
                case MESSAGE_COMPLETE:
                    return COMPLETING;
                case MESSAGE_EXIT:
                    return EXITING;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
                case MESSAGE_CANNOTCOMPLETE:
                    return NOT_COMPLETING;
                default:
                    return ACTIVE;
            }
        }
    },
    CANCELING_ACTIVE("CancelingActive"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CANCELING_ACTIVE;
                case MESSAGE_COMPLETE:
                    return CANCELING_ACTIVE;
                case MESSAGE_EXIT:
                    return EXITING;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
                case MESSAGE_CANCELED:
                    return ENDED;

                case MESSAGE_CANNOTCOMPLETE:
                    return NOT_COMPLETING;
                default:
                    return CANCELING_ACTIVE;
            }
        }
    },
    CANCELING_COMPLETING("CancelingActive"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CANCELING_COMPLETING;
                case MESSAGE_COMPLETE:
                    return CANCELING_COMPLETING;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
                case MESSAGE_CANCELED:
                    return ENDED;
                case MESSAGE_EXIT:
                    return EXITING;
                case MESSAGE_COMPLETED:
                    return COMPLETED;
                case MESSAGE_CANNOTCOMPLETE:
                    return NOT_COMPLETING;
                default:
                    return CANCELING_ACTIVE;
            }
        }
    },
    COMPLETING("Completing"){
      public BACCState transit(BACCEventType msg)
      {
          resendEvent = null;
          switch (msg) {
              case MESSAGE_CANCEL:
                  return CANCELING_COMPLETING;
              case MESSAGE_COMPLETE:
                  return COMPLETING;
              case MESSAGE_EXIT:
                  return EXITING;
              case MESSAGE_COMPLETED:
                  return COMPLETED;
              case MESSAGE_FAIL:
                  return FAILING_ACTIVE_CANCELING_COMPLETING;
              case MESSAGE_CANNOTCOMPLETE:
                  return NOT_COMPLETING;
              default:
                  return CANCELING_ACTIVE;
          }
      }
    },
    COMPLETED("Completed"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BACCEventType.MESSAGE_COMPLETED;
                    return COMPLETED;
                case MESSAGE_COMPLETE:
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
        public BACCState transit (BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return CLOSING;
                case MESSAGE_COMPLETE:
                    return CLOSING;
                case MESSAGE_CLOSED:
                    return ENDED;
                case MESSAGE_COMPLETED:
                    resendEvent = BACCEventType.MESSAGE_CLOSE;
                    return CLOSING;
                case MESSAGE_CLOSE:
                    return CLOSING;
                default:
                    return CLOSING;
            }
        }
    },
    COMPENSATING("Compensating"){
        public BACCState transit (BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return COMPENSATING;
                case MESSAGE_COMPLETE:
                    return COMPENSATING;
                case MESSAGE_COMPENSATE:
                    return COMPENSATING;
                case MESSAGE_FAIL:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPENSATED:
                    return ENDED;
                case MESSAGE_COMPLETED:
                    resendEvent = BACCEventType.MESSAGE_COMPENSATE;
                    return COMPENSATING;
                default:
                    return COMPENSATING;
            }
        }
    },
    FAILING_ACTIVE_CANCELING_COMPLETING("FailingActiveCancelingCompleting") {
        public BACCState transit (BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BACCEventType.MESSAGE_FAIL;
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
                case MESSAGE_COMPLETE:
                    resendEvent = BACCEventType.MESSAGE_FAIL;
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
                case MESSAGE_FAIL:
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
                default:
                    return FAILING_ACTIVE_CANCELING_COMPLETING;
            }
        }
    },
    FAILING_COMPENSATING("FailingCompensating") {
        public BACCState transit (BACCEventType msg) {
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPLETE:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPENSATE:
                    resendEvent = BACCEventType.MESSAGE_FAIL;
                    return FAILING_COMPENSATING;
                case MESSAGE_FAIL:
                    return FAILING_COMPENSATING;
                case MESSAGE_COMPLETED:
                    return FAILING_COMPENSATING;
                case MESSAGE_FAILED:
                    return ENDED;
                default:
                    return FAILING_COMPENSATING;
            }
        }
    },
    NOT_COMPLETING("NotCompleting"){
        public BACCState transit (BACCEventType msg) {
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BACCEventType.MESSAGE_CANNOTCOMPLETE;
                    return NOT_COMPLETING;
                case MESSAGE_COMPLETE:
                    resendEvent = BACCEventType.MESSAGE_CANNOTCOMPLETE;
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
        public BACCState transit (BACCEventType msg) {
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BACCEventType.MESSAGE_EXIT;
                    return EXITING;
                case MESSAGE_COMPLETE:
                    resendEvent = BACCEventType.MESSAGE_EXIT;
                case MESSAGE_EXIT:
                    return EXITING;
                case MESSAGE_EXITED:
                    return ENDED;
                default:
                    return EXITING;
            }
        }
    },
    ENDED("Ended") {
        public BACCState transit (BACCEventType msg){
            resendEvent = null;
            switch (msg) {
                case MESSAGE_CANCEL:
                    resendEvent = BACCEventType.MESSAGE_CANCELED;
                    return ENDED;
                case MESSAGE_COMPLETE:
                    resendEvent = BACCEventType.MESSAGE_FAIL;
                    return ENDED;
                case MESSAGE_CLOSE:
                    resendEvent = BACCEventType.MESSAGE_CLOSED;
                    return ENDED;
                case MESSAGE_COMPENSATE:
                    resendEvent = BACCEventType.MESSAGE_COMPENSATED;
                    return ENDED;
                case MESSAGE_FAILED:
                    return ENDED;
                case MESSAGE_EXITED:
                    return ENDED;
                case MESSAGE_NOTCOMPLETED:
                    return ENDED;
                case MESSAGE_EXIT:
                    resendEvent = BACCEventType.MESSAGE_EXITED;
                    return ENDED;
                case MESSAGE_CANCELED:
                    return ENDED;
                case MESSAGE_CLOSED:
                    return ENDED;
                case MESSAGE_COMPENSATED:
                    return ENDED;
                case MESSAGE_COMPLETED:
                    return ENDED;
                case MESSAGE_FAIL:
                    resendEvent = BACCEventType.MESSAGE_FAILED;
                    return ENDED;
                case MESSAGE_CANNOTCOMPLETE:
                    resendEvent = BACCEventType.MESSAGE_NOTCOMPLETED;
                    return ENDED;
                default:
                    return ENDED;
            }
        }
    },
    CLOSED("Closed"){
        public BACCState transit(BACCEventType msg){
            resendEvent = null;
            return null;
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(BACCState.class);

    private String name;
    private static BACCEventType resendEvent = null;

    private BACCState(String name)
    {
        this.name = name;
    }

    public BACCEventType getResendEvent(){
        return  resendEvent;
    }


    public static BACCState create(BACCEventType msg)
    {
        switch (msg) {
            case MESSAGE_CANCEL:
                return CANCELING_ACTIVE;
            case MESSAGE_COMPLETE:
                return COMPLETING;
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
                return FAILING_ACTIVE_CANCELING_COMPLETING;
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

    public abstract BACCState transit (BACCEventType msg);

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
    public static BACCState parseString(final String fragment)
    {
        for (BACCState state : values())
            if (state.name.equals(fragment))
                return state;

        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }



}


