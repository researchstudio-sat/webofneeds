package node.actor;

import akka.camel.CamelMessage;
import akka.camel.javaapi.UntypedConsumerActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Camel actor represents the need consumer protocol to a won node.
 * It is used to listen at the WoN node for events about new created needs,
 * activated needs and deactivated needs. Depending on the endpoint it
 * might be used with different protocols (e.g. JMS over activemq).
 *
 * User: hfriedrich
 * Date: 28.04.2015
 */
public class NeedConsumerProtocolActor extends UntypedConsumerActor
{
  private static final String MSG_HEADER_METHODNAME = "methodName";
  private static final String MSG_HEADER_METHODNAME_NEEDCREATED = "needCreated";
  private static final String MSG_HEADER_METHODNAME_NEEDACTIVATED = "needActivated";
  private static final String MSG_HEADER_METHODNAME_NEEDDEACTIVATED = "needDeactivated";
  private static final String MSG_HEADER_WON_NODE_URI = "wonNodeURI";
  private static final String MSG_HEADER_NEED_URI = "needUri";
  private final String endpoint;
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public NeedConsumerProtocolActor(String endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  public String getEndpointUri() {
    return endpoint;
  }

  @Override
  public void onReceive(final Object message) throws Exception {

    if (message instanceof CamelMessage) {
      CamelMessage camelMsg = (CamelMessage) message;
      String needUri = (String) camelMsg.getHeaders().get(MSG_HEADER_NEED_URI);
      String wonNodeUri = (String) camelMsg.getHeaders().get(MSG_HEADER_WON_NODE_URI);
      if (needUri != null && wonNodeUri != null) {
        Object methodName = camelMsg.getHeaders().get(MSG_HEADER_METHODNAME);
        if (methodName != null) {
          log.debug("Received event '{}' for needUri '{}' and wonNeedUri '{}'", methodName, needUri, wonNodeUri);
          if (methodName.equals(MSG_HEADER_METHODNAME_NEEDCREATED)) {
            onNeedCreated(needUri, wonNodeUri);
            return;
          } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDACTIVATED)) {
            onNeedActivated(needUri, wonNodeUri);
            return;
          } else if (methodName.equals(MSG_HEADER_METHODNAME_NEEDDEACTIVATED)) {
            onNeedDeactivated(needUri, wonNodeUri);
            return;
          }
        }
      }
    } else {
      System.out.print("some other message");
    }
    unhandled(message);
  }

  private void onNeedCreated(String needUri, String wonNodeUri) {

  }

  private void onNeedActivated(String needUri, String wonNodeUri) {

  }

  private void onNeedDeactivated(String needUri, String wonNodeUri) {

  }



}
