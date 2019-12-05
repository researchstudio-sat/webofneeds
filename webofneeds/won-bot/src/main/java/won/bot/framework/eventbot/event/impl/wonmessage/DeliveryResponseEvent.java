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
import java.util.Optional;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.ResponseEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.Connection;

/**
 * Event published whenever a WonMessage is received that indicates the failure
 * of a previous message.
 */
public class DeliveryResponseEvent extends BaseEvent implements ResponseEvent {
    private URI originalMessageURI;
    private WonMessage message;
    private URI senderAtomURI;
    private Optional<Connection> con;

    public DeliveryResponseEvent(URI originalMessageURI, WonMessage message, Optional<Connection> con) {
        assert originalMessageURI != null : "originalMessageURI must not be null!";
        assert message != null : "responseMessage must not be null!";
        this.originalMessageURI = originalMessageURI;
        this.message = message;
        this.senderAtomURI = WonMessageUtils.getSenderAtomURI(message).orElseThrow(() -> new IllegalArgumentException(
                        "Could not obtain sender atom URI for message " + message.getMessageURI()));
        this.con = con;
    }

    public URI getOriginalMessageURI() {
        return originalMessageURI;
    }

    public WonMessage getMessage() {
        return message;
    }

    public Optional<URI> getConnectionURI() {
        if (con.isPresent()) {
            return Optional.of(con.get().getConnectionURI());
        }
        return Optional.empty();
    }

    public Optional<URI> getSocketURI() {
        if (con.isPresent()) {
            return Optional.of(con.get().getSocketURI());
        }
        return Optional.empty();
    }

    public Optional<URI> getTargetSocketURI() {
        if (con.isPresent()) {
            return Optional.of(con.get().getTargetSocketURI());
        }
        return Optional.empty();
    }

    @Override
    public URI getTargetAtomURI() {
        return message.getSenderAtomURI();
    }

    @Override
    public URI getAtomURI() {
        return message.getRecipientAtomURI();
    }

    @Override
    public URI getSenderAtomURI() {
        return senderAtomURI;
    }
}
