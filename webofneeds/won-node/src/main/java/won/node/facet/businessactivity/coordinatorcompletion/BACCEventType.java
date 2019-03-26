package won.node.facet.businessactivity.coordinatorcompletion;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

import won.node.facet.impl.WON_TX;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 14.2.14. Time: 13.47 To
 * change this template use File | Settings | File Templates.
 */
public enum BACCEventType {
  // in general, be permissive about messages where possible. Don't care about
  // duplicate messages

  // close may always be called. It always closes the connection.
  ///
  MESSAGE_CANCEL("MessageCancel",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_CLOSE("MessageClose",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_COMPENSATE("MessageCompensate",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_FAILED("MessageFailed",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_EXITED("MessageExited",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_NOTCOMPLETED("MessageNotCompleted",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_EXIT("MessageExit",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_COMPLETED("MessageCompleted",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_FAIL("MessageFail",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_CANNOTCOMPLETE("MessageCanNotComplete",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_CANCELED("MessageCanceled",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_CLOSED("MessageClosed",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_COMPENSATED("MessageCompensated",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED))),

  MESSAGE_COMPLETE("MessageComplete",
      new ArrayList<BACCState>(Arrays.asList(BACCState.ACTIVE, BACCState.CANCELING_ACTIVE,
          BACCState.CANCELING_COMPLETING, BACCState.COMPLETING, BACCState.COMPLETED, BACCState.CLOSING,
          BACCState.COMPENSATING, BACCState.FAILING_ACTIVE_CANCELING_COMPLETING, BACCState.FAILING_COMPENSATING,
          BACCState.NOT_COMPLETING, BACCState.EXITING, BACCState.ENDED)));

  private String name;
  private ArrayList<BACCState> permittingPStates;

  BACCEventType(String name, ArrayList<BACCState> permittingPStates) {
    this.permittingPStates = permittingPStates;
    this.name = name;
  }

  BACCEventType(String name) {
    this.name = name;
    permittingPStates = new ArrayList<BACCState>();
    permittingPStates.add(BACCState.ACTIVE);
    permittingPStates.add(BACCState.CANCELING_ACTIVE);
    permittingPStates.add(BACCState.CANCELING_COMPLETING);
    permittingPStates.add(BACCState.COMPLETING);
    permittingPStates.add(BACCState.COMPLETED);
    permittingPStates.add(BACCState.CLOSING);
    permittingPStates.add(BACCState.COMPENSATING);
    permittingPStates.add(BACCState.FAILING_ACTIVE_CANCELING_COMPLETING);
    permittingPStates.add(BACCState.FAILING_COMPENSATING);
    permittingPStates.add(BACCState.NOT_COMPLETING);
    permittingPStates.add(BACCState.EXITING);
    permittingPStates.add(BACCState.ENDED);
  }

  public boolean isMessageAllowed(BACCState stateToCheck) {
    if (this.permittingPStates.contains(stateToCheck))
      return true;
    else
      return false;
  }

  public URI getURI() {
    return URI.create(WON_TX.BASE_URI + name);
  }

  public BACCEventType getBAEventTypeFromURIParticipantInbound(String sURI) {
    for (BACCEventType eventType : BACCEventType.values()) {
      if (sURI.equals(eventType.getURI().toString()))
        return eventType;
    }
    return null;
  }

  public static BACCEventType getBAEventTypeFromURI(String sURI) {
    for (BACCEventType eventType : BACCEventType.values()) {
      if (sURI.equals(eventType.getURI().toString()))
        return eventType;
    }
    return null;
  }

  public static BACCEventType getCoordinationEventTypeFromString(final String fragment) {
    for (BACCEventType event : BACCEventType.values()) {
      if (event.name().equals(fragment)) {
        return event;
      }
    }
    return null;
  }

  public static BACCEventType getCoordinationEventTypeFromURI(final String fragment) {
    String s = fragment.substring(fragment.lastIndexOf("#Message") + 8, fragment.length());
    for (BACCEventType event : BACCEventType.values())
      if (event.name().equals(
          "MESSAGE_" + fragment.substring(fragment.lastIndexOf("#Message") + 8, fragment.length()).toUpperCase()))
        return event;
    return null;
  }

  public static boolean isBACCParticipantEventType(final BACCEventType event) {
    boolean ret = false;
    if (event.equals(BACCEventType.MESSAGE_COMPLETED) || event.equals(BACCEventType.MESSAGE_EXIT)
        || event.equals(BACCEventType.MESSAGE_FAIL) || event.equals(BACCEventType.MESSAGE_CANNOTCOMPLETE)
        || event.equals(BACCEventType.MESSAGE_CANCELED) || event.equals(BACCEventType.MESSAGE_COMPENSATED)
        || event.equals(BACCEventType.MESSAGE_CLOSED))
      ret = true;
    return ret;
  }

  public static boolean isBACCCoordinatorEventType(final BACCEventType event) {
    return !isBACCParticipantEventType(event);
  }
}
