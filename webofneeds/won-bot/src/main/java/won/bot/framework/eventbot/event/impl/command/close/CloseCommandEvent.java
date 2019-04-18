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
package won.bot.framework.eventbot.event.impl.command.close;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;

/**
 * Instructs the bot to close the specified connection behalf of the atom.
 */
public class CloseCommandEvent extends BaseAtomAndConnectionSpecificEvent implements MessageCommandEvent {
    private String closeMessage;

    public CloseCommandEvent(Connection con, String closeMessage) {
        super(con);
        this.closeMessage = closeMessage;
    }

    public CloseCommandEvent(Connection con) {
        this(con, "Hello!");
    }

    public CloseCommandEvent(URI atomURI, URI targetAtomURI, URI connectionURI, String closeMessage) {
        this(makeConnection(atomURI, targetAtomURI, connectionURI), closeMessage);
    }

    public CloseCommandEvent(URI atomURI, URI targetAtomURI, URI connectionURI) {
        this(atomURI, targetAtomURI, connectionURI, "Hello!");
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.OPEN;
    }

    public String getCloseMessage() {
        return closeMessage;
    }
}