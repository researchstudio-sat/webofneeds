package won.server.xmpp;

import won.server.need.Need;
import won.server.ws.NeedTransaction;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 11:08
 * To change this template use File | Settings | File Templates.
 */
public interface XMPPServerService {
    public void registerNewNeed(Need need);
    public void deleteNeed(Need need);

    /**
     * very experimental - final implementation depends on XMPP server API
     *
     * All XMPP users for the group chat can (hopefully be extracted from the NeedTransaction.
     * A handle to the XMPP session should be added to the transaction object.
     */
    public void startGroupChatSession(NeedTransaction transaction);

    public void endGroupChatSession(NeedTransaction transaction);

    public void setupPrivateChannels(NeedTransaction transaction);

    public void endPrivateChannels(NeedTransaction transaction);

}
