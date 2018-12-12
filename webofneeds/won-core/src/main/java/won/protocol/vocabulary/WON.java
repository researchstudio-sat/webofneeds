/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import won.protocol.model.ConnectionState;
import won.protocol.model.NeedState;

/**
 * WoN Vocabulary
 */
public class WON {
    public static final String BASE_URI = "http://purl.org/webofneeds/model#";

    private static Model m = ModelFactory.createDefaultModel();


    public static final Resource NEED = m.createResource(BASE_URI + "Need");
    public static final Property HAS_WON_NODE = m.createProperty(BASE_URI, "hasWonNode");
    public static final Property HAS_DEFAULT_GRAPH_SIGNING_METHOD = m.createProperty(BASE_URI,
            "hasDefaultGraphSigningMethod");
    public static final Property HAS_NEED_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "hasNeedProtocolEndpoint");
    public static final Property HAS_MATCHER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "hasMatcherProtocolEndpoint");
    public static final Property HAS_OWNER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "hasOwnerProtocolEndpoint");

    public static final Property HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME = m.createProperty(BASE_URI, "hasActiveMQNeedProtocolQueueName");
    public static final Property HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME = m.createProperty(BASE_URI, "hasActiveMQOwnerProtocolQueueName");
    public static final Property HAS_ACTIVEMQ_MATCHER_PROTOCOL_QUEUE_NAME = m.createProperty(BASE_URI, "hasActiveMQMatcherProtocolQueueName");
    public static final Property HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_CREATED_TOPIC_NAME = m.createProperty(BASE_URI, "hasActiveMQMatcherProtocolOutNeedCreatedTopicName");
    public static final Property HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED_TOPIC_NAME = m.createProperty(BASE_URI, "hasActiveMQMatcherProtocolOutNeedActivatedTopicName");
    public static final Property HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED_TOPIC_NAME = m.createProperty(BASE_URI, "hasActiveMQMatcherProtocolOutNeedDeactivatedTopicName");
    public static final Property HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DELETED_TOPIC_NAME = m.createProperty(BASE_URI, "hasActiveMQMatcherProtocolOutNeedDeletedTopicName");
    public static final Property HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_MATCHER_REGISTERED_TOPIC_NAME = m.createProperty
            (BASE_URI, "hasActiveMQMatcherProtocolOutMatcherRegisteredTopicName");
    public static final Property HAS_URI_PATTERN_SPECIFICATION = m.createProperty(BASE_URI, "hasUriPrefixSpecification");
    public static final Property HAS_NEED_URI_PREFIX = m.createProperty(BASE_URI, "hasNeedUriPrefix");
    public static final Property HAS_CONNECTION_URI_PREFIX = m.createProperty(BASE_URI, "hasConnectionUriPrefix");
    public static final Property HAS_EVENT_URI_PREFIX = m.createProperty(BASE_URI, "hasEventUriPrefix");

    public static final Property HAS_NEED_LIST = m.createProperty(BASE_URI, "hasNeedList");

    public static final Property EMBED_SPIN_ASK = m.createProperty(BASE_URI, "embedSpinAsk");

    public static final Property SUPPORTS_WON_PROTOCOL_IMPL = m.createProperty(BASE_URI + "supportsWonProtocolImpl");
    public static final Resource WON_OVER_ACTIVE_MQ = m.createResource(BASE_URI + "WonOverActiveMq");
    public static final Property HAS_BROKER_URI = m.createProperty(BASE_URI, "hasBrokerUri");
    public static final Resource WON_OVER_SOAP_WS = m.createResource(BASE_URI + "WonOverSoapWs");
    public static final Property IS_IN_STATE = m.createProperty(BASE_URI, "isInState");

    public static final Property HAS_CONTENT_GRAPH = m.createProperty(BASE_URI, "hasContentGraph");
    public static final Property HAS_DERIVED_GRAPH = m.createProperty(BASE_URI, "hasDerivedGraph");

    public static final Property HAS_TEXT_MESSAGE = m.createProperty(BASE_URI + "hasTextMessage");
    public static final Property IS_PROCESSING = m.createProperty(BASE_URI + "isProcessing");
    public static final Resource MESSAGE = m.createResource(BASE_URI + "Message");
    public static final Property HAS_FEEDBACK = m.createProperty(BASE_URI, "hasFeedback");
    public static final Property HAS_FEEDBACK_EVENT = m.createProperty(BASE_URI, "hasFeedbackEvent");
    //used to express which URI the feedback relates to
    public static final Property FOR_RESOURCE = m.createProperty(BASE_URI, "forResource");
    public static final Property HAS_BINARY_RATING = m.createProperty(BASE_URI, "hasBinaryRating");
    public static final Resource GOOD = m.createResource(BASE_URI + "Good");
    public static final Resource BAD = m.createResource(BASE_URI + "Bad");

    public static final Property HAS_TAG = m.createProperty(BASE_URI, "hasTag");
    public static final Property HAS_ATTACHED_MEDIA = m.createProperty(BASE_URI, "hasAttachedMedia");
    public static final Property HAS_HEIGHT = m.createProperty(BASE_URI, "hasHeight");
    public static final Property HAS_DEPTH = m.createProperty(BASE_URI, "hasDepth");
    public static final Property HAS_WIDTH = m.createProperty(BASE_URI, "hasWidth");
    public static final Property HAS_WEIGHT = m.createProperty(BASE_URI, "hasWeight");
    public static final Property HAS_QUANTITATIVE_PROPERTY = m.createProperty(BASE_URI, "hasQuantitativeProperty");

    public static final Property TRAVEL_ACTION = m.createProperty("http://purl.org/webofneeds/model#travelAction");

    public static final Property HAS_FACET = m.createProperty(BASE_URI, "hasFacet");
    public static final Property HAS_DEFAULT_FACET = m.createProperty(BASE_URI, "hasDefaultFacet");
    public static final Resource FACET = m.createResource(BASE_URI + "Facet");
    //This property is used in the rdf-model part of connect (from owner) and hint
    //to specify a facet to which a connection is created
    public static final Property HAS_REMOTE_FACET = m.createProperty(BASE_URI + "hasRemoteFacet");

    public static final Property HAS_CONNECTIONS = m.createProperty(BASE_URI, "hasConnections");
    public static final Resource CONNECTION_CONTAINER = m.createResource(BASE_URI + "ConnectionContainer");

    public static final Resource CONNECTION = m.createResource(BASE_URI + "Connection");
    public static final Property HAS_CONNECTION_STATE = m.createProperty(BASE_URI, "hasConnectionState");
    public static final Property HAS_REMOTE_CONNECTION = m.createProperty(BASE_URI, "hasRemoteConnection");
    public static final Property HAS_REMOTE_NEED = m.createProperty(BASE_URI, "hasRemoteNeed");
    public static final Property BELONGS_TO_NEED = m.createProperty(BASE_URI, "belongsToNeed");

    public static final Property HAS_EVENT_CONTAINER = m.createProperty(BASE_URI, "hasEventContainer");
    public static final Resource EVENT_CONTAINER = m.createResource(BASE_URI + "EventContainer");

    public static final Property HAS_TIME_STAMP = m.createProperty(BASE_URI, "hasTimeStamp");
    public static final Property HAS_ORIGINATOR = m.createProperty(BASE_URI, "hasOriginator");

    public static final Property HAS_ADDITIONAL_DATA = m.createProperty(BASE_URI, "hasAdditionalData");
    public static final Resource ADDITIONAL_DATA_CONTAINER = m.createResource(BASE_URI + "AdditionalDataContainer");

    public static final Property HAS_MATCH_SCORE = m.createProperty(BASE_URI, "hasMatchScore");
    public static final Property HAS_MATCH_COUNTERPART = m.createProperty(BASE_URI, "hasMatchCounterpart");

    public static final Property SEEKS = m.createProperty(BASE_URI, "seeks");

    public static final Property GOAL = m.createProperty(BASE_URI, "goal");
    public static final Property HAS_SHAPES_GRAPH = m.createProperty(BASE_URI, "hasShapesGraph");
    public static final Property HAS_DATA_GRAPH = m.createProperty(BASE_URI, "hasDataGraph");

    public static final Property HAS_NEED_MODALITY = m.createProperty(BASE_URI, "hasNeedModality");
    public static final Resource NEED_MODALITY = m.createResource(BASE_URI + "NeedModality");

    public static final Property HAS_PRICE_SPECIFICATION = m.createProperty(BASE_URI, "hasPriceSpecification");
    public static final Resource PRICE_SPECIFICATION = m.createResource(BASE_URI + "PriceSpecification");
    public static final Property HAS_LOWER_PRICE_LIMIT = m.createProperty(BASE_URI, "hasLowerPriceLimit");
    public static final Property HAS_UPPER_PRICE_LIMIT = m.createProperty(BASE_URI, "hasUpperPriceLimit");
    public static final Property HAS_CURRENCY = m.createProperty(BASE_URI, "hasCurrency");

    public static final Property HAS_LOCATION = m.createProperty(BASE_URI, "hasLocation");
    public static final Property HAS_BOUNDING_BOX = m.createProperty(BASE_URI, "hasBoundingBox");
    public static final Property HAS_NORTH_WEST_CORNER = m.createProperty(BASE_URI, "hasNorthWestCorner");
    public static final Property HAS_SOUTH_EAST_CORNER = m.createProperty(BASE_URI, "hasSouthEastCorner");
    public static final Property HAS_BOUNDS_NORTH_WEST = m.createProperty(BASE_URI, "hasBoundsNorthWest");
    public static final Property HAS_BOUNDS_SOUTH_EAST = m.createProperty(BASE_URI, "hasBoundsSouthEast");
    public static final Resource LOCATION_SPECIFICATION = m.createResource(BASE_URI + "Location");
    public static final Property GEO_SPATIAL = m.createProperty(BASE_URI + "geoSpatial");
    public static final Property IS_CONCEALED = m.createProperty(BASE_URI, "isConcealed");
    public static final Resource REGION = m.createResource(BASE_URI + "Region");
    public static final Property HAS_ISO_CODE = m.createProperty(BASE_URI, "hasISOCode");

    public static final Property HAS_TIME_SPECIFICATION = m.createProperty(BASE_URI, "hasTimeSpecification");
    public static final Resource TIME_SPECIFICATION = m.createResource(BASE_URI + "TimeSpecification");
    public static final Property HAS_START_TIME = m.createProperty(BASE_URI, "hasStartTime");
    public static final Property HAS_END_TIME = m.createProperty(BASE_URI, "hasEndTime");
    public static final Property HAS_RECURS_IN = m.createProperty(BASE_URI, "hasRecursIn");
    public static final Property HAS_RECURS_TIMES = m.createProperty(BASE_URI, "hasRecursTimes");
    public static final Property HAS_RECUR_INFINITE_TIMES = m.createProperty(BASE_URI, "hasRecurInfiniteTimes");

    // Resource individuals

    public static final Resource NEED_STATE_ACTIVE = m.createResource(NeedState.ACTIVE.getURI().toString());
    public static final Resource NEED_STATE_INACTIVE = m.createResource(NeedState.INACTIVE.getURI().toString());
    public static final Resource NEED_STATE_DELETED = m.createResource(NeedState.DELETED.getURI().toString());

    public static final Resource CONNECTION_STATE_SUGGESTED = m.createResource(ConnectionState.SUGGESTED.getURI().toString());
    public static final Resource CONNECTION_STATE_REQUEST_SENT = m.createResource(ConnectionState.REQUEST_SENT.getURI().toString());
    public static final Resource CONNECTION_STATE_REQUEST_RECEIVED = m.createResource(ConnectionState.REQUEST_RECEIVED.getURI().toString());
    public static final Resource CONNECTION_STATE_CONNECTED = m.createResource(ConnectionState.CONNECTED.getURI().toString());
    public static final Resource CONNECTION_STATE_CLOSED = m.createResource(ConnectionState.CLOSED.getURI().toString());
    public static final Resource CONNECTION_STATE_DELETED = m.createResource(ConnectionState.DELETED.getURI().toString());
    
    public static final Property HAS_SUGGESTED_COUNT = m.createProperty(BASE_URI,
            "hasSuggestedCount");
    public static final Property HAS_REQUEST_RECEIVED_COUNT = m.createProperty(BASE_URI,
            "hasRequestReceivedCount");
    public static final Property HAS_REQUEST_SENT_COUNT = m.createProperty(BASE_URI,
            "hasRequestSentCount");
    public static final Property HAS_CONNECTED_COUNT = m.createProperty(BASE_URI,
            "hasConnectedCount");
    public static final Property HAS_CLOSED_COUNT = m.createProperty(BASE_URI,
            "hasClosedCount");
    
    //adds a flag to a need
    public static final Property HAS_FLAG = m.createProperty(BASE_URI + "hasFlag");

    public static final Property DO_NOT_MATCH_BEFORE = m.createProperty(BASE_URI + "doNotMatchBefore");
    public static final Property DO_NOT_MATCH_AFTER = m.createProperty(BASE_URI + "doNotMatchAfter");

    //the usedForTesting flag: need is not a real need, only match with other needs flagged with usedForTesting
    public static final Resource USED_FOR_TESTING = m.createResource(BASE_URI + "UsedForTesting");

    public static final Resource WHATS_AROUND = m.createResource(BASE_URI + "WhatsAround");

    public static final Resource WHATS_NEW = m.createResource(BASE_URI + "WhatsNew");
    
    // hint behaviour
    public static final Resource NO_HINT_FOR_COUNTERPART = m.createResource(BASE_URI + "NoHintForCounterpart");
    public static final Resource NO_HINT_FOR_ME = m.createResource(BASE_URI + "NoHintForMe");
    public static final Property HAS_MATCHING_CONTEXT = m.createProperty(BASE_URI + "hasMatchingContext");
    public static final Property HAS_QUERY = m.createProperty(BASE_URI + "hasQuery");

    
    public static final Property HAS_GRAPH = m.createProperty(BASE_URI, "hasGraph");


    //search result model
    public static final Resource Match = m.createResource(BASE_URI + "Match");
    public static final Property SEARCH_RESULT_URI = m.createProperty(BASE_URI, "uri");
    public static final Property SEARCH_RESULT_PREVIEW = m.createProperty(BASE_URI, "preview");

    public static final String PRIVATE_DATA_GRAPH_URI = BASE_URI + "privateDataGraph";

    public static final String GROUP_FACET_STRING = BASE_URI + "GroupFacet";
    
    public static final String HAS_GROUP_MEMBER_String = BASE_URI + "hasGroupMember";
    public static final Property HAS_GROUP_MEMBER = m.createProperty(BASE_URI, "hasGroupMember");
    public static final String OWNED_BY_STRING = BASE_URI + "ownedBy";
    public static final Property OWNED_BY = m.createProperty(BASE_URI + "ownedBy");
    public static final String HELD_BY_STRING = BASE_URI + "heldBy";
    public static final Property HELD_BY = m.createProperty(BASE_URI + "heldBy");
    public static final String HOLDS_STRING = BASE_URI + "holds";
    public static final Property HOLDS = m.createProperty(BASE_URI + "holds");
    public static final String OWNS_STRING = BASE_URI + "owns";
    public static final Property OWNS = m.createProperty(BASE_URI + "owns");
    public static final String CONNECTED_WITH_STRING = BASE_URI + "connectedWith";
    public static final Property CONNECTED_WITH = m.createProperty(BASE_URI + "connectedWith");
    public static final Property REVIEWS = m.createProperty(BASE_URI + "reviews");
    public static final String REVIEWS_STRING = BASE_URI + "reviews";
    
    public static final String CHAT_FACET_STRING = BASE_URI + "ChatFacet";
    public static final String REVIEW_FACET_STRING = BASE_URI + "ReviewFacet";
    
    //unread information
	public static final Property HAS_UNREAD_SUGGESTED = m.createProperty(BASE_URI + "hasUnreadSuggested");
	public static final Property HAS_UNREAD_REQUEST_SENT = m.createProperty(BASE_URI + "hasUnreadRequestSent");
	public static final Property HAS_UNREAD_REQUEST_RECEIVED = m.createProperty(BASE_URI + "hasUnreadRequestReceived");
	public static final Property HAS_UNREAD_CONNECTED = m.createProperty(BASE_URI + "hasUnreadConnected");
	public static final Property HAS_UNREAD_CLOSED = m.createProperty(BASE_URI + "hasUnreadClosed");
	public static final Property HAS_UNREAD_OLDEST_TIMESTAMP = m.createProperty(BASE_URI + "hasUnreadOldestTimestamp");
	public static final Property HAS_UNREAD_NEWEST_TIMESTAMP = m.createProperty(BASE_URI + "hasUnreadNewestTimestamp");
	public static final Property HAS_UNREAD_COUNT = m.createProperty(BASE_URI + "hasUnreadCount");
	
	

    /**
     * Returns the base URI for this schema.
     *
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }

    /**
     * Converts the NeedState Enum to a Resource.
     *
     * @param state
     * @return
     */
    public static Resource toResource(NeedState state) {
        switch (state) {
            case ACTIVE:
                return NEED_STATE_ACTIVE;
            case INACTIVE:
                return NEED_STATE_INACTIVE;
            case DELETED:
                return NEED_STATE_DELETED;
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
