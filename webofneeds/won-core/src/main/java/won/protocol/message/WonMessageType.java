package won.protocol.message;

import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.vocabulary.WONMSG;

/**
 * User: ypanchenko
 * Date: 13.08.2014
 */
public enum WonMessageType
{
  CREATE_NEED(WONMSG.TYPE_CREATE),
  CONNECT(WONMSG.TYPE_CONNECT),
  NEED_STATE(WONMSG.TYPE_NEED_STATE),
  CLOSE(WONMSG.TYPE_CLOSE),
  OPEN(WONMSG.TYPE_OPEN),
  CONNECTION_MESSAGE(WONMSG.TYPE_CONNECTION_MESSAGE),

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

}
