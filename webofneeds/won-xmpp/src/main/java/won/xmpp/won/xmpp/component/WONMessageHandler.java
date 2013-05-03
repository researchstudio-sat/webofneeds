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
import org.apache.vysper.xmpp.modules.core.base.handler.XMPPCoreStanzaHandler;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.stanza.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.xmpp.WONRoutingModule;
import won.xmpp.core.NeedProxy;

/**
 * User: Ashkan
 * Date: 19.04.13
 */
public class WONMessageHandler extends WONCoreStanzaHandler {
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected boolean verifyType(Stanza stanza) {
        return MessageStanza.isOfType(stanza);
    }

    @Override
    protected Stanza executeCore(XMPPCoreStanza stanza, ServerRuntimeContext serverRuntimeContext,
                                 boolean isOutboundStanza, SessionContext sessionContext) {

        logger.info("component recieved message\n");
        // (try to) read thread id
        String threadId = null;
        XMLElement threadElement = null;
        try {
            threadElement = stanza.getSingleInnerElementsNamed("thread");
            if (threadElement != null && threadElement.getSingleInnerText() != null) {
                try {
                    threadId = threadElement.getSingleInnerText().getText();
                } catch (Exception _) {
                    threadId = null;
                }
            }
        } catch (XMLSemanticError _) {
            threadId = null;
        }

        // (try to) read subject id
        String subject = null;
        XMLElement subjectElement = null;
        try {
            subjectElement = stanza.getSingleInnerElementsNamed("subject");
            if (subjectElement != null && subjectElement.getSingleInnerText() != null) {
                try {
                    subject = subjectElement.getSingleInnerText().getText();
                } catch (Exception _) {
                    subject = null;
                }
            }
        } catch (XMLSemanticError _) {
            subject = null;
        }

        // TODO inspect all BODY elements and make sure they conform to the spec


        //TODO think about what would you need (boolean)isOutboundStanza for
            // check if message reception is turned of either globally or locally
            if (!serverRuntimeContext.getServerFeatures().isRelayingMessages()
                    || (sessionContext != null && sessionContext
                    .getAttribute(SessionContext.SESSION_ATTRIBUTE_MESSAGE_STANZA_NO_RECEIVE) != null)) {
                logger.info("message reception is off\n");
                return null;
            }


        logger.info("preparing message: " + stanza.toString());

        try {

                XMPPCoreStanza resultStanza = XMPPCoreStanza.getWrapper( buildNextStanza(stanza, serverRuntimeContext, sessionContext) );

                logger.info("component built a message\n");


                return resultStanza;
            } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        //packet droped!
        return null;
    }

    @Override
    public String getName() {
        return  "WONMessageRouter";
    }


    @Override
    protected StanzaBuilder buildCoreStanza(Entity from, Entity to, Stanza oldStanza, ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext) throws XMLSemanticError {
        MessageStanza oldMsgStanza = new MessageStanza(oldStanza);
        StanzaBuilder stanzaBuilder = StanzaBuilder.createMessageStanza(from, to, oldMsgStanza.getXMLLang(), oldMsgStanza.getBody(oldMsgStanza.getXMLLang()));
        stanzaBuilder.addAttribute("type", "chat");

        WONRoutingModule router = serverRuntimeContext.getModule(WONRoutingModule.class);
        if(router == null){
            throw new IllegalStateException("router Module cannot be null");
        }
        NeedProxy fromProxy  = router.getProxy(from.getBareJID().toString());
        if(fromProxy == null ){
            throw new IllegalStateException("Stanza from invalid proxy: jid is " + from.getBareJID().toString() );
        }
        String nickname = fromProxy.getNickname();

        stanzaBuilder.addPreparedElement(new XMLElementBuilder("nick", "http://jabber.org/protocol/nick").addText(nickname).build());

        return stanzaBuilder;

    }
}
