package won.bot.framework.eventbot.listener.baStateBots;

import java.net.URI;

/**
 * Created with IntelliJ IDEA. User: Danijel Date: 21.2.14. Time: 11.54 To change this template use File | Settings |
 * File Templates.
 */
public interface BACCStateManagerBot {
    public Object getStateForNeedUri(URI ownerUri, URI needUri);

    public void setStateForNeedUri(Object state, URI ownerUri, URI needURI);
}
