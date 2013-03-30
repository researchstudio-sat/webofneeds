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
import org.apache.vysper.xmpp.modules.roster.persistence.MemoryRosterManager;
import org.apache.vysper.xmpp.server.XMPPServer;
import won.protocol.model.Need;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * User: Ashkan
 * Date: 29.03.13
 */
public class WONXmppServer{



    private String hostname = "sat.at";
    private XMPPServer server;

    StorageProviderRegistry providerRegistry;

    final WONUserAuthentication accountManagement;

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
        server.addModule(new VcardTempModule());

        server.addModule(new InBandRegistrationModule()); //under Development ??

        try {
            server.setTLSCertificateInfo(new File("won-xmpp/src/main/config/bogus_mina_tls.cert"), "boguspw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //for TESTING purpose
        initializeUsers();
    }

    public void start(){

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void stop(){

        server.stop();
    }


    public void registerNewNeed(Need need){
        try {
            Entity needEntity = EntityImpl.parse("Need_"+need.getId()+"@"+hostname);

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
            Entity needEntity = EntityImpl.parse("Need_"+need.getId()+"@"+hostname);

            if(accountManagement.verifyAccountExists(needEntity)){
                accountManagement.removeUser(needEntity);
            }
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    private void initializeUsers(){

        try {
            accountManagement.addUser(EntityImpl.parse("admin@"+hostname) , "password1");
            accountManagement.addUser(EntityImpl.parse("user1@"+hostname), "password1");

        } catch (AccountCreationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (EntityFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

}