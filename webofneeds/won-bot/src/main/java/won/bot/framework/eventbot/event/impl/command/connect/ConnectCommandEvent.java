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

import won.bot.framework.eventbot.event.BaseAtomSpecificEvent;
import won.bot.framework.eventbot.event.TargetAtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;

/**
 * Instructs the bot to connect to the specified targetAtom on behalf of the
 * atom.
 */
public class ConnectCommandEvent extends BaseAtomSpecificEvent implements MessageCommandEvent, TargetAtomSpecificEvent {
    private URI targetAtomURI;
    private URI localSocket;
    private URI targetSocket;
    private String welcomeMessage;

    /**
     * @deprecated use
     * {@link ConnectCommandEvent#ConnectCommandEvent(URI, URI, String)} instead
     */
    @Deprecated
    public ConnectCommandEvent(URI atomURI, URI targetAtomURI, URI localSocket, URI targetSocket,
                    String welcomeMessage) {
        super(atomURI);
        Objects.requireNonNull(localSocket);
        Objects.requireNonNull(targetSocket);
        this.targetAtomURI = targetAtomURI;
        this.localSocket = localSocket;
        this.targetSocket = targetSocket;
        this.welcomeMessage = welcomeMessage;
    }

    public ConnectCommandEvent(URI localSocket, URI targetSocket,
                    String welcomeMessage) {
        super(WonMessageUtils.stripFragment(localSocket));
        Objects.requireNonNull(localSocket);
        Objects.requireNonNull(targetSocket);
        this.targetAtomURI = WonMessageUtils.stripFragment(targetSocket);
        this.localSocket = localSocket;
        this.targetSocket = targetSocket;
        this.welcomeMessage = welcomeMessage;
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CONNECT;
    }

    public URI getTargetAtomURI() {
        return targetAtomURI;
    }

    public URI getLocalSocket() {
        return localSocket;
    }

    public URI getTargetSocket() {
        return targetSocket;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }
}
