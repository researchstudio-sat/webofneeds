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
package won.protocol.exception;

import java.net.URI;
import java.text.MessageFormat;

import won.protocol.model.ConnectionState;

/**
 * User: fkleedorfer Date: 02.11.12
 */
public class IllegalMessageForConnectionStateException extends WonProtocolException {
    /**
     * 
     */
    private static final long serialVersionUID = 7086516120895015908L;
    private String messageType;
    private ConnectionState connectionState;
    private URI connectionURI;

    public IllegalMessageForConnectionStateException(final URI connectionURI, final String messageType,
                    final ConnectionState connectionState) {
        super(MessageFormat.format(
                        "Message type {0} not allowed in connection state {2} (connection: {1}).",
                        messageType, safeToString(connectionURI), safeToString(connectionState)));
        this.messageType = messageType;
        this.connectionState = connectionState;
        this.connectionURI = connectionURI;
    }

    public URI getConnectionURI() {
        return connectionURI;
    }

    public String getMethodName() {
        return messageType;
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }
}
