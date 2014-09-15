package won.protocol.message;

import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko
 * Date: 13.08.2014
 */
public enum WonMessageType
{
  // main messages
  CREATE_NEED(WONMSG.TYPE_CREATE),
  CONNECT(WONMSG.TYPE_CONNECT),
  NEED_STATE(WONMSG.TYPE_NEED_STATE),
  CLOSE(WONMSG.TYPE_CLOSE),
  OPEN(WONMSG.TYPE_OPEN),
  CONNECTION_MESSAGE(WONMSG.TYPE_CONNECTION_MESSAGE),
  HINT_MESSAGE(WONMSG.TYPE_HINT),

  // notification messages
  HINT_NOTIFICATION(WONMSG.TYPE_HINT_NOTIFICATION),
  NEED_CREATED_NOTIFICATION(WONMSG.TYPE_NEED_CREATED_NOTIFICATION),

  // response messages
  CREATE_RESPONSE(WONMSG.TYPE_CREATE_RESPONSE),
  CONNECT_RESPONSE(WONMSG.TYPE_CONNECT_RESPONSE),
  NEED_STATE_RESPONSE(WONMSG.TYPE_NEED_STATE_RESPONSE),
  CLOSE_RESPONSE(WONMSG.TYPE_CLOSE_RESPONSE),
  OPEN_RESPONSE(WONMSG.TYPE_OPEN_RESPONSE),
  CONNECTION_MESSAGE_RESPONSE(WONMSG.TYPE_CONNECTION_MESSAGE_RESPONSE);


  private Resource resource;

  private WonMessageType(Resource resource)
  {
    this.resource = resource;
  }

  public Resource getResource()
  {
    return resource;
  }

  public static WonMessageType getWonMessageType(Resource resource) {

    if (WONMSG.TYPE_CREATE.equals(resource))
      return CREATE_NEED;
    if (WONMSG.TYPE_CONNECT.equals(resource))
      return CONNECT;
    if (WONMSG.TYPE_NEED_STATE.equals(resource))
      return NEED_STATE;
    if (WONMSG.TYPE_OPEN.equals(resource))
      return OPEN;
    if (WONMSG.TYPE_CLOSE.equals(resource))
      return CLOSE;
    if (WONMSG.TYPE_CONNECTION_MESSAGE.equals(resource))
      return CONNECTION_MESSAGE;
    if (WONMSG.TYPE_HINT.equals(resource))
      return HINT_MESSAGE;

    // response classes
    if (WONMSG.TYPE_CREATE_RESPONSE.equals(resource))
      return CREATE_RESPONSE;
    if (WONMSG.TYPE_CONNECT_RESPONSE.equals(resource))
      return CONNECT_RESPONSE;
    if (WONMSG.TYPE_NEED_STATE_RESPONSE.equals(resource))
      return NEED_STATE_RESPONSE;
    if (WONMSG.TYPE_CLOSE_RESPONSE.equals(resource))
      return CLOSE_RESPONSE;
    if (WONMSG.TYPE_OPEN_RESPONSE.equals(resource))
      return OPEN_RESPONSE;
    if (WONMSG.TYPE_CONNECTION_MESSAGE_RESPONSE.equals(resource))
      return CONNECT_RESPONSE;

    //notification classes
    if (WONMSG.TYPE_HINT_NOTIFICATION.equals(resource))
      return HINT_NOTIFICATION;
    if (WONMSG.TYPE_NEED_CREATED_NOTIFICATION.equals(resource))
      return NEED_CREATED_NOTIFICATION;
    return null;
  }

}
