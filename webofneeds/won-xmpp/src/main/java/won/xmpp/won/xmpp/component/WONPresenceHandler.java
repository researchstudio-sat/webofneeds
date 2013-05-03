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

package won.xmpp.won.xmpp.component;

import org.apache.vysper.xml.fragment.XMLElement;
import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityFormatException;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.roster.RosterException;
import org.apache.vysper.xmpp.modules.roster.RosterItem;
import org.apache.vysper.xmpp.modules.roster.RosterSubscriptionMutator;
import org.apache.vysper.xmpp.modules.roster.SubscriptionType;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManager;
import org.apache.vysper.xmpp.modules.roster.persistence.RosterManagerUtils;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.xmpp.WONRoutingModule;
import won.xmpp.core.NeedProxy;

/**
 * User: Ashkan
 * Date: 26.04.13
 */
public class WONPresenceHandler extends WONCoreStanzaHandler {

    final Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    protected StanzaBuilder buildCoreStanza(Entity from, Entity to, Stanza oldStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) throws XMLSemanticError {

        PresenceStanza oldPresenceStanza= new PresenceStanza(oldStanza);

        WONRoutingModule router = serverRuntimeContext.getModule(WONRoutingModule.class);
        if(router == null){
            throw new IllegalStateException("router cannot be null");
        }
        NeedProxy fromProxy  = router.getProxy(from.getBareJID().toString());
        if(fromProxy == null ){
            throw new IllegalStateException("Stanza from invalid proxy: jid is " + from.getBareJID().toString() );
        }
        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(from, to, oldPresenceStanza.getXMLLang(), oldPresenceStanza.getPresenceType(),
                oldPresenceStanza.getShow(), oldPresenceStanza.getStatus(oldPresenceStanza.getXMLLang()));
        String status = fromProxy.getStatus() == null ? "" : fromProxy.getStatus();

        stanzaBuilder.addPreparedElement(new XMLElementBuilder("nick", "http://jabber.org/protocol/nick").addText(status).build());
        stanzaBuilder.addPreparedElement(new XMLElementBuilder("status").addText("Status: " + status).build());

        return stanzaBuilder;
    }

    @Override
    protected boolean verifyType(Stanza stanza) {
        return PresenceStanza.isOfType(stanza);
    }

    @Override
    protected Stanza executeCore(XMPPCoreStanza xmppCoreStanza, ServerRuntimeContext serverRuntimeContext, boolean b, SessionContext sessionContext) {

        WONRoutingModule router = serverRuntimeContext.getModule(WONRoutingModule.class);

        PresenceStanza stanza = (PresenceStanza)PresenceStanza.getWrapper(xmppCoreStanza);

        logger.info("component received presense stanza!");

        if(PresenceStanzaType.isSubscriptionType(stanza.getPresenceType())){

            logger.info("subscription stanza");

            return executeSubscriptionPresence(stanza, serverRuntimeContext, sessionContext);

        }else{

            try {
                return PresenceStanza.getWrapper(buildNextStanza(stanza, serverRuntimeContext, sessionContext));//presense

            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }


        }

        return null;
    }

    @Override
    public String getName() {
        return "wonPresenceHandler";
    }

    private Stanza executeSubscriptionPresence(PresenceStanza stanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext){


        Entity from = stanza.getFrom();
        Entity to = stanza.getTo();
        if (from == null){
            throw new IllegalStateException("from cannot be null");
        }

        WONRoutingModule router = serverRuntimeContext.getModule(WONRoutingModule.class);
        if(router == null){
            throw new IllegalStateException("router module cannot be null");
        }

        if(!router.hasProxy(to.getBareJID().toString())){
            logger.info("Unknown proxy " + to.getBareJID().toString());
            return null;
        }

        NeedProxy proxy = router.getProxy(to.getBareJID().toString());
        String ownerJid = proxy.getOwner().getJid();

        if(!ownerJid.equals(from.getBareJID().toString())){
            logger.info("Subscription from invalid owner");
            return null;
        }

        RosterManager rosterManager = RosterManagerUtils.getRosterInstance(serverRuntimeContext, sessionContext);

        RosterItem rosterItem = null;
        try {
            rosterItem = rosterManager.getContact(from.getBareJID(), to.getBareJID());
        } catch (RosterException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if(rosterItem == null){
            rosterItem = new RosterItem(to.getBareJID(), SubscriptionType.NONE );
        }

        RosterSubscriptionMutator.Result result = RosterSubscriptionMutator.getInstance().add(rosterItem, SubscriptionType.TO);

        try {
            rosterManager.addContact(from.getBareJID(), rosterItem);
        } catch (RosterException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if(result != RosterSubscriptionMutator.Result.OK){
            logger.info("subscription failed");
            return  null;

        }



        StanzaBuilder stanzaBuilder = StanzaBuilder.createPresenceStanza(to.getBareJID(), from.getBareJID(), stanza.getXMLLang(), PresenceStanzaType.SUBSCRIBE, "available" , proxy.getStatus() );
        String status = proxy.getStatus() == null ? "" : proxy.getStatus();
        stanzaBuilder.addPreparedElement(new XMLElementBuilder("nick", "http://jabber.org/protocol/nick").addText(status).build());
        return stanzaBuilder.build();

    }
}
