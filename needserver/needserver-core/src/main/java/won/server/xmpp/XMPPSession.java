package won.server.xmpp;

/**
 * Created with IntelliJ IDEA.
 * User: fsalcher
 * Date: 10.10.12
 * Time: 11:10
 * To change this template use File | Settings | File Templates.
 */
public interface XMPPSession {

    public int sendMessage(String message);

    public void setReceiveMessageCallback(XMPPReceiveMessageCB cb);

    public int closeSession();

}
