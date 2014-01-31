package won.node.facet.businessactivity;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 23.1.14.
 * Time: 16.29
 * To change this template use File | Settings | File Templates.
 */
public interface BAStateManager {
    public BAParticipantCompletionState getStateForNeedUri(URI ownerUri, URI needUri);
    public void setupStateForNeedUri(URI needUri);
    public void setStateForNeedUri(BAParticipantCompletionState state, URI ownerUri, URI needURI);
}
