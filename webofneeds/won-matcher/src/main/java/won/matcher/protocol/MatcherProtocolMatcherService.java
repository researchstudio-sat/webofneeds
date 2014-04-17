package won.matcher.protocol;

import com.hp.hpl.jena.rdf.model.Model;

import java.net.URI;

/**
 * User: LEIH-NB
 * Date: 08.04.14
 */
public interface MatcherProtocolMatcherService {
    public void onNewNeed(URI needURI, Model content);
    public void onNeedActivated(URI needURI);
    public void onNeedDeactivated(URI needURI);

}
