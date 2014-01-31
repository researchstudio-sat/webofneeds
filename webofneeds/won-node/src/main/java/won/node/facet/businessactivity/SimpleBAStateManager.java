package won.node.facet.businessactivity;


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
    private HashMap<String, BAParticipantCompletionState> map = new HashMap();

    public BAParticipantCompletionState getStateForNeedUri(URI ownerUri, URI needUri){
        return map.get(ownerUri.toString()+ needUri.toString());
    }

    public void setupStateForNeedUri(URI needUri)
    {
       if (map.containsKey(needUri))
       {

       }
    }

    public void setStateForNeedUri(BAParticipantCompletionState state, URI ownerUri, URI needUri)
    {
       map.put(ownerUri.toString()+needUri.toString(), state);
    }

}
