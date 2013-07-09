package won.xmpp;

/**
 * User: Ashkan
 * Date: 03.05.13
 */
public interface WONXmppServer {

    void start();
    void stop();

    /**
     *
     * @param ownerJid owner bare Jid e.g "user@domain.com"
     * @param ownProxyJid proxy jid that will be created on this server for the owner e.g "proxy1"
     * @param partnerProxyBareJid partner proxy bare jid e.g "proxy2@domain"
     * @param nickname
     * @throws XmppAcountCreationException
     */
    void createXmppConnectionProxy(String ownerJid, String ownProxyJid, String partnerProxyBareJid, String nickname)
            throws XmppAcountCreationException;
    void deleteXmppConnectionProxy(String proxyJid);
}
