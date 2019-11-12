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

import java.net.URI;
import java.util.Optional;

import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import won.owner.protocol.message.OwnerCallback;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Simple implementation of the WonMessageHandlerAdapter that extracts the
 * information needed to create the Connection and Match objects directly from
 * the available message without using additional storage. This means that the
 * Connection's state and type properties are never available, as they are not
 * part of messages. Use with care.
 */
public class MessageExtractingOwnerCallbackAdapter extends OwnerCallbackAdapter {
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
            Dataset ds = linkedDataSource.getDataForResource(connectionUri.get());
            return WonRdfUtils.ConnectionUtils.getConnection(ds, connectionUri.get());
        }
        return null;
    }

    @Autowired(required = false)
    @Qualifier("default")
    public void setAdaptee(OwnerCallback adaptee) {
        super.setAdaptee(adaptee);
    }
}
