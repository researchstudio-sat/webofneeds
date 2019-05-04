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
package won.bot.framework.eventbot.event.impl.atomlifecycle;

import java.net.URI;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.protocol.model.SocketType;

/**
 *
 */
public class AtomCreatedEvent extends BaseAtomSpecificEvent {
    private final URI atomUriBeforeCreation;
    private final URI wonNodeUri;
    private final Dataset atomDataset;
    private final SocketType socketType;

    public AtomCreatedEvent(final URI atomURI, final URI wonNodeUri, final Dataset atomDataset,
                    final SocketType socketType, final URI atomUriBeforeCreation) {
        super(atomURI);
        this.wonNodeUri = wonNodeUri;
        this.atomDataset = atomDataset;
        this.socketType = socketType;
        this.atomUriBeforeCreation = atomUriBeforeCreation;
    }

    public AtomCreatedEvent(final URI atomURI, final URI wonNodeUri, final Dataset atomDataset,
                    final SocketType socketType) {
        this(atomURI, wonNodeUri, atomDataset, socketType, null);
    }

    public URI getWonNodeUri() {
        return wonNodeUri;
    }

    public Dataset getAtomDataset() {
        return atomDataset;
    }

    public URI getAtomUriBeforeCreation() {
        return atomUriBeforeCreation;
    }

    public SocketType getSocketType() {
        return socketType;
    }
}
