package won.node.facet.businessactivity.participantcompletion;


import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import won.node.facet.impl.WON_TX;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 24.1.14.
 * Time: 16.08
 * To change this template use File | Settings | File Templates.
 */
public enum BAPCEventType {
    //in general, be permissive about messages where possible. Don't care about duplicate messages

    //close may always be called. It always closes the connection.
    ///
    MESSAGE_CANCEL("MessageCancel", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_CLOSE("MessageClose", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_COMPENSATE("MessageCompensate", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_FAILED("MessageFailed", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_EXITED("MessageExited", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_NOTCOMPLETED("MessageNotCompleted", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_EXIT("MessageExit", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_COMPLETED("MessageCompleted", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_FAIL("MessageFail", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_CANNOTCOMPLETE("MessageCanNotComplete", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_CANCELED("MessageCanceled", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_CLOSED("MessageClosed", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED))),

    MESSAGE_COMPENSATED("MessageCompensated", new ArrayList<BAPCState>(Arrays.asList(BAPCState.ACTIVE,
            BAPCState.CANCELING,  BAPCState.COMPLETED,
            BAPCState.CLOSING, BAPCState.COMPENSATING,
            BAPCState.FAILING_ACTIVE_CANCELING, BAPCState.FAILING_COMPENSATING,
            BAPCState.NOT_COMPLETING, BAPCState.EXITING,
            BAPCState.ENDED)));

    private String name;
    private ArrayList<BAPCState> permittingPStates;


    BAPCEventType(String name, ArrayList<BAPCState> permittingPStates) {
        this.permittingPStates = permittingPStates;
        this.name = name;
    }

    public boolean isMessageAllowed(BAPCState stateToCheck){
        if (this.permittingPStates.contains(stateToCheck))
            return true;
        else
            return false;
    }


    public URI getURI() {
        return URI.create(WON_TX.BASE_URI + name);
    }

    public BAPCEventType getBAEventTypeFromURIParticipantInbound (String sURI)
    {
        for (BAPCEventType eventType: BAPCEventType.values()){
            if (sURI.equals(eventType.getURI().toString())) return eventType;
        }
        return null;
    }


    public static BAPCEventType getBAEventTypeFromURI (String sURI)
    {
        for (BAPCEventType eventType: BAPCEventType.values()){
            if (sURI.equals(eventType.getURI().toString())) return eventType;
        }
        return null;
    }

    public static BAPCEventType getCoordinationEventTypeFromString(final String fragment)
    {
        for (BAPCEventType event : BAPCEventType.values())
            if (event.name().equals(fragment))
            {
                return event;
            }
        return null;
    }

    public static BAPCEventType getCoordinationEventTypeFromURI(final String fragment)
    {
        String s = fragment.substring(fragment.lastIndexOf("#Message")+8,fragment.length());
        for (BAPCEventType event : BAPCEventType.values())
            if (event.name().equals("MESSAGE_"+fragment.substring(fragment.lastIndexOf("#Message")+8,fragment.length()).toUpperCase()))
                return event;
        return null;
    }

    public static boolean isBAPCParticipantEventType(final BAPCEventType event)
    {
        boolean ret = false;
        if(event.equals(BAPCEventType.MESSAGE_COMPLETED) || event.equals(BAPCEventType.MESSAGE_EXIT)
                || event.equals(BAPCEventType.MESSAGE_FAIL) || event.equals(BAPCEventType.MESSAGE_CANNOTCOMPLETE)
                || event.equals(BAPCEventType.MESSAGE_CANCELED) || event.equals(BAPCEventType.MESSAGE_COMPENSATED)
                || event.equals(BAPCEventType.MESSAGE_CLOSED))
            ret=true;
        return ret;
    }

    public static boolean isBAPCCoordinatorEventType(final BAPCEventType event)
    {
        return !isBAPCParticipantEventType(event);
    }

}


