package won.node.facet.impl;

//import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 14.1.14.
 * Time: 11.49
 * To change this template use File | Settings | File Templates.
 */
public enum BAEventType {
    //in general, be permissive about messages where possible. Don't care about duplicate messages

    //close may always be called. It always closes the connnection.
    ///
    PARTICIPANT_INBOUND_CANCEL("ParticipantInboundCancel", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING,  BAParticipantCompletionState.COMPLETED,
            BAParticipantCompletionState.CLOSING, BAParticipantCompletionState.COMPENSATING,
            BAParticipantCompletionState.FAILING_ACTIVE_CANCELING, BAParticipantCompletionState.FAILING_COMPENSATING,
            BAParticipantCompletionState.NOT_COMPLETING, BAParticipantCompletionState.EXITING,
            BAParticipantCompletionState.ENDED),

    PARTICIPANT_INBOUND_CLOSE("ParticipantInboundCancel", BAParticipantCompletionState.COMPLETED,
            BAParticipantCompletionState.CLOSING, BAParticipantCompletionState.ENDED),

    PARTICIPANT_INBOUND_COMPENSATE("ParticipantInboundCompensate", BAParticipantCompletionState.COMPLETED,
            BAParticipantCompletionState.COMPENSATING, BAParticipantCompletionState.FAILING_COMPENSATING,
            BAParticipantCompletionState.ENDED),

    PARTICIPANT_INBOUND_FAILED("ParticipantInboundFailed", BAParticipantCompletionState.FAILING_ACTIVE_CANCELING,
            BAParticipantCompletionState.FAILING_COMPENSATING, BAParticipantCompletionState.ENDED),

    PARTICIPANT_INBOUND_EXITED("ParticipantInboundExited", BAParticipantCompletionState.EXITING,
            BAParticipantCompletionState.ENDED),

    PARTICIPANT_INBOUND_NOT_COMPLETED("ParticipantInboundNotCompleted", BAParticipantCompletionState.NOT_COMPLETING,
            BAParticipantCompletionState.ENDED),



    PARTICIPANT_OUTBOUND_EXIT("ParticipantOutboundExit", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.EXITING),

    PARTICIPANT_OUTBOUND_COMPLETED("ParticipantOutboundCompleted", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.COMPLETED),

    PARTICIPANT_OUTBOUND_FAIL("ParticipantOutboundFail", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING, BAParticipantCompletionState.COMPENSATING,
            BAParticipantCompletionState.FAILING_ACTIVE_CANCELING, BAParticipantCompletionState.FAILING_COMPENSATING),

    PARTICIPANT_OUTBOUND_CANNOT_COMPLETE("ParticipantOutboundCannotComplete", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.NOT_COMPLETING),

    PARTICIPANT_OUTBOUND_CANCELED("ParticipantOutboundCanceled", BAParticipantCompletionState.CANCELING,
            BAParticipantCompletionState.ENDED),

    PARTICIPANT_OUTBOUND_CLOSED("ParticipantOutboundClosed", BAParticipantCompletionState.CLOSING,
            BAParticipantCompletionState.ENDED),

    PARTICIPANT_OUTBOUND_COMPENSATED("ParticipantOutboundCompensated", BAParticipantCompletionState.COMPENSATING,
            BAParticipantCompletionState.ENDED),



    COORDINATOR_INBOUND_EXIT("CoordinatorInboundExit", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING, BAParticipantCompletionState.EXITING,
            BAParticipantCompletionState.ENDED),

    COORDINATOR_INBOUND_COMPLETED("CoordinatorInboundCompleted", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING, BAParticipantCompletionState.COMPLETED,
            BAParticipantCompletionState.CLOSING, BAParticipantCompletionState.COMPENSATING,
            BAParticipantCompletionState.FAILING_COMPENSATING, BAParticipantCompletionState.ENDED),

    COORDINATOR_INBOUND_FAIL("CoordinatorInboundFail", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING, BAParticipantCompletionState.COMPENSATING,
            BAParticipantCompletionState.FAILING_ACTIVE_CANCELING, BAParticipantCompletionState.FAILING_COMPENSATING,
            BAParticipantCompletionState.ENDED),

    COORDINATOR_INBOUND_CANNOT_COMPLETE("CoordinatorInboundCannotComplete", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING, BAParticipantCompletionState.NOT_COMPLETING,
            BAParticipantCompletionState.ENDED),

    COORDINATOR_INBOUND_CANCELED("CoordinatorInboundCanceled", BAParticipantCompletionState.CANCELING,
            BAParticipantCompletionState.ENDED),

    COORDINATOR_INBOUND_CLOSED("CoordinatorInboundClosed", BAParticipantCompletionState.CLOSING,
            BAParticipantCompletionState.ENDED),

    COORDINATOR_INBOUND_COMPENSATED("CoordinatorInboundCompensated", BAParticipantCompletionState.COMPENSATING,
            BAParticipantCompletionState.ENDED),



    COORDINATOR_OUTBOUND_CANCEL("CoordinatorOutboundCancel", BAParticipantCompletionState.ACTIVE,
            BAParticipantCompletionState.CANCELING),

    COORDINATOR_OUTBOUND_CLOSE("CoordinatorOutboundClose", BAParticipantCompletionState.COMPLETED,
            BAParticipantCompletionState.CLOSING),

    COORDINATOR_OUTBOUND_COMPENSATE("CoordinatorOutboundCompensate", BAParticipantCompletionState.COMPLETED,
            BAParticipantCompletionState.COMPENSATING),

    COORDINATOR_OUTBOUND_FAILED("CoordinatorOutboundFailed", BAParticipantCompletionState.FAILING_ACTIVE_CANCELING,
            BAParticipantCompletionState.FAILING_COMPENSATING, BAParticipantCompletionState.ENDED),

    COORDINATOR_OUTBOUND_EXITED("CoordinatorOutboundExited", BAParticipantCompletionState.EXITING,
            BAParticipantCompletionState.ENDED),

    COORDINATOR_OUTBOUND_NOT_COMPLETED("CoordinatorOutBoundNotCompleted", BAParticipantCompletionState.NOT_COMPLETING,
            BAParticipantCompletionState.ENDED);

    private String name;
    private BAParticipantCompletionState[] permittingStates;

    BAEventType(String name, BAParticipantCompletionState... permittingStates) {
        this.permittingStates = permittingStates;
        this.name = name;
    }

    public boolean isMessageAllowed(BAParticipantCompletionState stateToCheck){
        for (BAParticipantCompletionState st: this.permittingStates) {
            if (st.equals(stateToCheck)) return true;
        }
        return false;
    }

    public URI getURI() {
        return URI.create(WON_BA.BASE_URI + name);
    }

    public BAEventType getBAEventTypeFromURIParticipantInbound (String sURI)
    {
        for (BAEventType eventType: BAEventType.values()){
            if (sURI.equals(eventType.getURI().toString())) return eventType;
        }
        return null;
        /*if(sURI.toString().equals(WON_BA.MESSAGE_CANCEL.toString()))
            return BAEventType.PARTICIPANT_INBOUND_CANCEL;
        else if(sURI.toString().equals(BAEventType.PARTICIPANT_INBOUND_CLOSE.getURI().toString()))
            return BAEventType.PARTICIPANT_INBOUND_CLOSE;
        else if(sURI.toString().equals((BAEventType.PARTICIPANT_INBOUND_COMPENSATE.getURI().toString())))
            return BAEventType.PARTICIPANT_INBOUND_COMPENSATE;
        else return null;   */

        //TODO implement all!

    }
}
