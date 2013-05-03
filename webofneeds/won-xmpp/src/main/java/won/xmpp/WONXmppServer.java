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
import org.apache.vysper.xmpp.modules.extension.xep0054_vcardtemp.VcardTempModule;
import org.apache.vysper.xmpp.modules.extension.xep0077_inbandreg.InBandRegistrationModule;
import org.apache.vysper.xmpp.modules.extension.xep0092_software_version.SoftwareVersionModule;
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
import won.xmpp.core.ProxyRepository;
import won.xmpp.won.xmpp.component.WONXmppComponent;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * User: Ashkan
 * Date: 29.03.13
 */
public class WONXmppServer implements InitializingBean, DisposableBean{

    final Logger logger = LoggerFactory.getLogger(getClass());

    private String hostname = "sat.at";
    private XMPPServer server;

    private StorageProviderRegistry providerRegistry;

    final WONUserAuthentication accountManagement;

    private WONRoutingModule routingModule;

    public WONXmppServer(){

        accountManagement = new WONUserAuthentication();
        providerRegistry = new WONStorageProviderRegistry(accountManagement, new MemoryRosterManager());
        //accountManagement = (WONUserAuthentication) providerRegistry.retrieve(AccountManagement.class);
        server = new XMPPServer(hostname);
        server.addEndpoint(new C2SEndpoint());
        server.addEndpoint(new S2SEndpoint());
        server.setStorageProviderRegistry(providerRegistry);
        //configuring Modules
        //Change here for additional XMPP features
        server.addModule(new SoftwareVersionModule());
        server.addModule(new EntityTimeModule());
        //server.addModule(new VcardTempModule());

        server.addModule(new InBandRegistrationModule()); //under Development ??

        server.addModule(new WONXmppComponent(server.getServerRuntimeContext()));

        /*routing module*/
        routingModule = new WONRoutingModule();
        server.addModule(routingModule);
        //"won-xmpp/src/main/config/bogus_mina_tls.cert"
        server.setTLSCertificateInfo(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/config/bogus_mina_tls.cert"), "boguspw");


    }


    public void start(){

        try {
            server.start();
            //server.getServerRuntimeContext().registerComponent(new WONXmppComponent(server.getServerRuntimeContext()));
            //for TESTING purpose
            initializeUsers();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void stop(){

        server.stop();
    }


    public void registerNewNeed(Need need){
        try {
            Entity needEntity = EntityImpl.parse("need_"+need.getId()+"@"+hostname);
            logger.info(String.format("Jabber id %s created!", "need_" + need.getId() + "@" + hostname));
            if(!accountManagement.verifyAccountExists(needEntity)){
                accountManagement.addUser(needEntity, "password1");
            }
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (AccountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    public void deleteNeed(Need need){
        try {
            Entity needEntity = EntityImpl.parse("need_"+need.getId()+"@"+hostname);

            if(accountManagement.verifyAccountExists(needEntity)){
                accountManagement.removeUser(needEntity);
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

        String wonComp = "won."+hostname;
        String pw = "password1";
        String proxy1 = "proxy1@" + wonComp;
        String proxy2 = "proxy2@" +wonComp;
        Entity proxy1Entity = EntityImpl.parse(proxy1);
        Entity proxy2Entity = EntityImpl.parse(proxy2);


            accountManagement.addUser(adminEntity , pw);
            accountManagement.addUser(user1Entity, pw);
            accountManagement.addUser(EntityImpl.parse("won@" + wonComp), pw);
            accountManagement.addUser(EntityImpl.parse("user1@" + wonComp), pw);
            accountManagement.addUser(EntityImpl.parse("admin@" + wonComp), pw);
            accountManagement.addUser(proxy1Entity, pw);
            accountManagement.addUser(proxy2Entity, pw);

            routingModule.addProxy(proxy1, admin, proxy2);
            routingModule.addProxy(proxy2, user1, proxy1);

            routingModule.getProxy(proxy1).setStatus("I need a car");
            routingModule.getProxy(proxy2).setStatus("I have a car");

            for(SessionContext sc: server.getServerRuntimeContext().getResourceRegistry().getSessions(adminEntity)){

                RosterManagerUtils.getRosterInstance(server.getServerRuntimeContext(), sc).addContact(proxy1Entity
                        , new RosterItem(proxy1Entity, routingModule.getProxy(proxy1).getStatus(), SubscriptionType.TO , AskSubscriptionType.ASK_SUBSCRIBED) );

            }

            for(SessionContext sc: server.getServerRuntimeContext().getResourceRegistry().getSessions(user1Entity)){

                RosterManagerUtils.getRosterInstance(server.getServerRuntimeContext(), sc).addContact(proxy2Entity
                        , new RosterItem(proxy2Entity, routingModule.getProxy(proxy2).getStatus(), SubscriptionType.TO , AskSubscriptionType.ASK_SUBSCRIBED) );

            }

        } catch (AccountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (RosterException e) {
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
    }
}