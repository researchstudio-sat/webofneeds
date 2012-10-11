import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.storage.jcr.JcrStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.authorization.AccountManagement;
import org.apache.vysper.xmpp.server.XMPPServer;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: ggrill
 * Date: 11.10.12
 * Time: 14:19
 * To change this template use File | Settings | File Templates.
 */

public class XmppServer {
    public static void main(String[] args) {
        // choose the storage you want to use
        //StorageProviderRegistry providerRegistry = new JcrStorageProviderRegistry();
        StorageProviderRegistry providerRegistry = new MemoryStorageProviderRegistry();

        final AccountManagement accountManagement = (AccountManagement) providerRegistry.retrieve(AccountManagement.class);

        try {
            if(!accountManagement.verifyAccountExists(EntityImpl.parse("user1@localhost"))) {
                accountManagement.addUser(EntityImpl.parse("user1@localhost"), "password1");
            }
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        XMPPServer server = new XMPPServer("localhost");
        server.addEndpoint(new TCPEndpoint());
        server.setStorageProviderRegistry(providerRegistry);

        try {
            server.setTLSCertificateInfo(new File("needserver-xmpp/src/main/config/bogus_mina_tls.cert"), "boguspw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            server.start();
            System.out.println("server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
