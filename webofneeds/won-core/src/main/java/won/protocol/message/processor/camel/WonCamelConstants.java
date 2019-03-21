/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.message.processor.camel;

import org.apache.jena.riot.Lang;

/**
 * Constants used for camel.
 */
public class WonCamelConstants {
  public static final String MESSAGE_HEADER = "wonMessage";
  public static final String DIRECTION_HEADER = "wonDirection";
  public static final String FACET_TYPE_HEADER = "wonFacetType";
  public static final String MESSAGE_TYPE_HEADER = "wonMessageType";

  public static final Lang RDF_LANGUAGE_FOR_MESSAGE = Lang.TRIG;
  public static final String ORIGINAL_MESSAGE_HEADER = "wonOriginalMessage";
  public static final String OUTBOUND_MESSAGE_HEADER = "wonOutboundMessage";
  public static final String CONNECTION_URI_HEADER = "wonConnectionURI";
  public static final String CONNECTION_STATE_CHANGE_BUILDER_HEADER = "connectionStateChangeBuilder";

  public static final String OUTBOUND_MESSAGE_FACTORY_HEADER = "wonOutboundMessageFactory";

  public static final String OWNER_APPLICATION_ID = "ownerApplicationId";
  public static final String OWNER_APPLICATIONS = "ownerApplications";

  public static final String SUPPRESS_MESSAGE_TO_OWNER = "suppressMessageToOwner";
  public static final String SUPPRESS_MESSAGE_REACTION = "suppressMessageReaction";

  public static final String IGNORE_HINT = "ignoreHint";

}
