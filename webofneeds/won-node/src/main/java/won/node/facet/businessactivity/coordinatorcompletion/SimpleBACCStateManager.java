package won.node.facet.businessactivity.coordinatorcompletion;

import java.net.URI;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 6.2.14.
 * Time: 16.06
 * To change this template use File | Settings | File Templates.
 */
public class SimpleBACCStateManager  implements  BACCStateManager{
    private HashMap<String, BACCState> map = new HashMap();

    public synchronized BACCState getStateForNeedUri(URI ownerUri, URI needUri){
        return map.get(ownerUri.toString()+ needUri.toString());
    }

    public void setupStateForNeedUri(URI needUri)
    {
        if (map.containsKey(needUri))
        {

        }
    }

    public synchronized void setStateForNeedUri(BACCState state, URI ownerUri, URI needUri)
    {
        map.put(ownerUri.toString()+needUri.toString(), state);
    }

}

