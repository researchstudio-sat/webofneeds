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
package won.bot.framework.eventbot.event.impl.command.connect;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.bot.framework.eventbot.event.TargetAtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;

/**
 * Instructs the bot to connect to the specified targetAtom on behalf of the
 * atom.
 */
public class ConnectCommandEvent extends BaseAtomSpecificEvent implements MessageCommandEvent, TargetAtomSpecificEvent {
    private URI targetAtomURI;
    private Optional<URI> localSocket = Optional.empty();
    private Optional<URI> targetSocket = Optional.empty();
    private String welcomeMessage;

    public ConnectCommandEvent(URI atomURI, URI targetAtomURI, URI localSocket, URI targetSocket,
                    String welcomeMessage) {
        super(atomURI);
        Objects.requireNonNull(localSocket);
        Objects.requireNonNull(targetSocket);
        this.targetAtomURI = targetAtomURI;
        this.localSocket = Optional.of(localSocket);
        this.targetSocket = Optional.of(targetSocket);
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectCommandEvent(URI atomURI, URI targetAtomURI, String welcomeMessage) {
        super(atomURI);
        this.targetAtomURI = targetAtomURI;
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectCommandEvent(URI atomURI, URI targetAtomURI) {
        this(atomURI, targetAtomURI, "Hello!");
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CONNECT;
    }

    public URI getTargetAtomURI() {
        return targetAtomURI;
    }

    public Optional<URI> getLocalSocket() {
        return localSocket;
    }

    public Optional<URI> getTargetSocket() {
        return targetSocket;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }
}