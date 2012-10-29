import org.apache.vysper.mina.S2SEndpoint;
import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authorization.AccountCreationException;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.MUCModule;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Affiliation;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Conference;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.Room;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.model.RoomType;
import org.apache.vysper.xmpp.modules.extension.xep0045_muc.storage.OccupantStorageProvider;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;
import org.apache.vysper.xmpp.server.XMPPServer;
import won.server.need.Need;
import won.server.need.NeedState;
import won.server.ws.NeedTransaction;
import won.server.xmpp.XMPPServerService;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: ggrill
 * Date: 11.10.12
 * Time: 14:19
 * To change this template use File | Settings | File Templates.
 */

public class XmppServer implements XMPPServerService {
    private WONUserAuthorization accountManagement;
    private MemoryRosterManager rosterManagement;
    private Conference conf;

    private static final String CREDENTIALS_NAMESPACE = "vysper_internal_credentials";

    public static void main(String[] args) {
        new XmppServer().init();
    }

    public void init() {

        // choose the storage you want to use
        //StorageProviderRegistry providerRegistry = new JcrStorageProviderRegistry();
        accountManagement = new WONUserAuthorization();

        rosterManagement = new MemoryRosterManager();
        StorageProviderRegistry providerRegistry = new WONStorageProviderRegistry(accountManagement, rosterManagement);

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
        server.addEndpoint(new S2SEndpoint());
        server.setStorageProviderRegistry(providerRegistry);

        try {
            server.setTLSCertificateInfo(new File("needserver-xmpp/src/main/config/bogus_mina_tls.cert"), "boguspw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        try {
            server.start();

            conf = new Conference("name");
            conf.setRoomStorageProvider(new WONRoomStorageProvider());
            conf.setOccupantStorageProvider(new OccupantStorageProvider() {
                @Override
                public void initialize() {
                    //To change body of implemented methods use File | Settings | File Templates.
                }
            });
            server.addModule(new MUCModule("subdom", conf));
            //Server2Server communcation enable
            server.getServerRuntimeContext().getServerFeatures().setRelayingToFederationServers(true);

            System.out.println("server is running...");

            System.in.read();
            Need n1 = new Need() {
                @Override
                public String getID() {
                    return "car";
                }

                @Override
                public void setState(NeedState state) {
                }
            };
            registerNewNeed(n1);
            System.out.println("New Need: 'car' created...");
            System.in.read();
            Need n2 = new Need() {
                @Override
                public String getID() {
                    return "money";
                }

                @Override
                public void setState(NeedState state) {
                }
            };
            registerNewNeed(n2);
            rosterManagement.addContact(EntityImpl.parse("need_" +
                    n1.getID() + "@localhost"), new RosterItem(EntityImpl.parse("need_" +
                    n2.getID() + "@localhost"), SubscriptionType.BOTH));
            rosterManagement.addContact(EntityImpl.parse("need_" +
                    n2.getID() + "@localhost"), new RosterItem(EntityImpl.parse("need_" +
                    n1.getID() + "@localhost"), SubscriptionType.BOTH));
            System.out.println("New Need: 'money' created...");
            System.in.read();
            NeedTransaction t = new NeedTransaction() {

                @Override
                public int getContextID() {
                    return 0;
                }
            };

            startGroupChatSession(t, n1, n2);
            System.out.println("GroupChat started...");
            System.in.read();
            endGroupChatSession(t);
            System.out.println("GroupChat deleted...");
            System.in.read();
            deleteNeed(n1);
            deleteNeed(n2);
            System.out.println("Needs deleted");
            System.in.read();
            server.stop();
            System.out.println("Server stop");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerNewNeed(Need need) {
        try {
            if(!accountManagement.verifyAccountExists(EntityImpl.parse("need_" +
                    need.getID() + "@localhost"))) {
                accountManagement.addUser(EntityImpl.parse("need_" +
                        need.getID() + "@localhost"), "password1");
            }
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void deleteNeed(Need need) {
        try {
            if(accountManagement.verifyAccountExists(EntityImpl.parse("need_" +
                    need.getID() + "@localhost"))) {
                accountManagement.removeUser(EntityImpl.parse("need_" +
                        need.getID() + "@localhost"));
            }
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    //TODO: Ich brauche Referenzen auf die JabberID(UserID + Serverdomain) der betroffenen Needs
    public void startGroupChatSession(NeedTransaction transaction, Need n1,  Need n2) {
        try {
            Room r = conf.createRoom(EntityImpl.parse(transaction.getContextID()+"room"), "need",RoomType.MembersOnly);

            r.getAffiliations().add(EntityImpl.parse("need_" +
                    n1.getID() + "@localhost"), Affiliation.Member);
            r.addOccupant(EntityImpl.parse("need_" +
                    n1.getID() + "@localhost"), "Trader1");

            r.getAffiliations().add(EntityImpl.parse("need_" +
                    n2.getID() + "@localhost"), Affiliation.Member);
            r.addOccupant(EntityImpl.parse("need_" +
                    n2.getID() + "@localhost"), "Trader2");
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void startGroupChatSession(NeedTransaction transaction) {
        try {
            Room r = conf.createRoom(EntityImpl.parse(transaction.getContextID()+"room"), "need",RoomType.MembersOnly);
            r.addOccupant(EntityImpl.parse(""), "Trader");
            r.addOccupant(EntityImpl.parse(""), "Trader");
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void endGroupChatSession(NeedTransaction transaction) {
        try {
            conf.deleteRoom(EntityImpl.parse(transaction.getContextID()+"room"));
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO: Brauche Referenzen auf die JabberID's(UserID + Serverdomain) der betroffenen Needs
    @Override
    public void setupPrivateChannels(NeedTransaction transaction) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void endPrivateChannels(NeedTransaction transaction) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
