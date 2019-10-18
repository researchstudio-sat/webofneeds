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
package won.bot.framework.eventbot.event.impl.command.base;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandResultEvent;
import won.protocol.model.Connection;

/**
 * Base class for command results (failure and successes).
 */
public abstract class AbstractMessageCommandResultEvent extends BaseAtomAndConnectionSpecificEvent
                implements MessageCommandResultEvent {
    private MessageCommandEvent originalCommandEvent;
    private String message; // TODO: unused variable/parameter (msg is only used in one subclass an that has
                            // its own variable for a message)

    public AbstractMessageCommandResultEvent(MessageCommandEvent originalCommandEvent, Connection con) {
        this(originalCommandEvent, con, null);
    }

    public AbstractMessageCommandResultEvent(MessageCommandEvent originalCommandEvent, URI atomURI, URI targetAtomURI,
                    URI connectionURI) {
        this(originalCommandEvent, makeConnection(atomURI, targetAtomURI, connectionURI));
    }

    public AbstractMessageCommandResultEvent(MessageCommandEvent originalCommandEvent, URI atomURI, URI targetAtomURI,
                    URI connectionURI, String message) {
        this(originalCommandEvent, makeConnection(atomURI, targetAtomURI, connectionURI), message);
    }

    public AbstractMessageCommandResultEvent(MessageCommandEvent originalCommandEvent, Connection con, String message) {
        super(con);
        this.originalCommandEvent = originalCommandEvent;
        this.message = message;
    }

    @Override
    public MessageCommandEvent getOriginalCommandEvent() {
        return originalCommandEvent;
    }

    @Override
    public String getMessage() {
        return null;
    }

    protected static Connection makeConnection(URI atomURI, URI targetAtomURI, URI connectionURI) {
        Connection con = new Connection();
        con.setConnectionURI(connectionURI);
        con.setAtomURI(atomURI);
        con.setTargetAtomURI(targetAtomURI);
        return con;
    }
}
