/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.xmpp;

import org.apache.vysper.mina.C2SEndpoint;
import org.apache.vysper.mina.S2SEndpoint;
import org.apache.vysper.mina.TCPEndpoint;
import org.apache.vysper.storage.StorageProviderRegistry;
import org.apache.vysper.storage.inmemory.MemoryStorageProviderRegistry;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.authentication.AccountCreationException;
import org.apache.vysper.xmpp.authentication.AccountManagement;
import org.apache.vysper.xmpp.modules.core.bind.handler.BindIQHandler;
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0077_inbandreg.InBandRegistrationModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
import org.apache.vysper.xmpp.modules.extension.xep0199_xmppping.XmppPingModule;
import org.apache.vysper.xmpp.modules.extension.xep0202_entity_time.EntityTimeModule;
import org.apache.vysper.xmpp.modules.roster.AskSubscriptionType;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManagerUtils;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.XMPPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import won.protocol.model.Need;
import won.xmpp.core.NeedProxy;
import won.xmpp.core.ProxyRepository;
import won.xmpp.won.xmpp.component.WONXmppComponent;
import org.apache.vysper.xmpp.extension.xep0124.BoshEndpoint;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * User: Ashkan
 * Date: 29.03.13
 */
public class WONXmppServerImpl implements InitializingBean, DisposableBean, WONXmppServer{

    final Logger logger = LoggerFactory.getLogger(getClass());

    private String hostname = null;
    
    private String wonDomain = "won."+hostname;
    private XMPPServer server;

    private S2SEndpoint s2s;

    private StorageProviderRegistry providerRegistry;

    final WONUserAuthentication accountManagement;
    private WONRoutingModule routingModule;

