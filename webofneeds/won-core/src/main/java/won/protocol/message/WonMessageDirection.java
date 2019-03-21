package won.protocol.message;

import org.apache.jena.rdf.model.Resource;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * User: syim Date: 17.02.2015
 */
public enum WonMessageDirection {

  FROM_OWNER(WONMSG.TYPE_FROM_OWNER), FROM_SYSTEM(WONMSG.TYPE_FROM_SYSTEM), FROM_EXTERNAL(WONMSG.TYPE_FROM_EXTERNAL);

  private Resource resource;

  private WonMessageDirection(Resource resource) {
    this.resource = resource;
  }

  public Resource getResource() {
    return resource;
  }

  public static WonMessageDirection getWonMessageDirection(URI uri) {
    return getWonMessageDirection(WONMSG.toResource(uri));
  }

  public boolean isIdentifiedBy(URI uri) {
    if (uri == null)
      return false;
    return getResource().getURI().toString().equals(uri.toString());
  }

  public static WonMessageDirection getWonMessageDirection(Resource resource) {

    if (WONMSG.TYPE_FROM_OWNER.equals(resource))
      return FROM_OWNER;
    if (WONMSG.TYPE_FROM_SYSTEM.equals(resource))
      return FROM_SYSTEM;
    if (WONMSG.TYPE_FROM_EXTERNAL.equals(resource))
      return FROM_EXTERNAL;

    return null;
  }

}
