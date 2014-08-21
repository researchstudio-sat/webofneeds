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
  CONNECTION_MESSAGE(WONMSG.TYPE_CONNECTION_MESSAGE);

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
