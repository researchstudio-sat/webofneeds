package won.matcher.protocol;

import com.hp.hpl.jena.rdf.model.Model;

import java.net.URI;

/**
 * User: LEIH-NB
 * Date: 08.04.14
 */
public interface MatcherProtocolMatcherService {
    public void onMatcherRegistration(URI wonNodeUri);
    public void onNewNeed(final URI wonNodeURI, URI needURI, Model content);
    public void onNeedActivated(final URI wonNodeURI, URI needURI);
    public void onNeedDeactivated(final URI wonNodeURI, URI needURI);

}
