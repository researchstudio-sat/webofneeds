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

import org.apache.vysper.xmpp.delivery.failure.DeliveryException;
import org.apache.vysper.xmpp.delivery.failure.IgnoreFailureStrategy;
import org.apache.vysper.xmpp.protocol.*;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.SessionContext;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;
import org.apache.vysper.xmpp.stanza.Stanza;
import org.apache.vysper.xmpp.stanza.XMPPCoreStanza;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: Ashkan
 * Date: 12.04.13
 */
public class WONXmppComponentStanzaProcessor extends ComponentStanzaProcessor {

    final Logger logger = LoggerFactory.getLogger(getClass());


    public WONXmppComponentStanzaProcessor(ServerRuntimeContext serverRuntimeContext) {
        super(serverRuntimeContext);
    }

    @Override
    public void processStanza(ServerRuntimeContext serverRuntimeContext, SessionContext sessionContext, Stanza stanza, SessionStateHolder sessionStateHolder) {
        if(stanza == null){
            throw new RuntimeException("cannot process NULL stanza");
        }

        XMPPCoreStanza xmppStanza = XMPPCoreStanza.getWrapper(stanza);
        if(xmppStanza == null){
            throw new RuntimeException("cannot process, only: IQ, message, or presence");
        }

        StanzaHandler stanzaHandler = componentStanzaHandlerLookup.getHandler(xmppStanza);

        if(stanzaHandler == null){
            logger.info("Unhandled stanza\n");
            unhandledStanza(stanza);
            return;
        }

        ResponseStanzaContainer responseStanzaContainer = null;
        try{
            responseStanzaContainer = stanzaHandler.execute(stanza, serverRuntimeContext, true, sessionContext,sessionStateHolder );
            logger.info("response: " + responseStanzaContainer.getResponseStanza().toString());
        } catch (ProtocolException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        if(responseStanzaContainer != null && responseStanzaContainer.getResponseStanza()!= null){
               Stanza responseStanza = responseStanzaContainer.getResponseStanza();

                logger.info("begin relaying\n");
                try {
                    IgnoreFailureStrategy failureStrategy = new IgnoreFailureStrategy();

                    serverRuntimeContext.getStanzaRelay().relay(responseStanza.getTo(), responseStanza, failureStrategy);
                } catch (DeliveryException e) {
                    throw new RuntimeException(e);
                }
        }else{

            logger.info("Stanza droped!");
        }
    }

    private void unhandledStanza(Stanza stanza){
        throw new RuntimeException("no handler for stanza");
    }
}
