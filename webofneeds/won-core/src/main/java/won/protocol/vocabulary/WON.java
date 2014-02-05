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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.model.BasicNeedType;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.NeedState;

/**
 * WON vocabulary.
 * <p/>
 * User: fkleedorfer                     public Model showNodeInformation(final int page)
 * Date: 20.11.12
 */
public class WON
{
  public static final String BASE_URI = "http://purl.org/webofneeds/model#";


    private static Model m = ModelFactory.createDefaultModel();


  public static final Resource NEED = m.createResource(BASE_URI + "Need");
  public static final Property HAS_NEED_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "hasNeedProtocolEndpoint");
  public static final Property HAS_MATCHER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "hasMatcherProtocolEndpoint");
  public static final Property HAS_OWNER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "hasOwnerProtocolEndpoint");

  public static final Property HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME = m.createProperty(BASE_URI,"hasActiveMQNeedProtocolQueueName");
  public static final Property HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME = m.createProperty(BASE_URI,"hasActiveMQOwnerProtocolQueueName");
  public static final Property EMBED_SPIN_ASK = m.createProperty(BASE_URI, "embedSpinAsk");

  public static final Property SUPPORTS_WON_PROTOCOL_IMPL = m.createProperty(BASE_URI + "supportsWonProtocolImpl");
  public static final Resource WON_OVER_ACTIVE_MQ = m.createResource(BASE_URI + "WonOverActiveMq");
  public static final Property HAS_BROKER_URI = m.createProperty(BASE_URI,"hasBrokerUri");
  public static final Resource WON_OVER_SOAP_WS = m.createResource(BASE_URI + "WonOverSoapWs");
  public static final Property IS_IN_STATE = m.createProperty(BASE_URI, "isInState");

  public static final Property HAS_BASIC_NEED_TYPE = m.createProperty(BASE_URI, "hasBasicNeedType");

  public static final Property HAS_CONTENT = m.createProperty(BASE_URI, "hasContent");

  public static final Resource TEXT_MESSAGE = m.createResource(BASE_URI + "TextMessage");
  public static final Property HAS_TEXT_MESSAGE = m.createProperty(BASE_URI + "hasTextMessage");

  public static final Resource NEED_CONTENT = m.createResource(BASE_URI + "NeedContent");
  public static final Property HAS_TEXT_DESCRIPTION = m.createProperty(BASE_URI, "hasTextDescription");
  public static final Property HAS_CONTENT_DESCRIPTION = m.createProperty(BASE_URI, "hasContentDescription");
  public static final Property HAS_TAG = m.createProperty(BASE_URI, "hasTag");

  public static final Property HAS_FACET = m.createProperty(BASE_URI, "hasFacet");
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
  public static final Resource EVENT = m.createResource(BASE_URI + "Event");
  public static final Property HAS_TIME_STAMP = m.createProperty(BASE_URI, "hasTimeStamp");
  public static final Property HAS_ORIGINATOR = m.createProperty(BASE_URI, "hasOriginator");

  public static final Property HAS_ADDITIONAL_DATA = m.createProperty(BASE_URI, "hasAdditionalData");
  public static final Resource ADDITIONAL_DATA_CONTAINER = m.createResource(BASE_URI + "AdditionalDataContainer");

  public static final Property HAS_MATCH_SCORE = m.createProperty(BASE_URI, "hasMatchScore");

  public static final Property HAS_NEED_MODALITY = m.createProperty(BASE_URI, "hasNeedModality");
  public static final Resource NEED_MODALITY = m.createResource(BASE_URI + "NeedModality");

  public static final Property HAS_PRICE_SPECIFICATION = m.createProperty(BASE_URI, "hasPriceSpecification");
  public static final Resource PRICE_SPECIFICATION = m.createResource(BASE_URI + "PriceSpecification");
  public static final Property HAS_LOWER_PRICE_LIMIT = m.createProperty(BASE_URI, "hasLowerPriceLimit");
  public static final Property HAS_UPPER_PRICE_LIMIT = m.createProperty(BASE_URI, "hasUpperPriceLimit");
  public static final Property HAS_CURRENCY = m.createProperty(BASE_URI, "hasCurrency");

  public static final Property AVAILABLE_AT_LOCATION = m.createProperty(BASE_URI, "hasLocationSpecification");
  public static final Resource LOCATION_SPECIFICATION = m.createResource(BASE_URI + "LocationSpecification");
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
  public static final Resource EVENT_TYPE_OWNER_CLOSE = m.createResource(ConnectionEventType.OWNER_CLOSE.getURI().toString());
  public static final Resource EVENT_TYPE_OWNER_OPEN = m.createResource(ConnectionEventType.OWNER_OPEN.getURI().toString());
  public static final Resource EVENT_TYPE_PARTNER_CLOSE = m.createResource(ConnectionEventType.PARTNER_CLOSE.getURI().toString());
  public static final Resource EVENT_TYPE_PARTNER_OPEN = m.createResource(ConnectionEventType.PARTNER_OPEN.getURI().toString());
  public static final Resource EVENT_TYPE_PARTNER_MESSAGE = m.createResource(ConnectionEventType.PARTNER_MESSAGE.getURI().toString());
  public static final Resource EVENT_TYPE_OWNER_MESSAGE = m.createResource(ConnectionEventType.OWNER_MESSAGE.getURI().toString());
  public static final Resource EVENT_TYPE_HINT = m.createResource(ConnectionEventType.MATCHER_HINT.getURI().toString());
  public static final Resource EVENT_TYPE_CHAT_MESSAGE = m.createResource(ConnectionEventType.CHAT_MESSAGE.getURI().toString());

  public static final Resource BASIC_NEED_TYPE_DO_TOGETHER = m.createResource(BasicNeedType.DO_TOGETHER.getURI().toString());
  public static final Resource BASIC_NEED_TYPE_SUPPLY = m.createResource(BasicNeedType.SUPPLY.getURI().toString());
  public static final Resource BASIC_NEED_TYPE_DEMAND = m.createResource(BasicNeedType.DEMAND.getURI().toString());
  public static final Resource BASIC_NEED_TYPE_CRITIQUE = m.createResource(BasicNeedType.CRITIQUE.getURI().toString());

  public static final Resource NEED_STATE_ACTIVE = m.createResource(NeedState.ACTIVE.getURI().toString());
  public static final Resource NEED_STATE_INACTIVE = m.createResource(NeedState.INACTIVE.getURI().toString());

  public static final Resource CONNECTION_STATE_SUGGESTED = m.createResource(ConnectionState.SUGGESTED.getURI().toString());
  public static final Resource CONNECTION_STATE_REQUEST_SENT = m.createResource(ConnectionState.REQUEST_SENT.getURI().toString());
  public static final Resource CONNECTION_STATE_REQUEST_RECEIVED = m.createResource(ConnectionState.REQUEST_RECEIVED.getURI().toString());
  public static final Resource CONNECTION_STATE_CONNECTED = m.createResource(ConnectionState.CONNECTED.getURI().toString());
  public static final Resource CONNECTION_STATE_CLOSED = m.createResource(ConnectionState.CLOSED.getURI().toString());

  //search result model
  public static final Property SEARCH_RESULT_URI = m.createProperty(BASE_URI,"uri");
  public static final Property SEARCH_RESULT_PREVIEW = m.createProperty(BASE_URI, "preview");

    public static final Property COORDINATION_MESSAGE = m.createProperty(BASE_URI + "coordinationMessage");
  /**
   * Returns the base URI for this schema.
   *
   * @return the URI for this schema
   */
  public static String getURI()
  {
    return BASE_URI;
  }

  /**
   * Converts the NeedState Enum to a Resource.
   *
   * @param state
   * @return
   */
  public static Resource toResource(NeedState state)
  {
    switch (state) {
      case ACTIVE:
        return NEED_STATE_ACTIVE;
      case INACTIVE:
        return NEED_STATE_INACTIVE;
      default:
        throw new IllegalStateException("No case specified for " + state.name());
    }
  }

  /**
   * Converts the BasicNeedType Enum to a Resource.
   *
   * @param type
   * @return
   */
  public static Resource toResource(BasicNeedType type)
  {
    switch (type) {
      case DO_TOGETHER:
        return BASIC_NEED_TYPE_DO_TOGETHER;
      case SUPPLY:
        return BASIC_NEED_TYPE_SUPPLY;
      case DEMAND:
        return BASIC_NEED_TYPE_DEMAND;
      case CRITIQUE:
        return BASIC_NEED_TYPE_CRITIQUE;
      default:
        throw new IllegalStateException("No such case specified for " + type.name());
    }
  }

  /**
   * Converts the EventType Enum to a Resource.
   *
   * @param type
   * @return
   */
  public static Resource toResource(ConnectionEventType type)
  {
    switch (type) {
      case OWNER_CLOSE:
        return EVENT_TYPE_OWNER_CLOSE;
      case PARTNER_CLOSE:
        return EVENT_TYPE_PARTNER_CLOSE;
      case MATCHER_HINT:
        return EVENT_TYPE_HINT;
      case OWNER_OPEN:
        return EVENT_TYPE_OWNER_OPEN;
      case PARTNER_OPEN:
        return EVENT_TYPE_PARTNER_OPEN;
      case OWNER_MESSAGE:
        return EVENT_TYPE_OWNER_MESSAGE;
      case PARTNER_MESSAGE:
        return EVENT_TYPE_PARTNER_MESSAGE;
      case CHAT_MESSAGE:
            return EVENT_TYPE_CHAT_MESSAGE;
      default:
        throw new IllegalStateException("No such case specified for " + type.name());
    }
  }

  /**
   * Converts the ConnectionState Enum to a Resource.
   *
   * @param type
   * @return
   */
  public static Resource toResource(ConnectionState type)
  {
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
        throw new IllegalStateException("No such case specified for " + type.name());
    }
  }

}
