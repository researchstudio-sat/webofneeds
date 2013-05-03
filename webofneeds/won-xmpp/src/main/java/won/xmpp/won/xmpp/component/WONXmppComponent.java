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

import org.apache.vysper.xmpp.modules.DefaultModule;
import org.apache.vysper.xmpp.modules.Module;
import org.apache.vysper.xmpp.protocol.StanzaProcessor;
import org.apache.vysper.xmpp.server.ServerRuntimeContext;
import org.apache.vysper.xmpp.server.components.Component;
import org.apache.vysper.xmpp.server.components.ComponentStanzaProcessor;

/**
 * User: Ashkan
 * Date: 12.04.13
 */
public class WONXmppComponent extends DefaultModule implements Component{

    private ComponentStanzaProcessor stanzaProcessor;

    public WONXmppComponent(ServerRuntimeContext ctx) {
        this.stanzaProcessor = new WONXmppComponentStanzaProcessor(ctx);
        stanzaProcessor.addHandler(new WONMessageHandler());
        stanzaProcessor.addHandler(new WONPresenceHandler());

    }


    @Override
    public String getSubdomain() {
        return "won";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public StanzaProcessor getStanzaProcessor() {
        return stanzaProcessor;
    }

    @Override
    public String getName() {
        return "WONXmppComponent";  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getVersion() {
        return "0.1";  //To change body of implemented methods use File | Settings | File Templates.
    }
}
