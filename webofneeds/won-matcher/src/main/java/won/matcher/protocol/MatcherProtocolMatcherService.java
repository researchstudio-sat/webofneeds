package won.matcher.protocol;

import java.net.URI;

import org.apache.jena.query.Dataset;

/**
 * User: LEIH-NB Date: 08.04.14
 */
public interface MatcherProtocolMatcherService {
    public void onMatcherRegistration(URI wonNodeUri);

    public void onNewAtom(final URI wonNodeURI, URI atomURI, Dataset content);

    public void onAtomModified(final URI wonNodeURI, URI atomURI);

    public void onAtomActivated(final URI wonNodeURI, URI atomURI);

    public void onAtomDeactivated(final URI wonNodeURI, URI atomURI);
}