    public WONXmppServerImpl(String hostname){

        this.hostname = hostname;
        this.wonDomain = "won." + hostname;
        logger.info("Creating xmpp server with hostname : "+ hostname);
        accountManagement = new WONUserAuthentication();
        providerRegistry = new WONStorageProviderRegistry(accountManagement, new MemoryRosterManager());
        //accountManagement = (WONUserAuthentication) providerRegistry.retrieve(AccountManagement.class);
        server = new XMPPServer(hostname);
        server.addEndpoint(new C2SEndpoint());
        s2s = new S2SEndpoint();
        server.addEndpoint(s2s);
        server.setStorageProviderRegistry(providerRegistry);
        //configuring Modules
        //Change here for additional XMPP features
        server.addModule(new SoftwareVersionModule());
        server.addModule(new EntityTimeModule());
        //server.addModule(new VcardTempModule());
        server.addModule(new XmppPingModule());
        server.addModule(new InBandRegistrationModule()); //under Development ??

        server.addModule(new WONXmppComponent(server.getServerRuntimeContext()));

        /*routing module*/
        routingModule = new WONRoutingModule();
        server.addModule(routingModule);
        //"won-xmpp/src/main/config/bogus_mina_tls.cert"
        server.setTLSCertificateInfo(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/config/bogus_mina_tls.cert"), "boguspw");

        //BOSH
        BoshEndpoint bosh = new BoshEndpoint();
        bosh.setPort(8081);
        //bosh.setContextPath("/xmpp");
        server.addEndpoint(bosh);
    }

    @Override
    public void start(){

        try {
            server.start();
            //server.getServerRuntimeContext().registerComponent(new WONXmppComponent(server.getServerRuntimeContext()));
            //for TESTING purpose
            server.getServerRuntimeContext().getServerFeatures().setRelayingToFederationServers(true);

            //initializeUsers();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
    @Override
    public void stop(){

        server.stop();
    }

    @Override
    public void createXmppConnectionProxy(String ownerBareJid, String ownProxyJid, String partnerProxyBareJid, String nickname)
    throws XmppAcountCreationException{
        try {
            String ownProxyBareJid = ownProxyJid + "@"+wonDomain;
            //String partnerProxyBareJid = partnerProxyJid + "@"+wonDomain;

            Entity ownerEntity = EntityImpl.parse(ownerBareJid);
            Entity ownProxyEntity = EntityImpl.parse(ownProxyBareJid);
            Entity partnerProxyEntity = EntityImpl.parse(partnerProxyBareJid);

            /*Owner should have been registered with a password*/
            if(!accountManagement.verifyAccountExists(ownerEntity)){
                throw new XmppAcountCreationException("Unknown Owner jid !");
            }

            /*
            if(!accountManagement.verifyAccountExists(partnerProxyEntity)){
                accountManagement.addUser(partnerProxyEntity, "password1");
            }*/

            if(accountManagement.verifyAccountExists(ownProxyEntity)){
                throw new XmppAcountCreationException("OwnProxy jid exists!");
            }

            accountManagement.addUser(ownProxyEntity, "password1");


            NeedProxy newProxy = routingModule.addProxy(ownProxyBareJid, ownerBareJid, partnerProxyBareJid);

            newProxy.setNickname(nickname);
            //TODO update roster of the owner

            logger.info(String.format("connectionProxy with jid: %s successfully created!", ownProxyBareJid));


        } catch (EntityFormatException e) {
            throw new XmppAcountCreationException(e);  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccountCreationException e) {
            throw new XmppAcountCreationException(e);  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    @Override
    public void deleteXmppConnectionProxy(String connectionJid){
        try {
            String connectionBareJid = connectionJid + "@" + wonDomain;
            Entity connectionProxyEntity = EntityImpl.parse(connectionBareJid);

            if(routingModule.hasProxy(connectionBareJid)){
                NeedProxy proxy = routingModule.removeProxy(connectionBareJid);
                accountManagement.removeUser(EntityImpl.parse(connectionBareJid));
            }

        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void initializeUsers(){

        try{
        String admin = "admin@"+hostname;
        Entity adminEntity = EntityImpl.parse(admin);
        String user1 =  "user1@"+hostname;
        Entity user1Entity = EntityImpl.parse(user1);

        String user001 = "user001@sat001";
        String user016 = "user016@sat016";

        Entity user001Entity = EntityImpl.parse(user001);
        Entity user016Entity = EntityImpl.parse(user016);

        String pw = "password1";
        String proxy1 = "proxy1@" + wonDomain;
        String proxy2 = "proxy2@" +wonDomain;
        //Entity proxy1Entity = EntityImpl.parse(proxy1);
        //Entity proxy2Entity = EntityImpl.parse(proxy2);


            accountManagement.addUser(adminEntity , pw);
            accountManagement.addUser(user1Entity, pw);

            if(hostname.equals("sat001")){
                accountManagement.addUser(user001Entity, pw);
                createXmppConnectionProxy(user001, "proxy001@won.sat001", "proxy016@won.sat016", "I am user001 and I need a house");

            }else if(hostname.equals("sat016")){
                accountManagement.addUser(user016Entity, pw);
                createXmppConnectionProxy(user016, "proxy016@won.sat016", "proxy001@won.sat001", "I am user016 and I have a house");

            }

            /*accountManagement.addUser(EntityImpl.parse("won@" + wonComp), pw);
            accountManagement.addUser(EntityImpl.parse("user1@" + wonComp), pw);
            accountManagement.addUser(EntityImpl.parse("admin@" + wonComp), pw);
            accountManagement.addUser(proxy1Entity, pw);
            accountManagement.addUser(proxy2Entity, pw);

            routingModule.addProxy(proxy1, admin, proxy2);
            routingModule.addProxy(proxy2, user1, proxy1);
            */

            createXmppConnectionProxy(admin, "proxy1", "proxy2"+"@"+wonDomain, "I am admin and need a car");
            createXmppConnectionProxy(user1, "proxy2", "proxy1"+"@"+wonDomain, "I am user1 and have a car");




            routingModule.getProxy(proxy1).setStatus("I need a car");
            routingModule.getProxy(proxy2).setStatus("I have a car");


            /*
            for(SessionContext sc: server.getServerRuntimeContext().getResourceRegistry().getSessions(adminEntity)){

                RosterManagerUtils.getRosterInstance(server.getServerRuntimeContext(), sc).addContact(proxy1Entity
                        , new RosterItem(proxy1Entity, routingModule.getProxy(proxy1).getStatus(), SubscriptionType.TO , AskSubscriptionType.ASK_SUBSCRIBED) );

            }

            for(SessionContext sc: server.getServerRuntimeContext().getResourceRegistry().getSessions(user1Entity)){

                RosterManagerUtils.getRosterInstance(server.getServerRuntimeContext(), sc).addContact(proxy2Entity
                        , new RosterItem(proxy2Entity, routingModule.getProxy(proxy2).getStatus(), SubscriptionType.TO , AskSubscriptionType.ASK_SUBSCRIBED) );

            }
            */
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (XmppAcountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }


    @Override
    public void afterPropertiesSet() throws Exception {
        this.start();
    }

    @Override
    public void destroy() throws Exception {
        logger.info("destroying server");
        server.stop();
        logger.info("server stopped!");
    }



    public void setConnectionProxyInitString(String connections){

        for(String connection : connections.split(";")){
            String connSplit[] = connection.split(":");
            try {
                logger.info("creating user "+ connSplit[0]);
                accountManagement.addUser(EntityImpl.parse(connSplit[0]), "password1");
                logger.info(String.format("creating connection proxy: %s : %s : %s : %s",connSplit[0], connSplit[1], connSplit[2], connSplit[3]  ));
                createXmppConnectionProxy(connSplit[0], connSplit[1], connSplit[2], connSplit[3]);
            } catch (XmppAcountCreationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (AccountCreationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (EntityFormatException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }
    }
}