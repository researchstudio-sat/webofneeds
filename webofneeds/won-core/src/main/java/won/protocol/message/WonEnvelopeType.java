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
  NodeToOwner(WONMSG.TYPE_NODE2OWNER),
  OwnerToNode(WONMSG.TYPE_OWNER2NODE),
  SystemMsg(WONMSG.TYPE_SYSTEMMSG),
  NodeToNode(WONMSG.TYPE_NODE2NODE),
  MatcherToNode(WONMSG.TYPE_MATCHER2NODE),
  NodeToMatcher(WONMSG.TYPE_NODE2MATCHER);

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

    if (WONMSG.TYPE_OWNER2NODE.equals(resource))
      return OwnerToNode;
    if (WONMSG.TYPE_NODE2OWNER.equals(resource))
      return NodeToOwner;
    if (WONMSG.TYPE_NODE2NODE.equals(resource))
      return NodeToNode;
    if (WONMSG.TYPE_SYSTEMMSG.equals(resource))
      return SystemMsg;
    if (WONMSG.TYPE_MATCHER2NODE.equals(resource))
      return SystemMsg;
    if (WONMSG.TYPE_NODE2MATCHER.equals(resource))
      return SystemMsg;

    return null;
  }

}

