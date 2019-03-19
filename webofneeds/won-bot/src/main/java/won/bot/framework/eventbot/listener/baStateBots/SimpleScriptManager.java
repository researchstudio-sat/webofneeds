package won.bot.framework.eventbot.listener.baStateBots;

import java.net.URI;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 21.2.14. Time: 11.57 To change this template use File | Settings |
 * File Templates.
 */
public class SimpleScriptManager {
    private HashMap<String, BATestBotScript> map = new HashMap();

    public BATestBotScript getStateForNeedUri(URI ownerUri, URI needUri) {
        return map.get(ownerUri.toString() + needUri.toString());
    }

    public void setStateForNeedUri(BATestBotScript state, URI coordinatorUri, URI participantUri) {
        map.put(coordinatorUri.toString() + participantUri.toString(), state);
    }

}
