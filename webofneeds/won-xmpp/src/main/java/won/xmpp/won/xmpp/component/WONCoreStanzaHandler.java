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

        String resource = null;

        subdomain = subdomain + "." + serverCtx.getServerEnitity().getDomain();
        logger.info("from subdomain: " + subdomain);
        Entity from = stanza.getFrom();

        //TODO: if resource is not set
        if (from == null || !from.isResourceSet()) {
            // rewrite stanza with new from
            resource = serverCtx.getResourceRegistry()
                    .getUniqueResourceForSession(sessionContext);
            if (resource == null)
                throw new IllegalStateException("could not determine unique resource");
            from = new EntityImpl(sessionContext.getInitiatingEntity(), resource);

        }

        resource = from.getResource();

        if(!subdomain.equals(stanza.getTo().getDomain())){
            logger.info("recipient from outside domain: " + stanza.getTo().getDomain());
            throw new IllegalStateException("Recipient address outside of component");

        }



       if (subdomain.equals(from.getDomain())){
            //msg comes from a proxy inside the component


            //msg goes to another Proxy
            String toBareJid = stanza.getTo().getBareJID().toString();

            if(!router.hasProxy(toBareJid)){
                logger.info("Target proxy cannot be found!");
                return null;
            }

            if(!router.getProxy(toBareJid).getOtherProxyJid().equals(from.getBareJID().toString())){
                logger.info(String.format("Invalid source proxy: %s , for destination proxy: %s ", from.getBareJID().toString(), toBareJid));
                return null;
            }
            // so we direct it to TO's owner
            String ownerJid = router.getProxy(toBareJid).getOwner().getJid();
            logger.info("owner Jid for " + stanza.getTo().getFullQualifiedName() + " : " + ownerJid);
            Entity owner = EntityImpl.parse(ownerJid);
            Entity newSrc = new EntityImpl(stanza.getTo(),resource);
            stanzaBuilder = buildCoreStanza(newSrc, owner, stanza, serverCtx, sessionContext);

            return stanzaBuilder.build();




        }else{

            //msg comes from outside of the component

            String toBareJid = stanza.getTo().getBareJID().toString();

            //check if we have the dest address
            if(!router.hasProxy(toBareJid)){
                logger.info("Target proxy cannot be found! : " + toBareJid);
                return null;
            }

            NeedProxy toProxy = router.getProxy(toBareJid);

            String destJid = null; //our new destination

            //check if msg comes from authorized owner
            if(stanza.getFrom().getBareJID().toString().equals(toProxy.getOwner().getJid())){
                destJid = toProxy.getOtherProxyJid();

            }else if(stanza.getFrom().getBareJID().toString().equals(toProxy.getOtherProxyJid())){

                //msg comes from another proxy from another server
                destJid = toProxy.getOwner().getJid();
                logger.info("owner Jid for " + stanza.getTo().getFullQualifiedName() + " : " + destJid);

            }else{
                logger.info("Unauthorized incoming stanza for proxy: "+ toBareJid);
                return null;
            }

            // now we make a new msg stanza from incoming toJid to destJid


            logger.info("Destination Jid for "+ stanza.getTo().getFullQualifiedName() + " : " + destJid);


            Entity destProxy = EntityImpl.parse(destJid);

           Entity newSrc = new EntityImpl(stanza.getTo(),resource);

           stanzaBuilder = buildCoreStanza(newSrc, destProxy, stanza, serverCtx, sessionContext);


            return stanzaBuilder.build();



        }


    }



    protected abstract StanzaBuilder buildCoreStanza(Entity from, Entity to, Stanza oldStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) throws XMLSemanticError;
}
