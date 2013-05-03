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

import org.apache.vysper.xml.fragment.XMLElementBuilder;
import org.apache.vysper.xml.fragment.XMLSemanticError;
import org.apache.vysper.xmpp.addressing.Entity;
import org.apache.vysper.xmpp.addressing.EntityImpl;
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.MessageStanza;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.StanzaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.xmpp.WONRoutingModule;
import won.xmpp.core.NeedProxy;

/**
 * User: Ashkan
 * Date: 26.04.13
 */
public abstract class WONCoreStanzaHandler extends XMPPCoreStanzaHandler {

    final Logger logger = LoggerFactory.getLogger(getClass());

    protected Stanza buildNextStanza(Stanza stanza, ServerRuntimeContext serverCtx, SessionContext sessionContext) throws Exception{
        //MessageStanza msgStanza = new MessageStanza(stanza);
        StanzaBuilder stanzaBuilder = null;
        WONRoutingModule router = serverCtx.getModule(WONRoutingModule.class);
        if(router == null){
            throw new Exception("RoutingModule is null");
        }
        String subdomain = serverCtx.getModule(WONXmppComponent.class).getSubdomain();

        if(subdomain == null)
            throw new Exception("Component subdomain cannot be null!");

        subdomain = subdomain + "." + serverCtx.getServerEnitity().getDomain();
        logger.info("from subdomain: " + subdomain);
        Entity from = stanza.getFrom();

        //TODO: if resource is not set
        /*if (from == null || !from.isResourceSet()) {
            // rewrite stanza with new from
            String resource = serverCtx.getResourceRegistry()
                    .getUniqueResourceForSession(sessionContext);
            if (resource == null)
                throw new IllegalStateException("could not determine unique resource");
            from = new EntityImpl(sessionContext.getInitiatingEntity(), resource);

        } */

        if (subdomain.equals(from.getDomain())){
            //msg comes from a proxy inside the component

            if(!subdomain.equals(stanza.getTo().getDomain())){
                throw new IllegalStateException("Recipient address outside of component");
            }
            //msg goes to another Proxy
            String toJid = stanza.getTo().getFullQualifiedName();

            if(!router.hasProxy(toJid)){
                logger.info("Target proxy cannot be found!");
                return null;
            }
            // so we direct it to TO's owner
            String ownerJid = router.getProxy(stanza.getTo().getFullQualifiedName()).getOwner().getJid();
            logger.info("owner Jid for " + stanza.getTo().getFullQualifiedName() + " : " + ownerJid);
            Entity owner = EntityImpl.parse(ownerJid);

            stanzaBuilder = buildCoreStanza(stanza.getTo(), owner, stanza, serverCtx, sessionContext);
            /* stanzaBuilder = StanzaBuilder.createMessageStanza(stanza.getTo(), owner, stanza.getXMLLang(),
                    stanza.getBody(stanza.getXMLLang()));
            stanzaBuilder.addAttribute("type", "chat");*/

            return stanzaBuilder.build();




        }else{

            //msg comes from outside of the component
            if(subdomain.equals(stanza.getTo().getDomain())){
                //msg goes to a Proxy inside the component
                String toJid = stanza.getTo().getFullQualifiedName();

                if(!router.hasProxy(toJid)){
                    logger.info("Target proxy cannot be found! : " + toJid);
                    return null;
                }

                NeedProxy toProxy = router.getProxy(toJid);

                //check if msg comes from authorized owner
                if(!stanza.getFrom().getBareJID().toString().equals(toProxy.getOwner().getJid())){
                    logger.info("Incoming Message to Proxy : "+ toProxy.getJid() + " from invalid owner! : " + stanza.getFrom().getBareJID());
                    return null;
                }

                // no we make a new msg stanza from OtherProxy to it's Owner

                String otherProxyJid = toProxy.getOtherProxyJid();

                if(!router.hasProxy(otherProxyJid)){
                    logger.debug("Target proxy cannot be found! : " + otherProxyJid);
                    return null;
                }
                logger.info("other proxy Jid for "+ stanza.getTo().getFullQualifiedName() + " : " + otherProxyJid);

                String otherOwnerJid = router.getProxy(otherProxyJid).getOwner().getJid();

                Entity otherProxy = EntityImpl.parse(otherProxyJid);
                Entity otherOwner = EntityImpl.parse(otherOwnerJid);

                /*stanzaBuilder = StanzaBuilder.createMessageStanza(otherProxy, otherOwner,stanza.getXMLLang(),
                        stanza.getBody(stanza.getXMLLang()));
                stanzaBuilder.addAttribute("type", "chat");

                String status = router.getProxy(otherProxyJid).getStatus() == null ? "" : router.getProxy(otherProxyJid).getStatus();

                stanzaBuilder.addPreparedElement(new XMLElementBuilder("nick", "http://jabber.org/protocol/nick").addText(status).build());
                */

                stanzaBuilder = buildCoreStanza(otherProxy, otherOwner, stanza, serverCtx, sessionContext);


                return stanzaBuilder.build();


            }
        }

        return null;
    }



    protected abstract StanzaBuilder buildCoreStanza(Entity from, Entity to, Stanza oldStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) throws XMLSemanticError;
}
