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
package won.protocol.model;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.vocabulary.WON;

/**
 * User: fkleedorfer Date: 30.10.12
 */
public enum ConnectionState {
    SUGGESTED("Suggested"), REQUEST_SENT("RequestSent"), REQUEST_RECEIVED("RequestReceived"), CONNECTED("Connected"),
    CLOSED("Closed"), DELETED("Deleted");
    private static final Logger logger = LoggerFactory.getLogger(ConnectionState.class);
    private String name;

    private ConnectionState(String name) {
        this.name = name;
    }

    public static ConnectionState create(ConnectionEventType msg) {
        switch (msg) {
            case MATCHER_HINT:
                return SUGGESTED;
            case OWNER_OPEN:
                return REQUEST_SENT;
            case PARTNER_OPEN:
                return REQUEST_RECEIVED;
        }
        throw new IllegalArgumentException("Connection creation failed: Wrong ConnectionEventType");
    }

    public ConnectionState transit(ConnectionEventType msg) {
        switch (this) {
            case SUGGESTED:
                switch (msg) {
                    case OWNER_OPEN:
                        return REQUEST_SENT;
                    case PARTNER_OPEN:
                        return REQUEST_RECEIVED;
                    case OWNER_CLOSE:
                        return CLOSED;
                    case PARTNER_CLOSE:
                        return CLOSED;
                    default:
                        return this;
                }
            case REQUEST_SENT: // the owner has initiated the connection, the request was sent to the remote
                               // need
                switch (msg) {
                    case PARTNER_OPEN:
                        return CONNECTED; // the partner accepted
                    case OWNER_CLOSE:
                        return CLOSED;
                    case PARTNER_CLOSE:
                        return CLOSED;
                    default:
                        return this;
                }
            case REQUEST_RECEIVED: // a remote need has requested a connection
                switch (msg) {
                    case OWNER_OPEN:
                        return CONNECTED;
                    case OWNER_CLOSE:
                        return CLOSED;
                    case PARTNER_CLOSE:
                        return CLOSED;
                    default:
                        return this;
                }
            case CONNECTED: // the connection is established
                switch (msg) {
                    case PARTNER_CLOSE:
                        return CLOSED;
                    case OWNER_CLOSE:
                        return CLOSED;
                    default:
                        return this;
                }
            case CLOSED:
                switch (msg) {
                    case OWNER_OPEN:
                        return REQUEST_SENT; // reopen connection
                    case PARTNER_OPEN:
                        return REQUEST_RECEIVED;
                    default:
                        return this;
                }
            default:
                return this;
        }
    }

    public static boolean closeOnNeedDeactivate(ConnectionState state) {
        return state == CONNECTED || state == REQUEST_RECEIVED || state == REQUEST_SENT;
    }

    public URI getURI() {
        return URI.create(WON.BASE_URI + name);
    }

    /**
     * Tries to match the given string against all enum values.
     *
     * @param fragment string to match
     * @return matched enum, null otherwise
     */
    public static ConnectionState parseString(final String fragment) {
        for (ConnectionState state : values())
            if (state.name.equals(fragment))
                return state;
        logger.warn("No enum could be matched for: {}", fragment);
        return null;
    }

    /**
     * Tries to match the given URI against all enum values.
     *
     * @param uri URI to match
     * @return matched enum, null otherwise
     */
    public static ConnectionState fromURI(final URI uri) {
        for (ConnectionState state : values())
            if (state.getURI().equals(uri))
                return state;
        return null;
    }
}
