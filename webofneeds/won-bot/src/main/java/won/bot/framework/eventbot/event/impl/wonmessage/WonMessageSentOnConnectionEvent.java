/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.eventbot.event.impl.wonmessage;

import java.net.URI;

import won.bot.framework.eventbot.event.AtomSpecificEvent;
import won.bot.framework.eventbot.event.ConnectionSpecificEvent;
import won.bot.framework.eventbot.event.TargetAtomSpecificEvent;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;

/**
 * Created by fkleedorfer on 14.06.2016.
 */
public class WonMessageSentOnConnectionEvent extends WonMessageSentEvent
                implements ConnectionSpecificEvent, AtomSpecificEvent, TargetAtomSpecificEvent {
    public WonMessageSentOnConnectionEvent(final WonMessage message) {
        super(message);
    }

    @Override
    public URI getConnectionURI() {
        return getWonMessage().getSenderURI();
    }

    @Override
    public URI getAtomURI() {
        return getWonMessage().getSenderAtomURI();
    }

    @Override
    public URI getTargetAtomURI() {
        return getWonMessage().getRecipientAtomURI();
    }

    public Connection getCon() {
        Connection con = new Connection();
        con.setConnectionURI(getConnectionURI());
        con.setAtomURI(getAtomURI());
        con.setTargetAtomURI(getTargetAtomURI());
        return con;
    }
}
