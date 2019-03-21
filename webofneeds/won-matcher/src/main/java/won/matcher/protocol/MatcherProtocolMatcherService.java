package won.matcher.protocol;

import org.apache.jena.query.Dataset;

import java.net.URI;

/**
 * User: LEIH-NB Date: 08.04.14
 */
public interface MatcherProtocolMatcherService {
  public void onMatcherRegistration(URI wonNodeUri);

  public void onNewNeed(final URI wonNodeURI, URI needURI, Dataset content);

  public void onNeedActivated(final URI wonNodeURI, URI needURI);

  public void onNeedDeactivated(final URI wonNodeURI, URI needURI);

}
