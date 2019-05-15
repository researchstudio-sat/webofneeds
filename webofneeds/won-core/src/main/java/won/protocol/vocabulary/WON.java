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

import won.protocol.model.ConnectionState;
import won.protocol.model.AtomState;

/**
 * WoN Vocabulary
 */
public class WON {
    public static final String BASE_URI = "https://w3id.org/won/core#";
    public static final String DEFAULT_PREFIX = "won";
    private static Model m = ModelFactory.createDefaultModel();
    public static final Resource Atom = m.createResource(BASE_URI + "Atom");
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
    public static final Property eventUriPrefix = m.createProperty(BASE_URI, "eventUriPrefix");
    public static final Property atomList = m.createProperty(BASE_URI, "atomList");
    public static final Property embedSpinAsk = m.createProperty(BASE_URI, "embedSpinAsk");
    public static final Property supportsWonProtocolImpl = m.createProperty(BASE_URI + "supportsWonProtocolImpl");
    public static final Resource WonOverActiveMq = m.createResource(BASE_URI + "WonOverActiveMq");
    public static final Property brokerUri = m.createProperty(BASE_URI, "brokerUri");
    public static final Resource WonOverSoapWs = m.createResource(BASE_URI + "WonOverSoapWs");
    public static final Property atomState = m.createProperty(BASE_URI, "atomState");
    public static final Property contentGraph = m.createProperty(BASE_URI, "contentGraph");
    public static final Property derivedGraph = m.createProperty(BASE_URI, "derivedGraph");
    public static final Property textMessage = m.createProperty(BASE_URI + "textMessage");
    public static final Property isProcessing = m.createProperty(BASE_URI + "isProcessing");
    public static final Resource Message = m.createResource(BASE_URI + "Message");
    public static final Property feedback = m.createProperty(BASE_URI, "feedback");
    public static final Property feedbackEvent = m.createProperty(BASE_URI, "feedbackEvent");
    // used to express which URI the feedback relates to
    public static final Property forResource = m.createProperty(BASE_URI, "forResource");
    public static final Property binaryRating = m.createProperty(BASE_URI, "binaryRating");
    public static final Resource Good = m.createResource(BASE_URI + "Good");
    public static final Resource Bad = m.createResource(BASE_URI + "Bad");
    public static final Property tag = m.createProperty(BASE_URI, "tag");
    public static final Property attachedMedia = m.createProperty(BASE_URI, "attachedMedia");
    public static final Property height = m.createProperty(BASE_URI, "height");
    public static final Property depth = m.createProperty(BASE_URI, "depth");
    public static final Property width = m.createProperty(BASE_URI, "width");
    public static final Property weight = m.createProperty(BASE_URI, "weight");
    public static final Property quantitativeProperty = m.createProperty(BASE_URI, "quantitativeProperty");
    public static final Property travelAction = m.createProperty("https://w3id.org/won/core#travelAction");
    public static final Property socket = m.createProperty(BASE_URI, "socket");
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
    public static final Property targetConnection = m.createProperty(BASE_URI, "targetConnection");
    public static final Property targetAtom = m.createProperty(BASE_URI, "targetAtom");
    public static final Property sourceAtom = m.createProperty(BASE_URI, "sourceAtom");
    public static final Property messageContainer = m.createProperty(BASE_URI, "messageContainer");
    public static final Resource MessageContainer = m.createResource(BASE_URI + "MessageContainer");
    public static final Property timeStamp = m.createProperty(BASE_URI, "timeStamp");
    public static final Property originator = m.createProperty(BASE_URI, "originator");
    public static final Property additionalData = m.createProperty(BASE_URI, "additionalData");
    public static final Resource AdditionalDataContainer = m.createResource(BASE_URI + "AdditionalDataContainer");
    public static final Property matchScore = m.createProperty(BASE_URI, "matchScore");
    public static final Property matchCounterpart = m.createProperty(BASE_URI, "matchCounterpart");
    public static final Property seeks = m.createProperty(BASE_URI, "seeks");
    public static final Property goal = m.createProperty(BASE_URI, "goal");
    public static final Property shapesGraph = m.createProperty(BASE_URI, "shapesGraph");
    public static final Property dataGraph = m.createProperty(BASE_URI, "dataGraph");
    public static final Property atomModality = m.createProperty(BASE_URI, "atomModality");
    public static final Resource AtomModality = m.createResource(BASE_URI + "AtomModality");
    public static final Property priceSpecification = m.createProperty(BASE_URI, "priceSpecification");
    public static final Resource PriceSpecification = m.createResource(BASE_URI + "PriceSpecification");
    public static final Property lowerPriceLimit = m.createProperty(BASE_URI, "lowerPriceLimit");
    public static final Property upperPriceLimit = m.createProperty(BASE_URI, "upperPriceLimit");
    public static final Property currency = m.createProperty(BASE_URI, "currency");
    /**
     * RDF-Property for location
     *
     * @deprecated Only use this to parse from existing content, create new content
     * by using {@link won.protocol.vocabulary.SCHEMA#LOCATION} instead
     */
    public static final Property location = m.createProperty(BASE_URI, "location");
    public static final Property boundingBox = m.createProperty(BASE_URI, "boundingBox");
    public static final Property northWestCorner = m.createProperty(BASE_URI, "northWestCorner");
    public static final Property southEastCorner = m.createProperty(BASE_URI, "southEastCorner");
    public static final Property boundsNorthWest = m.createProperty(BASE_URI, "boundsNorthWest");
    public static final Property boundsSouthEast = m.createProperty(BASE_URI, "boundsSouthEast");
    public static final Resource Location = m.createResource(BASE_URI + "Location");
    public static final Property geoSpatial = m.createProperty(BASE_URI + "geoSpatial");
    public static final Property isConcealed = m.createProperty(BASE_URI, "isConcealed");
    public static final Resource Region = m.createResource(BASE_URI + "Region");
    public static final Property iSOCode = m.createProperty(BASE_URI, "iSOCode");
    public static final Property timeSpecification = m.createProperty(BASE_URI, "timeSpecification");
    public static final Resource TimeSpecification = m.createResource(BASE_URI + "TimeSpecification");
    public static final Property startTime = m.createProperty(BASE_URI, "startTime");
    public static final Property endTime = m.createProperty(BASE_URI, "endTime");
    public static final Property recursIn = m.createProperty(BASE_URI, "recursIn");
    public static final Property recursTimes = m.createProperty(BASE_URI, "recursTimes");
    public static final Property recurInfiniteTimes = m.createProperty(BASE_URI, "recurInfiniteTimes");
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
    // adds a flag to an atom
    public static final Property flag = m.createProperty(BASE_URI + "flag");
    public static final Property doNotMatchBefore = m.createProperty(BASE_URI + "doNotMatchBefore");
    public static final Property doNotMatchAfter = m.createProperty(BASE_URI + "doNotMatchAfter");
    // the usedForTesting flag: atom is not a real atom, only match with other atoms
    // flagged with usedForTesting
    public static final Resource UsedForTesting = m.createResource(BASE_URI + "UsedForTesting");
    public static final Resource WhatsAround = m.createResource(BASE_URI + "WhatsAround");
    public static final Resource WhatsNew = m.createResource(BASE_URI + "WhatsNew");
    // hint behaviour
    public static final Resource NoHintForCounterpart = m.createResource(BASE_URI + "NoHintForCounterpart");
    public static final Resource NoHintForMe = m.createResource(BASE_URI + "NoHintForMe");
    public static final Property matchingContext = m.createProperty(BASE_URI + "matchingContext");
    public static final Property sparqlQuery = m.createProperty(BASE_URI + "sparqlQuery");
    public static final Property graph = m.createProperty(BASE_URI, "graph");
    // search result model
    public static final Resource Match = m.createResource(BASE_URI + "Match");
    public static final Property uri = m.createProperty(BASE_URI, "uri");
    public static final Property preview = m.createProperty(BASE_URI, "preview");
    public static final String privateDataGraph = BASE_URI + "privateDataGraph";
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
