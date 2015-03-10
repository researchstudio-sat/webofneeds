package won.protocol.message;

import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim
 * Date: 17.02.2015
 */
public enum WonEnvelopeType
{

  FROM_NODE(WONMSG.TYPE_FROM_NODE),
  FROM_OWNER(WONMSG.TYPE_FROM_OWNER),
  FROM_SYSTEM(WONMSG.TYPE_FROM_SYSTEM),
  FROM_EXTERNAL(WONMSG.TYPE_FROM_EXTERNAL);

  private Resource resource;

  private WonEnvelopeType(Resource resource)
  {
    this.resource = resource;
  }

  public Resource getResource()
  {
    return resource;
  }

  public static WonEnvelopeType getWonEnvelopeType(URI uri){
    return getWonEnvelopeType(WONMSG.toResource(uri));
  }


  public static WonEnvelopeType getWonEnvelopeType(Resource resource) {

    if (WONMSG.TYPE_FROM_OWNER.equals(resource))
      return FROM_OWNER;
    if (WONMSG.TYPE_FROM_NODE.equals(resource))
      return FROM_NODE;
    if (WONMSG.TYPE_FROM_SYSTEM.equals(resource))
      return FROM_SYSTEM;
    if (WONMSG.TYPE_FROM_EXTERNAL.equals(resource))
      return FROM_EXTERNAL;

    return null;
  }

}

