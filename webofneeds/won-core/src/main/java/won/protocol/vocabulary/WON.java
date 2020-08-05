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
package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import won.protocol.model.AtomState;
import won.protocol.model.ConnectionState;

/**
 * WoN Vocabulary
 */
public class WON {
    public static final String BASE_URI = "https://w3id.org/won/core#";
    public static final String DEFAULT_PREFIX = "won";
    private static Model m = ModelFactory.createDefaultModel();
    public static final Resource Atom = m.createResource(BASE_URI + "Atom");
    public static final Resource Persona = m.createResource(BASE_URI + "Persona");
    public static final Property wonNode = m.createProperty(BASE_URI, "wonNode");
    public static final Property defaultGraphSigningMethod = m.createProperty(BASE_URI, "defaultGraphSigningMethod");
    public static final Property nodeQueue = m.createProperty(BASE_URI, "nodeQueue");
    public static final Property ownerQueue = m.createProperty(BASE_URI, "ownerQueue");
    public static final Property matcherQueue = m.createProperty(BASE_URI, "matcherQueue");
    public static final Property atomCreatedTopic = m.createProperty(BASE_URI, "atomCreatedTopic");
    public static final Property atomActivatedTopic = m.createProperty(BASE_URI, "atomActivatedTopic");
    public static final Property atomDeactivatedTopic = m.createProperty(BASE_URI, "atomDeactivatedTopic");
    public static final Property atomDeletedTopic = m.createProperty(BASE_URI, "atomDeletedTopic");
    public static final Property uriPrefixSpecification = m.createProperty(BASE_URI, "uriPrefixSpecification");
    public static final Property atomUriPrefix = m.createProperty(BASE_URI, "atomUriPrefix");
    public static final Property connectionUriPrefix = m.createProperty(BASE_URI, "connectionUriPrefix");
    public static final Property messageUriPrefix = m.createProperty(BASE_URI, "messageUriPrefix");
    public static final Property atomList = m.createProperty(BASE_URI, "atomList");
    public static final Property supportsWonProtocolImpl = m.createProperty(BASE_URI + "supportsWonProtocolImpl");
    public static final Resource WonOverActiveMq = m.createResource(BASE_URI + "WonOverActiveMq");
    public static final Property brokerUri = m.createProperty(BASE_URI, "brokerUri");
    public static final Property atomState = m.createProperty(BASE_URI, "atomState");
    public static final Property contentGraph = m.createProperty(BASE_URI, "contentGraph");
    public static final Property derivedGraph = m.createProperty(BASE_URI, "derivedGraph");
    public static final Property socket = m.createProperty(BASE_URI, "socket");
    /**
     * @deprecated Default Socket is not used anymore, will be removed soon
     */
    @Deprecated
    public static final Property defaultSocket = m.createProperty(BASE_URI, "defaultSocket");
    public static final Resource Socket = m.createResource(BASE_URI + "Socket");
    public static final Property compatibleSocketDefinition = m.createProperty(BASE_URI + "compatibleSocketDefinition");
    public static final Property socketDefinition = m.createProperty(BASE_URI, "socketDefinition");
    public static final Property derivesAtomProperty = m.createProperty(BASE_URI + "derivesAtomProperty");
    public static final Property derivesInverseAtomProperty = m.createProperty(BASE_URI + "derivesInverseAtomProperty");
    public static final Property socketCapacity = m.createProperty(BASE_URI, "socketCapacity");
    public static final Property autoOpen = m.createProperty(BASE_URI, "autoOpen");
    // This property is used in the rdf-model part of connect (from owner) and hint
    // to specify a socket to which a connection is created
    public static final Property targetSocket = m.createProperty(BASE_URI + "targetSocket");
    public static final Property connections = m.createProperty(BASE_URI, "connections");
    public static final Resource ConnectionContainer = m.createResource(BASE_URI + "ConnectionContainer");
    public static final Resource Connection = m.createResource(BASE_URI + "Connection");
    public static final Property connectionState = m.createProperty(BASE_URI, "connectionState");
    public static final Property previousConnectionState = m.createProperty(BASE_URI, "previousConnectionState");
    public static final Property targetConnection = m.createProperty(BASE_URI, "targetConnection");
    public static final Property targetAtom = m.createProperty(BASE_URI, "targetAtom");
    public static final Property sourceAtom = m.createProperty(BASE_URI, "sourceAtom");
    public static final Property messageContainer = m.createProperty(BASE_URI, "messageContainer");
    public static final Resource MessageContainer = m.createResource(BASE_URI + "MessageContainer");
    public static final Property timeStamp = m.createProperty(BASE_URI, "timeStamp");
    public static final Property additionalData = m.createProperty(BASE_URI, "additionalData");
    public static final Property goal = m.createProperty(BASE_URI, "goal");
    public static final Property shapesGraph = m.createProperty(BASE_URI, "shapesGraph");
    public static final Property dataGraph = m.createProperty(BASE_URI, "dataGraph");
    public static final Property derivedForConnectionState = m.createProperty(BASE_URI + "derivedForConnectionState");
    // Resource individuals
    public static final Resource ATOM_STATE_ACTIVE = m.createResource(AtomState.ACTIVE.getURI().toString());
    public static final Resource ATOM_STATE_INACTIVE = m.createResource(AtomState.INACTIVE.getURI().toString());
    public static final Resource ATOM_STATE_DELETED = m.createResource(AtomState.DELETED.getURI().toString());
    public static final Resource CONNECTION_STATE_SUGGESTED = m
                    .createResource(ConnectionState.SUGGESTED.getURI().toString());
    public static final Resource CONNECTION_STATE_REQUEST_SENT = m
                    .createResource(ConnectionState.REQUEST_SENT.getURI().toString());
    public static final Resource CONNECTION_STATE_REQUEST_RECEIVED = m
                    .createResource(ConnectionState.REQUEST_RECEIVED.getURI().toString());
    public static final Resource CONNECTION_STATE_CONNECTED = m
                    .createResource(ConnectionState.CONNECTED.getURI().toString());
    public static final Resource CONNECTION_STATE_CLOSED = m.createResource(ConnectionState.CLOSED.getURI().toString());
    public static final Resource CONNECTION_STATE_DELETED = m
                    .createResource(ConnectionState.DELETED.getURI().toString());
    public static final Property suggestedCount = m.createProperty(BASE_URI, "suggestedCount");
    public static final Property requestReceivedCount = m.createProperty(BASE_URI, "requestReceivedCount");
    public static final Property requestSentCount = m.createProperty(BASE_URI, "requestSentCount");
    public static final Property connectedCount = m.createProperty(BASE_URI, "connectedCount");
    public static final Property closedCount = m.createProperty(BASE_URI, "closedCount");
    public static final Property deletedCount = m.createProperty(BASE_URI, "deletedCount");
    // unread information
    public static final Property unreadSuggested = m.createProperty(BASE_URI + "unreadSuggested");
    public static final Property unreadRequestSent = m.createProperty(BASE_URI + "unreadRequestSent");
    public static final Property unreadRequestReceived = m.createProperty(BASE_URI + "unreadRequestReceived");
    public static final Property unreadConnected = m.createProperty(BASE_URI + "unreadConnected");
    public static final Property unreadClosed = m.createProperty(BASE_URI + "unreadClosed");
    public static final Property unreadOldestTimestamp = m.createProperty(BASE_URI + "unreadOldestTimestamp");
    public static final Property unreadNewestTimestamp = m.createProperty(BASE_URI + "unreadNewestTimestamp");
    public static final Property unreadCount = m.createProperty(BASE_URI + "unreadCount");
    // crypt information
    public static final Resource ECCPublicKey = m.createResource(BASE_URI + "ECCPublicKey");
    public static final Property ecc_curveId = m.createProperty(BASE_URI, "ecc_curveId");
    public static final Property ecc_algorithm = m.createProperty(BASE_URI, "ecc_algorithm");
    public static final Property ecc_qx = m.createProperty(BASE_URI, "ecc_qx");
    public static final Property ecc_qy = m.createProperty(BASE_URI, "ecc_qy");
    public static final String CLIENT_CERTIFICATE_HEADER = "X-Client-Certificate";

    /**
     * Returns the base URI for this schema.
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }

    /**
     * Converts the AtomState Enum to a Resource.
     *
     * @param state
     * @return
     */
    public static Resource toResource(AtomState state) {
        switch (state) {
            case ACTIVE:
                return ATOM_STATE_ACTIVE;
            case INACTIVE:
                return ATOM_STATE_INACTIVE;
            case DELETED:
                return ATOM_STATE_DELETED;
            default:
                throw new IllegalArgumentException("No case specified for " + state.name());
        }
    }

    /**
     * Converts the ConnectionState Enum to a Resource.
     *
     * @param type
     * @return
     */
    public static Resource toResource(ConnectionState type) {
        switch (type) {
            case SUGGESTED:
                return CONNECTION_STATE_SUGGESTED;
            case REQUEST_SENT:
                return CONNECTION_STATE_REQUEST_SENT;
            case REQUEST_RECEIVED:
                return CONNECTION_STATE_REQUEST_RECEIVED;
            case CONNECTED:
                return CONNECTION_STATE_CONNECTED;
            case CLOSED:
                return CONNECTION_STATE_CLOSED;
            default:
                throw new IllegalArgumentException("No such case specified for " + type.name());
        }
    }
}
