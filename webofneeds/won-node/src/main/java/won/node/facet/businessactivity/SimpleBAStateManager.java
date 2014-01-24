package won.node.facet.businessactivity;

import won.node.facet.impl.BAParticipantCompletionState;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 23.1.14.
 * Time: 17.15
 * To change this template use File | Settings | File Templates.
 */
public class SimpleBAStateManager  implements  BAStateManager{
    private HashMap<URI, BAParticipantCompletionState> map = new HashMap();

    public BAParticipantCompletionState getStateForNeedUri(URI needUri){
        return map.get(needUri);
    }

    public void setupStateForNeedUri(URI needUri)
    {
       if (map.containsKey(needUri))
       {

       }
    }

    public void setStateForNeedUri(BAParticipantCompletionState state, URI uri)
    {
       map.put(uri, state);
    }

}
