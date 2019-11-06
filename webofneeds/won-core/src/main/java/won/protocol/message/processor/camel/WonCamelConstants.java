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
package won.protocol.message.processor.camel;

import org.apache.jena.riot.Lang;

/**
 * Constants used for camel.
 */
public class WonCamelConstants {
    public static final String RECIPIENT_ATOM_HEADER = "won.atom";
    public static final String SENDER_NODE_HEADER = "won.senderNode";
    public static final String RECIPIENT_NODE_HEADER = "won.recipientNode";
    public static final String MESSAGE_HEADER = "won.message";
    public static final String RESPONSE_HEADER = "won.responseMessage";
    public static final String MESSAGE_TO_SEND_HEADER = "won.messageToSend";
    public static final String DIRECTION_HEADER = "won.direction";
    public static final String SOCKET_TYPE_HEADER = "won.socketType";
    public static final String MESSAGE_TYPE_HEADER = "won.messageType";
    public static final Lang RDF_LANGUAGE_FOR_MESSAGE = Lang.TRIG;
    public static final String OUTBOUND_MESSAGE_HEADER = "won.outboundMessage";
    public static final String CONNECTION_URI_HEADER = "won.connectionURI";
    public static final String CONNECTION_STATE_CHANGE_BUILDER_HEADER = "won.connectionStateChangeBuilder";
    public static final String OWNER_APPLICATION_ID_HEADER = "won.ownerApplicationId";
    public static final String OWNER_APPLICATION_IDS_HEADER = "won.ownerApplicationIds";
    public static final String SUPPRESS_MESSAGE_TO_OWNER_HEADER = "won.suppressMessageToOwner";
    public static final String SUPPRESS_MESSAGE_TO_NODE_HEADER = "won.suppressMessageToNode";
    public static final String SUPPRESS_MESSAGE_REACTION_HADER = "won.suppressMessageReaction";
    public static final String IGNORE_HINT_HEADER = "won.ignoreHint";
    public static final String PARENT_URI_HEADER = "won.ParentUri";
    public static final String REMOTE_BROKER_ENDPOINT_HEADER = "won.remoteBrokerEndpoint";
}
