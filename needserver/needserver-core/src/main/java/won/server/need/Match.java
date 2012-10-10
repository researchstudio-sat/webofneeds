package won.server.need;

import won.server.xmpp.XMPPSession;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 11:48
 * To change this template use File | Settings | File Templates.
 */
public interface Match {

    public void success();
    public void abort();
    public XMPPSession getXMPPSession();


}
