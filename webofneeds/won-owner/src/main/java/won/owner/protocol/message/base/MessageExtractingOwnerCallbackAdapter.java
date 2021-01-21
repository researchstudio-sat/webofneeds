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
package won.owner.protocol.message.base;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.owner.protocol.message.OwnerCallback;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

/**
 * Simple implementation of the WonMessageHandlerAdapter that extracts the
 * information needed to create the Connection and Match objects directly from
 * the available message without using additional storage. This means that the
 * Connection's state and type properties are never available, as they are not
 * part of messages. Use with care.
 */
public class MessageExtractingOwnerCallbackAdapter extends OwnerCallbackAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private LinkedDataSource linkedDataSource;

    public MessageExtractingOwnerCallbackAdapter(OwnerCallback adaptee) {
        super(adaptee);
    }

    public MessageExtractingOwnerCallbackAdapter() {
    }

    @Override
    protected Connection makeConnection(WonMessage wonMessage) {
        return toConnection(wonMessage);
    }

    private URI getOurAtomFromIncomingMessage(WonMessage wonMessage) {
        if (wonMessage.getMessageTypeRequired().isSocketHintMessage()) {
            return WonMessageUtils.getRecipientAtomURIRequired(wonMessage);
        } else if (wonMessage.getMessageTypeRequired().isAtomHintMessage()) {
            return null;
        }
        if (wonMessage.isMessageWithBothResponses()) {
            // message with both responses is an incoming message from another atom.
            // the head message is out partner's, so we are the recipient
            return WonMessageUtils.getRecipientAtomURIRequired(wonMessage);
        } else if (wonMessage.isMessageWithResponse()) {
            // message with onlny one response is our node's response plus the echo
            // the head message is the message we sent, so we are the sender
            return WonMessageUtils.getSenderAtomURIRequired(wonMessage);
        } else if (wonMessage.isRemoteResponse()) {
            // only a remote response. we are the recipient
            return WonMessageUtils.getRecipientAtomURIRequired(wonMessage);
        }
        return null;
    }

    /**
     * Creates a connection object representing the connection that the wonMessage
     * is addressed at, if any. The resulting Connection object will not have a
     * state or type property set.
     *
     * @param wonMessage or null if the message is not directed at a connection
     */
    private Connection toConnection(WonMessage wonMessage) {
        Optional<URI> connectionUri = WonLinkedDataUtils.getConnectionURIForIncomingMessage(wonMessage,
                        linkedDataSource);
        if (connectionUri.isPresent()) {
            // fetch the rest of the connection data from the node and make a connection
            // object for use in events.
            try {
                Dataset ds = linkedDataSource
                                .getDataForResource(connectionUri.get(),
                                                getOurAtomFromIncomingMessage(wonMessage));
                return WonRdfUtils.ConnectionUtils.getConnection(ds, connectionUri.get());
            } catch (Exception e) {
                logger.debug("Error fetching connection data for {}", connectionUri, e);
            }
        }
        return null;
    }

    @Autowired(required = false)
    @Qualifier("default")
    public void setAdaptee(OwnerCallback adaptee) {
        super.setAdaptee(adaptee);
    }
}
