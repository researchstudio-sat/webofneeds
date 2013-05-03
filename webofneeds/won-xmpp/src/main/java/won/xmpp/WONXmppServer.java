package won.xmpp;

/**
 * User: Ashkan
 * Date: 03.05.13
 */
public interface WONXmppServer {

    void start();
    void stop();
    void createXmppConnectionProxy(String ownerJid, String ownProxyJid, String partnerProxyJid, String nickname)
            throws XmppAcountCreationException;
    void deleteXmppConnectionProxy(String proxyJid);
}
