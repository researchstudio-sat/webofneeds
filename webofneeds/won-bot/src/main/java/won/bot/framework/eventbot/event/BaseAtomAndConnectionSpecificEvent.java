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
package won.bot.framework.eventbot.event;

import java.net.URI;

import won.protocol.model.Connection;

/**
 *
 */
public abstract class BaseAtomAndConnectionSpecificEvent extends BaseEvent
                implements AtomSpecificEvent, ConnectionSpecificEvent, TargetAtomSpecificEvent {
    private final Connection con;

    public BaseAtomAndConnectionSpecificEvent(final Connection con) {
        this.con = con;
    }

    public Connection getCon() {
        return con;
    }

    @Override
    public URI getConnectionURI() {
        return con.getConnectionURI();
    }

    @Override
    public URI getAtomURI() {
        return con.getAtomURI();
    }

    @Override
    public URI getTargetAtomURI() {
        return con.getTargetAtomURI();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + "{" + "atomURI=" + getAtomURI()
                        + ", connectionURI=" + getConnectionURI() + '}';
    }

    protected static Connection makeConnection(URI atomURI, URI targetAtomURI, URI connectionURI) {
        Connection con = new Connection();
        con.setConnectionURI(connectionURI);
        con.setAtomURI(atomURI);
        con.setTargetAtomURI(targetAtomURI);
        return con;
    }
}
