package won.protocol.message;

import org.apache.jena.rdf.model.Resource;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 13.08.2014
 */
public enum WonMessageType
{
  // main messages
  CREATE_NEED(WONMSG.TYPE_CREATE),
  CONNECT(WONMSG.TYPE_CONNECT),
  DEACTIVATE(WONMSG.TYPE_DEACTIVATE),
  ACTIVATE(WONMSG.TYPE_ACTIVATE),
  CLOSE(WONMSG.TYPE_CLOSE),
  DELETE(WONMSG.TYPE_DELETE),
  OPEN(WONMSG.TYPE_OPEN),
  CONNECTION_MESSAGE(WONMSG.TYPE_CONNECTION_MESSAGE),
  NEED_MESSAGE(WONMSG.TYPE_NEED_MESSAGE),
  HINT_MESSAGE(WONMSG.TYPE_HINT),
  HINT_FEEDBACK_MESSAGE(WONMSG.TYPE_HINT_FEEDBACK),

  // notification messages
  HINT_NOTIFICATION(WONMSG.TYPE_HINT_NOTIFICATION),
  NEED_CREATED_NOTIFICATION(WONMSG.TYPE_NEED_CREATED_NOTIFICATION),

  // response messages
  SUCCESS_RESPONSE(WONMSG.TYPE_SUCCESS_RESPONSE),
  FAILURE_RESPONSE(WONMSG.TYPE_FAILURE_RESPONSE);


  private Resource resource;

  private WonMessageType(Resource resource)
  {
    this.resource = resource;
  }

  public Resource getResource()
  {
    return resource;
  }

  public URI getURI(){
    return URI.create(getResource().getURI().toString());
  }

  public static WonMessageType getWonMessageType(URI uri){
    return getWonMessageType(WONMSG.toResource(uri));
  }

  public boolean isIdentifiedBy(URI uri){
    if (uri == null) return false;
    return getResource().getURI().toString().equals(uri.toString());
  }
  
  public boolean causesConnectionStateChange() {
	  return this == CLOSE || this == CONNECT || this == OPEN;
  }
  
  public boolean causesNeedStateChange() {
	  return this == ACTIVATE || this == DEACTIVATE;
  }

  public boolean causesNewConnection() {
	  return this == CONNECT || this == HINT_MESSAGE;
  }

  public static WonMessageType getWonMessageType(Resource resource) {

    if (WONMSG.TYPE_CREATE.equals(resource))
      return CREATE_NEED;
    if (WONMSG.TYPE_CONNECT.equals(resource))
      return CONNECT;
    if (WONMSG.TYPE_DEACTIVATE.equals(resource))
      return DEACTIVATE;
    if (WONMSG.TYPE_ACTIVATE.equals(resource))
      return ACTIVATE;
    if (WONMSG.TYPE_OPEN.equals(resource))
      return OPEN;
    if (WONMSG.TYPE_CLOSE.equals(resource))
      return CLOSE;
    if (WONMSG.TYPE_CONNECTION_MESSAGE.equals(resource))
      return CONNECTION_MESSAGE;
    if (WONMSG.TYPE_NEED_MESSAGE.equals(resource))
      return NEED_MESSAGE;
    if (WONMSG.TYPE_HINT.equals(resource))
      return HINT_MESSAGE;
    if (WONMSG.TYPE_HINT_FEEDBACK.equals(resource))
      return HINT_FEEDBACK_MESSAGE;

    // response classes
    if (WONMSG.TYPE_SUCCESS_RESPONSE.equals(resource))
      return SUCCESS_RESPONSE;
    if (WONMSG.TYPE_FAILURE_RESPONSE.equals(resource))
      return FAILURE_RESPONSE;


    //notification classes
    if (WONMSG.TYPE_HINT_NOTIFICATION.equals(resource))
      return HINT_NOTIFICATION;
    if (WONMSG.TYPE_NEED_CREATED_NOTIFICATION.equals(resource))
      return NEED_CREATED_NOTIFICATION;
    return null;
  }

}
