package won.node.facet.businessactivity.participantcompletion;


import won.node.facet.businessactivity.participantcompletion.BAPCState;
import won.node.facet.businessactivity.participantcompletion.BAPCStateManager;

import java.net.URI;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 23.1.14.
 * Time: 17.15
 * To change this template use File | Settings | File Templates.
 */
public class SimpleBAPCStateManager implements BAPCStateManager {
    private HashMap<String, BAPCState> map = new HashMap();

    public BAPCState getStateForNeedUri(URI ownerUri, URI needUri){
        return map.get(ownerUri.toString()+ needUri.toString());
    }

    public void setupStateForNeedUri(URI needUri)
    {
       if (map.containsKey(needUri))
       {

       }
    }

    public void setStateForNeedUri(BAPCState state, URI ownerUri, URI needUri)
    {
       map.put(ownerUri.toString()+needUri.toString(), state);
    }

}
