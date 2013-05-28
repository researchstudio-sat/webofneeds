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
 *
 * User: fkleedorfer
 * Date: 20.11.12
 */
public class WON
{
  public static final String BASE_URI = "http://purl.org/webofneeds/model#";
  private static Model m = ModelFactory.createDefaultModel();

  public static final Resource NEED = m.createResource(BASE_URI + "Need");
  public static final Property NEED_CREATION_DATE = m.createProperty(BASE_URI, "needCreationDate");
  public static final Property NEED_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "needProtocolEndpoint");
  public static final Property MATCHER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "matcherProtocolEndpoint");
  public static final Property OWNER_PROTOCOL_ENDPOINT = m.createProperty(BASE_URI, "ownerProtocolEndpoint");

  public static final Property IS_IN_STATE = m.createProperty(BASE_URI, "isInState");
  public static final Resource NEED_STATE = m.createResource(BASE_URI + "NeedState");

  public static final Property HAS_BASIC_NEED_TYPE = m.createProperty(BASE_URI, "hasBasicNeedType");
  public static final Resource BASIC_NEED_TYPE = m.createResource(BASE_URI + "BasicNeedType");

  public static final Property HAS_CONTENT = m.createProperty(BASE_URI, "hasContent");
  public static final Resource NEED_CONTENT = m.createResource(BASE_URI + "NeedContent");
  public static final Property TEXT_DESCRIPTION = m.createProperty(BASE_URI, "hasTextDescription");
  public static final Property CONTENT_DESCRIPTION = m.createProperty(BASE_URI, "hasContentDescription");
  public static final Property HEIGHT = m.createProperty(BASE_URI, "hasHeight");
  public static final Property DEPTH = m.createProperty(BASE_URI, "hasDepth");
  public static final Property WIDTH = m.createProperty(BASE_URI, "hasWidth");
  public static final Property WEIGHT = m.createProperty(BASE_URI, "hasWeight");
  public static final Property QUANTITATIVE_PROPERTY = m.createProperty(BASE_URI, "hasQuantitativeProperty");

  public static final Property HAS_OWNER = m.createProperty(BASE_URI, "hasOwner");
  public static final Resource OWNER = m.createResource(BASE_URI + "Owner");
  public static final Resource ANONYMIZED_OWNER = m.createResource(BASE_URI + "AnonymizedOwner");

  public static final Property HAS_CONNECTIONS = m.createProperty(BASE_URI, "hasConnections");
  public static final Resource CONNECTION_CONTAINER = m.createResource(BASE_URI + "ConnectionContainer");

  public static final Resource CONNECTION = m.createResource(BASE_URI + "Connection");
  public static final Property HAS_CONNECTION_STATE = m.createProperty(BASE_URI, "hasConnectionState");
  public static final Property HAS_REMOTE_CONNECTION = m.createProperty(BASE_URI, "hasRemoteConnection");
  public static final Property BELONGS_TO_NEED = m.createProperty(BASE_URI, "belongsToNeed");

  public static final Property HAS_EVENT_CONTAINER = m.createProperty(BASE_URI, "hasEventContainer");
  public static final Resource EVENT_CONTAINER = m.createResource(BASE_URI + "EventContainer");
  public static final Resource EVENT = m.createResource(BASE_URI + "Event");
  public static final Property OCCURED_AT = m.createProperty(BASE_URI, "hasTimeStamp");
  public static final Property HAS_ORIGINATOR = m.createProperty(BASE_URI, "hasOriginator");

  public static final Property HAS_ADDITIONAL_DATA = m.createProperty(BASE_URI, "hasAdditionalData");
  public static final Resource ADDITIONAL_DATA_CONTAINER = m.createResource(BASE_URI + "AdditionalDataContainer");

  public static final Property HAS_NEED_MODALITY = m.createProperty(BASE_URI, "hasNeedModality");
  public static final Resource NEED_MODALITY = m.createResource(BASE_URI + "NeedModality");

  public static final Property HAS_PRICE_SPECIFICATION = m.createProperty(BASE_URI, "hasPriceSpecification");
  public static final Resource PRICE_SPECIFICATION = m.createResource(BASE_URI + "PriceSpecification");
  public static final Property HAS_LOWER_PRICE_LIMIT = m.createProperty(BASE_URI, "hasLowerPriceLimit");
  public static final Property HAS_UPPER_PRICE_LIMIT = m.createProperty(BASE_URI, "hasUpperPriceLimit");
  public static final Property HAS_CURRENCY = m.createProperty(BASE_URI, "hasCurrency");

  public static final Property AVAILABLE_AT_LOCATION = m.createProperty(BASE_URI, "hasLocationSpecification");
  public static final Resource LOCATION = m.createResource(BASE_URI + "LocationSpecification");
  public static final Property IS_CONCEALED = m.createProperty(BASE_URI, "isConcealed");
  public static final Resource REGION = m.createResource(BASE_URI + "Region");
  public static final Property HAS_ISO_CODE = m.createProperty(BASE_URI, "hasISOCode");

  public static final Property AVAILABLE_AT_TIME = m.createProperty(BASE_URI, "hasTimeSpecification");
  public static final Resource TIME_CONSTRAINT = m.createResource(BASE_URI + "TimeSpecification");
  public static final Property START_TIME = m.createProperty(BASE_URI, "hasStartTime");
  public static final Property END_TIME = m.createProperty(BASE_URI, "hasEndTime");
  public static final Property RECUR_IN = m.createProperty(BASE_URI, "hasRecursIn");
  public static final Property RECUR_TIMES = m.createProperty(BASE_URI, "hasRecursTimes");
  public static final Property RECUR_INFINITE_TIMES = m.createProperty(BASE_URI, "hasRecurInfiniteTimes");

  // Resource individuals
  public static final Resource EVENT_TYPE_CLOSE = m.createResource(ConnectionEventType.OWNER_CLOSE.getURI().toString());
  public static final Resource EVENT_TYPE_OPEN = m.createResource(ConnectionEventType.OWNER_OPEN.getURI().toString());
  public static final Resource EVENT_TYPE_HINT = m.createResource(ConnectionEventType.MATCHER_HINT.getURI().toString());

  public static final Resource BASIC_NEED_TYPE_DO = m.createResource(BasicNeedType.DO.getURI().toString());
  public static final Resource BASIC_NEED_TYPE_GIVE = m.createResource(BasicNeedType.GIVE.getURI().toString());
  public static final Resource BASIC_NEED_TYPE_TAKE = m.createResource(BasicNeedType.TAKE.getURI().toString());

  public static final Resource NEED_STATE_ACTIVE = m.createResource(NeedState.ACTIVE.getURI().toString());
  public static final Resource NEED_STATE_INACTIVE = m.createResource(NeedState.INACTIVE.getURI().toString());

  public static final Resource CONNECTION_STATE_SUGGESTED = m.createResource(ConnectionState.SUGGESTED.getURI().toString());
  public static final Resource CONNECTION_STATE_REQUEST_SENT = m.createResource(ConnectionState.REQUEST_SENT.getURI().toString());
  public static final Resource CONNECTION_STATE_REQUEST_RECEIVED = m.createResource(ConnectionState.REQUEST_RECEIVED.getURI().toString());
  public static final Resource CONNECTION_STATE_CONNECTED = m.createResource(ConnectionState.CONNECTED.getURI().toString());
  public static final Resource CONNECTION_STATE_CLOSED = m.createResource(ConnectionState.CLOSED.getURI().toString());

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
      case DO:
        return BASIC_NEED_TYPE_DO;
      case GIVE:
        return BASIC_NEED_TYPE_GIVE;
      case TAKE:
        return BASIC_NEED_TYPE_TAKE;
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
      case PARTNER_CLOSE:
        return EVENT_TYPE_CLOSE;
      case MATCHER_HINT:
        return EVENT_TYPE_HINT;
      case OWNER_OPEN:
      case PARTNER_OPEN:
        return EVENT_TYPE_OPEN;
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
