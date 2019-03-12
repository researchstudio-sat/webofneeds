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

/**
 * Created by LEIH-NB on 19.08.2014.
 */
"format es6" /* required to force babel to transpile this so the minifier is happy */;
import { is, prefixOfUri, isArray, clone, createArray } from "../utils.js";
import {
  clearReadUris,
  clearDisclaimerAccepted,
  isDisclaimerAccepted,
  setDisclaimerAccepted,
} from "../won-localstorage.js";
import jsonld from "jsonld";

import N3 from "n3";
window.N34dbg = N3;

let won = {};

/**
 *  Constants
 *
 */

won.debugmode = false; //if you set this to true, the created needs will get flagged as debug needs in order to get matches and requests from the debugbot

won.clearReadUris = clearReadUris;
won.isDisclaimerAccepted = isDisclaimerAccepted;
won.clearDisclaimerAccepted = clearDisclaimerAccepted;
won.setDisclaimerAccepted = setDisclaimerAccepted;

won.WON = {};
won.WON.baseUri = "http://purl.org/webofneeds/model#";
//won.WON.matcherURI = "http://localhost:8080/matcher/search/"
won.WON.matcherURI = "https://localhost:8443/matcher/search/";
//won.WON.matcherURI = "http://sat001.researchstudio.at:8080/matcher/search/";
//won.WON.matcherURI = "https://sat001.researchstudio.at:8443/matcher/search/";

won.WON.prefix = "won";
won.WON.hasWonNode = won.WON.baseUri + "hasWonNode";
won.WON.hasWonNodeCompacted = won.WON.prefix + ":hasWonNode";
won.WON.Active = won.WON.baseUri + "Active";
won.WON.ActiveCompacted = won.WON.prefix + ":Active";
won.WON.Closed = won.WON.baseUri + "Closed";
won.WON.ClosedCompacted = won.WON.prefix + ":Closed";

won.WON.Inactive = won.WON.baseUri + "Inactive";
won.WON.InactiveCompacted = won.WON.prefix + ":Inactive";

won.WON.isInState = won.WON.baseUri + "isInState";
won.WON.isInStateCompacted = won.WON.prefix + ":isInState";
won.WON.hasFacet = won.WON.baseUri + "hasFacet";
won.WON.hasFacetCompacted = won.WON.prefix + ":hasFacet";
won.WON.hasFlag = won.WON.baseUri + "hasFlag";
won.WON.hasFlagCompacted = won.WON.prefix + "hasFlag";
won.WON.hasRemoteFacet = won.WON.baseUri + "hasRemoteFacet";
won.WON.hasRemoteFacetCompacted = won.WON.prefix + ":hasRemoteFacet";

won.WON.hasRemoteNeed = won.WON.baseUri + "hasRemoteNeed";
won.WON.hasRemoteNeedCompacted = won.WON.prefix + ":hasRemoteNeed";
won.WON.hasRemoteConnection = won.WON.baseUri + "hasRemoteConnection";
won.WON.hasRemoteConnectionCompacted = won.WON.prefix + ":hasRemoteConnection";
won.WON.forResource = won.WON.baseUri + "forResource";
won.WON.hasBinaryRating = won.WON.baseUri + "hasBinaryRating";
won.WON.binaryRatingGood = won.WON.baseUri + "Good";
won.WON.binaryRatingBad = won.WON.baseUri + "Bad";
won.WON.hasFeedback = won.WON.baseUri + "hasFeedback";

won.WON.hasConnectionState = won.WON.baseUri + "hasConnectionState";
won.WON.hasConnectionState = won.WON.prefix + ":hasConnectionState";
won.WON.Suggested = won.WON.baseUri + "Suggested";
won.WON.SuggestedCompacted = won.WON.baseUri + ":Suggested";
won.WON.RequestReceived = won.WON.baseUri + "RequestReceived";
won.WON.RequestReceivedCompacted = won.WON.baseUri + ":RequestReceived";
won.WON.RequestSent = won.WON.baseUri + "RequestSent";
won.WON.RequestSentCompacted = won.WON.baseUri + ":RequestSent";

won.WON.Connected = won.WON.baseUri + "Connected";

//EVENT TYPES
won.WON.OwnerClose = won.WON.baseUri + "OwnerClose";
won.WON.OwnerCloseCompacted = won.WON.prefix + ":OwnerClose";
won.WON.OwnerOpen = won.WON.baseUri + "OwnerOpen";
won.WON.OwnerOpenCompacted = won.WON.prefix + ":OwnerOpen";
won.WON.PartnerClose = won.WON.baseUri + "PartnerClose";
won.WON.PartnerCloseCompacted = won.WON.prefix + ":PartnerClose";
won.WON.PartnerOpen = won.WON.baseUri + "PartnerOpen";
won.WON.PartnerOpenCompacted = won.WON.prefix + ":PartnerOpen";
won.WON.PartnerMessage = won.WON.baseUri + "PartnerMessage";
won.WON.PartnerMessageCompacted = won.WON.prefix + ":PartnerMessage";
won.WON.OwnerMessage = won.WON.baseUri + "OwnerMessage";
won.WON.OwnerMessageCompacted = won.WON.prefix + ":OwnerMessage";
won.WON.Hint = won.WON.baseUri + "Hint";
won.WON.HintCompacted = won.WON.prefix + ":Hint";

//TOAST TYPES
won.WON.infoToast = won.WON.baseUri + "InfoToast";
won.WON.warnToast = won.WON.baseUri + "WarnToast";
won.WON.errorToast = won.WON.baseUri + "ErrorToast";

won.WON.hasGraph = won.WON.baseUri + "hasGraph";
won.WON.hasGraphCompacted = won.WON.prefix + ":hasGraph";

won.WON.Connection = won.WON.baseUri + "Connection";
won.WON.ConnectionCompacted = won.WON.prefix + ":Connection";

won.WON.Event = won.WON.baseUri + "Event";
won.WON.EventCompacted = won.WON.prefix + ":Event";

won.WON.Need = won.WON.baseUri + "Need";
won.WON.NeedCompacted = won.WON.prefix + ":Need";
won.WON.BasicNeedTypeDemand = won.WON.baseUri + "Demand";
won.WON.BasicNeedTypeDemandCompacted = won.WON.prefix + ":Demand";
won.WON.BasicNeedTypeSupply = won.WON.baseUri + "Supply";
won.WON.BasicNeedTypeSupplyCompacted = won.WON.prefix + ":Supply";
won.WON.BasicNeedTypeDotogether = won.WON.baseUri + "DoTogether";
won.WON.BasicNeedTypeDotogetherCompacted = won.WON.prefix + ":DoTogether";
won.WON.BasicNeedTypeCombined = won.WON.baseUri + "Combined";
won.WON.BasicNeedTypeCombinedCompacted = won.WON.baseUri + ":Combined";
won.WON.BasicNeedTypeCritique = won.WON.baseUri + "Critique";
won.WON.BasicNeedTypeCritiqueCompacted = won.WON.prefix + ":Critique";
won.WON.BasicNeedTypeWhatsAroundCompacted = won.WON.prefix + ":WhatsAround";
won.WON.BasicNeedTypeWhatsNewCompacted = won.WON.prefix + ":WhatsNew";
won.WON.NoHintForCounterpartCompacted =
  won.WON.prefix + ":NoHintForCounterpart";
won.WON.UsedForTestingCompacted = won.WON.prefix + ":UsedForTesting";
won.WON.NoHintForMeCompacted = won.WON.prefix + ":NoHintForMe";
won.WON.HoldableFacet = won.WON.baseUri + "HoldableFacet";
won.WON.HoldableFacetCompacted = won.WON.prefix + ":HoldableFacet";
won.WON.HolderFacet = won.WON.baseUri + "HolderFacet";
won.WON.HolderFacetCompacted = won.WON.prefix + ":HolderFacet";
won.WON.ReviewFacet = won.WON.baseUri + "ReviewFacet";
won.WON.ReviewFacetCompacted = won.WON.prefix + ":ReviewFacet";
won.WON.ChatFacet = won.WON.baseUri + "ChatFacet";
won.WON.ChatFacetCompacted = won.WON.prefix + ":ChatFacet";
won.WON.GroupFacet = won.WON.baseUri + "GroupFacet";
won.WON.GroupFacetCompacted = won.WON.prefix + ":GroupFacet";
won.WON.ParticipantFacet = won.WON.baseUri + "ParticipantFacet";
won.WON.ParticipantFacetCompacted = won.WON.prefix + ":ParticipantFacet";
won.WON.CommentFacet = won.WON.baseUri + "CommentFacet";
won.WON.CommentFacetCompacted = won.WON.prefix + ":CommentFacet";
won.WON.CoordinatorFacet = won.WON.baseUri + "CoordinatorFacet";
won.WON.CoordinatorFacetCompacted = won.WON.prefix + ":CoordinatorFacet";
won.WON.belongsToNeed = won.WON.baseUri + "belongsToNeed";
won.WON.belongsToNeedCompacted = won.WON.prefix + ":belongsToNeed";
won.WON.hasBasicNeedType = won.WON.baseUri + "hasBasicNeedType";
won.WON.hasBasicNeedTypeCompacted = won.WON.prefix + ":hasBasicNeedType";
won.WON.hasConnections = won.WON.baseUri + "hasConnections";
won.WON.hasConnectionsCompacted = won.WON.prefix + ":hasConnections";
won.WON.hasConnectionState = won.WON.baseUri + "hasConnectionState";
won.WON.hasConnectionStateCompacted = won.WON.prefix + ":hasConnectionState";
won.WON.hasContent = won.WON.baseUri + "hasContent";
won.WON.hasContentCompacted = won.WON.prefix + ":hasContent";
won.WON.hasContentDescription = won.WON.baseUri + "hasContentDescription";
won.WON.hasContentDescriptionCompacted =
  won.WON.prefix + ":hasContentDescription";
won.WON.hasEventContainer = won.WON.baseUri + "hasEventContainer";
won.WON.hasEventContainerCompacted = won.WON.prefix + ":hasEventContainer";
won.WON.hasOriginator = won.WON.baseUri + "hasOriginator";
won.WON.hasOriginatorCompacted = won.WON.prefix + ":hasOriginator";
won.WON.hasRecurInfiniteTimes = won.WON.baseUri + "hasRecurInfiniteTimes";
won.WON.hasRecurInfiniteTimesCompacted =
  won.WON.prefix + ":hasRecurInfiniteTimes";
won.WON.hasRecursIn = won.WON.baseUri + "hasRecursIn";
won.WON.hasRecursInCompacted = won.WON.prefix + ":hasRecurInfiniteTimes";
won.WON.hasScore = won.WON.baseUri + "hasScore";
won.WON.hasScoreCompacted = won.WON.prefix + ":hasScore";
won.WON.hasTag = won.WON.baseUri + "hasTag";
won.WON.hasTagCompacted = won.WON.prefix + ":hasTag";

won.WON.hasMatchScore = won.WON.baseUri + "hasMatchScore";
won.WON.hasMatchScoreCompacted = won.WON.prefix + ":hasMatchScore";
won.WON.hasMatchCounterpart = won.WON.baseUri + "hasMatchCounterpart";
won.WON.hasMatchCounterpart = won.WON.prefix + ":hasMatchCounterpart";
won.WON.hasTextMessage = won.WON.baseUri + "hasTextMessage";
won.WON.hasTextMessageCompacted = won.WON.prefix + ":hasTextMessage";

won.WON.searchResultURI = won.WON.baseUri + "uri";
won.WON.searchResultPreview = won.WON.baseUri + "preview";
//todo: change to SearchResult
won.WON.searchResult = won.WON.baseUri + "Match";

won.WON.usedForTesting = won.WON.prefix + "UserForTesting";

won.WONMSG = {};
won.WONMSG.baseUri = "http://purl.org/webofneeds/message#";
won.WONMSG.prefix = "msg";

//sender/receiver etc.
won.WONMSG.hasReceiverNeed = won.WONMSG.baseUri + "hasReceiverNeed";
won.WONMSG.hasReceiverNeedCompacted = won.WONMSG.prefix + ":hasReceiverNeed";
won.WONMSG.hasReceiver = won.WONMSG.baseUri + "hasReceiver";
won.WONMSG.hasReceiverCompacted = won.WONMSG.prefix + ":hasReceiver";
won.WONMSG.hasReceiverNode = won.WONMSG.baseUri + "hasReceiverNode";
won.WONMSG.hasReceiverNodeCompacted = won.WONMSG.prefix + ":hasReceiverNode";
won.WONMSG.hasReceiverFacet = won.WONMSG.baseUri + "hasReceiverFacet";
won.WONMSG.hasReceiverFacetCompacted = won.WONMSG.prefix + ":hasReceiverFacet";
won.WONMSG.hasSenderNeed = won.WONMSG.baseUri + "hasSenderNeed";
won.WONMSG.hasSenderNeedCompacted = won.WONMSG.prefix + ":hasSenderNeed";
won.WONMSG.hasSender = won.WONMSG.baseUri + "hasSender";
won.WONMSG.hasSenderCompacted = won.WONMSG.prefix + ":hasSender";
won.WONMSG.hasSenderNode = won.WONMSG.baseUri + "hasSenderNode";
won.WONMSG.hasSenderNodeCompacted = won.WONMSG.prefix + ":hasSenderNode";
won.WONMSG.hasSenderFacet = won.WONMSG.baseUri + "hasSenderFacet";
won.WONMSG.hasSenderFacetCompacted = won.WONMSG.prefix + ":hasSenderFacet";
won.WONMSG.hasMessageType = won.WONMSG.baseUri + ":hasMessageType";
won.WONMSG.hasMessageTypeCompacted = won.WONMSG.prefix + ":hasMessageType";
won.WONMSG.hasTimestamp = won.WONMSG.baseUri + "hasTimestamp";
won.WONMSG.hasTimestampCompacted = won.WONMSG.prefix + ":hasTimestamp";
won.WONMSG.refersTo = won.WONMSG.baseUri + "refersTo";
won.WONMSG.refersToCompacted = won.WONMSG.prefix + ":refersTo";
won.WONMSG.isResponseTo = won.WONMSG.baseUri + "isResponseTo";
won.WONMSG.isResponseToCompacted = won.WONMSG.prefix + ":isResponseTo";
won.WONMSG.isRemoteResponseTo = won.WONMSG.baseUri + "isRemoteResponseTo";
won.WONMSG.isRemoteResponseToCompacted =
  won.WONMSG.prefix + ":isRemoteResponseTo";
won.WONMSG.EnvelopeGraph = won.WONMSG.baseUri + "EnvelopeGraph";
won.WONMSG.EnvelopeGraphCompacted = won.WONMSG.prefix + ":EnvelopeGraph";

won.WONMSG.hasContent = won.WONMSG.baseUri + "hasContent";
won.WONMSG.hasContentCompacted = won.WONMSG.prefix + ":hasContent";

won.WONMSG.FromOwner = won.WONMSG.baseUri + "FromOwner";
won.WONMSG.FromOwnerCompacted = won.WONMSG.prefix + ":FromOwner";
won.WONMSG.FromExternal = won.WONMSG.baseUri + "FromExternal";
won.WONMSG.FromSystem = won.WONMSG.baseUri + "FromSystem";

//message types
won.WONMSG.createMessage = won.WONMSG.baseUri + "CreateMessage";
won.WONMSG.createMessageCompacted = won.WONMSG.prefix + ":CreateMessage";
won.WONMSG.activateNeedMessage = won.WONMSG.baseUri + "ActivateMessage";
won.WONMSG.activateNeedMessageCompacted =
  won.WONMSG.prefix + ":ActivateMessage";
won.WONMSG.deleteNeedMessage = won.WONMSG.baseUri + "DeleteMessage";
won.WONMSG.deleteNeedMessageCompacted = won.WONMSG.prefix + ":DeleteMessage";
won.WONMSG.deleteNeedSentMessage = won.WONMSG.baseUri + "DeleteSentMessage";
won.WONMSG.deleteNeedSentMessageCompacted =
  won.WONMSG.prefix + ":DeleteSentMessage";
won.WONMSG.closeNeedMessage = won.WONMSG.baseUri + "DeactivateMessage";
won.WONMSG.closeNeedMessageCompacted = won.WONMSG.prefix + ":DeactivateMessage";
won.WONMSG.closeNeedSentMessage = won.WONMSG.baseUri + "DeactivateSentMessage";
won.WONMSG.closeNeedSentMessageCompacted =
  won.WONMSG.prefix + ":DeactivateSentMessage";
won.WONMSG.hintMessage = won.WONMSG.baseUri + "HintMessage";
won.WONMSG.hintMessageCompacted = won.WONMSG.prefix + ":HintMessage";
won.WONMSG.hintFeedbackMessage = won.WONMSG.baseUri + "HintFeedbackMessage";
won.WONMSG.hintFeedbackMessageCompacted =
  won.WONMSG.prefix + ":HintFeedbackMessage";
won.WONMSG.connectMessage = won.WONMSG.baseUri + "ConnectMessage";
won.WONMSG.connectMessageCompacted = won.WONMSG.prefix + ":ConnectMessage";
won.WONMSG.connectSentMessage = won.WONMSG.baseUri + "ConnectSentMessage";
won.WONMSG.connectSentMessageCompacted =
  won.WONMSG.prefix + ":ConnectSentMessage";
won.WONMSG.needStateMessage = won.WONMSG.baseUri + "NeedStateMessage";
won.WONMSG.needStateMessageCompacted = won.WONMSG.prefix + ":NeedStateMessage";
won.WONMSG.closeMessage = won.WONMSG.baseUri + "CloseMessage";
won.WONMSG.closeMessageCompacted = won.WONMSG.prefix + ":CloseMessage";
won.WONMSG.openMessage = won.WONMSG.baseUri + "OpenMessage";
won.WONMSG.feedbackMessage = won.WONMSG.baseUri + "HintFeedbackMessage";
won.WONMSG.openMessageCompacted = won.WONMSG.prefix + ":OpenMessage";
won.WONMSG.openSentMessage = won.WONMSG.baseUri + "OpenSentMessage";
won.WONMSG.openSentMessageCompacted = won.WONMSG.prefix + ":OpenSentMessage";
won.WONMSG.connectionMessage = won.WONMSG.baseUri + "ConnectionMessage";
won.WONMSG.connectionMessageCompacted =
  won.WONMSG.prefix + ":ConnectionMessage";
won.WONMSG.connectionMessageSentMessage =
  won.WONMSG.baseUri + "ConnectionMessageSentMessage";
won.WONMSG.connectionMessageSentMessageCompacted =
  won.WONMSG.prefix + ":ConnectionMessageSentMessage";
won.WONMSG.connectionMessageReceivedMessage =
  won.WONMSG.baseUri + "ConnectionMessageReceivedMessage";
won.WONMSG.connectionMessageReceivedMessageCompacted =
  won.WONMSG.prefix + ":ConnectionMessageReceivedMessage";

//response types
won.WONMSG.successResponse = won.WONMSG.baseUri + "SuccessResponse";
won.WONMSG.successResponseCompacted = won.WONMSG.prefix + ":SuccessResponse";
won.WONMSG.failureResponse = won.WONMSG.baseUri + "FailureResponse";
won.WONMSG.failureResponseCompacted = won.WONMSG.prefix + ":FailureResponse";

won.EVENT = {};
won.EVENT.WON_MESSAGE_RECEIVED = "WonMessageReceived";
won.EVENT.WON_SEARCH_RECEIVED = "SearchReceivedEvent";
won.EVENT.NEED_CREATED = "NeedCreatedEvent";
won.EVENT.HINT_RECEIVED = "HintReceivedEvent";
won.EVENT.CONNECT_SENT = "ConnectSentEvent";
won.EVENT.CONNECT_RECEIVED = "ConnectReceivedEvent";
won.EVENT.OPEN_SENT = "OpenSentEvent";
won.EVENT.ACTIVATE_NEED_SENT = "ActivateNeedSentEvent";
won.EVENT.ACTIVATE_NEED_RECEIVED = "ActivateNeedReceivedEvent";
won.EVENT.CLOSE_NEED_SENT = "DeactivateSentEvent";
won.EVENT.CLOSE_NEED_RECEIVED = "Deactivate_Received_Event";
won.EVENT.OPEN_RECEIVED = "OpenReceivedEvent";
won.EVENT.CLOSE_SENT = "CloseSentEvent";
won.EVENT.CLOSE_RECEIVED = "CloseReceivedEvent";
won.EVENT.CONNECTION_MESSAGE_RECEIVED = "ConnectionMessageReceivedEvent";
won.EVENT.CONNECTION_MESSAGE_SENT = "ConnectionMessageSentEvent";
won.EVENT.NEED_STATE_MESSAGE_RECEIVED = "NeedStateMessageReceivedEvent";
won.EVENT.NO_CONNECTION = "NoConnectionErrorEvent";
won.EVENT.NOT_TRANSMITTED = "NotTransmittedErrorEvent";
won.EVENT.USER_SIGNED_IN = "UserSignedInEvent";
won.EVENT.USER_SIGNED_OUT = "UserSignedOutEvent";
//TODO: this temp event, before we find out how to deal with session timeout
won.EVENT.WEBSOCKET_CLOSED_UNEXPECTED = "WebSocketClosedUnexpected";

won.EVENT.APPSTATE_CURRENT_NEED_CHANGED = "AppState.CurrentNeedChangedEvent";

//keys for things that can be shown in the GUI as 'unread'
won.UNREAD = {};
won.UNREAD.TYPE = {};
won.UNREAD.TYPE.CREATED = "created";
won.UNREAD.TYPE.HINT = "hint";
won.UNREAD.TYPE.MESSAGE = "message";
won.UNREAD.TYPE.CONNECT = "connect";
won.UNREAD.TYPE.CLOSE = "close";
won.UNREAD.TYPES = [
  won.UNREAD.TYPE.CREATED,
  won.UNREAD.TYPE.HINT,
  won.UNREAD.TYPE.MESSAGE,
  won.UNREAD.TYPE.CONNECT,
  won.UNREAD.TYPE.CLOSE,
];
won.UNREAD.GROUP = {};
won.UNREAD.GROUP.ALL = "all";
won.UNREAD.GROUP.BYNEED = "byNeed";

//Code definitions as enum in RestStatusResponse.java
won.RESPONSECODE = Object.freeze({
  USER_CREATED: 1200,
  USER_TRANSFERRED: 1201,
  USER_SIGNED_OUT: 1202,
  USER_ANONYMOUSLINK_SENT: 1203,
  USER_NOT_FOUND: 1400,
  USER_NOT_SIGNED_IN: 1401,
  USER_BAD_CREDENTIALS: 1402,
  USER_NOT_VERIFIED: 1403,
  USERNAME_MISMATCH: 1404,
  USER_ALREADY_EXISTS: 1405,
  TRANSFERUSER_NOT_FOUND: 2400,
  TRANSFERUSER_ALREADY_EXISTS: 2401,
  TOKEN_VERIFICATION_SUCCESS: 3200,
  TOKEN_RESEND_SUCCESS: 3201,
  TOKEN_NOT_FOUND: 3400,
  TOKEN_CREATION_FAILED: 3401,
  TOKEN_EXPIRED: 3403,
  TOKEN_RESEND_FAILED_ALREADY_VERIFIED: 3404,
  TOKEN_RESEND_FAILED_USER_ANONYMOUS: 3405,
  SIGNUP_FAILED: 4400,
  SETTINGS_CREATED: 5200,
  TOS_ACCEPT_SUCCESS: 6200,
  EXPORT_SUCCESS: 7200,
  EXPORT_IS_ANONYMOUS: 7403,

  PRIVATEID_NOT_FOUND: 666, //this one is not defined in RestStatusResponse.java
});

//we need to define this error here because we will not retrieve it from the rest endpoint
won.PRIVATEID_NOT_FOUND_ERROR = Object.freeze({
  code: 666,
  message: "invalid privateId",
});

/**
 * type of latest message for a connection in a given state.
 */
won.cnctState2MessageType = Object.freeze({
  [won.WON.Suggested]: won.WONMSG.hintMessage,
  [won.WON.RequestReceived]: won.WONMSG.connectMessage,
  [won.WON.RequestSent]: won.WONMSG.connectSentMessage,
  [won.WON.Connected]: won.WONMSG.connectionMessage,
  [won.WON.Closed]: won.WONMSG.closeMessage,
});

won.messageType2EventType = {
  [won.WONMSG.hintMessageCompacted]: won.EVENT.HINT_RECEIVED,
  [won.WONMSG.connectMessageCompacted]: won.EVENT.CONNECT_RECEIVED,
  [won.WONMSG.connectSentMessageCompacted]: won.EVENT.CONNECT_SENT,
  [won.WONMSG.openMessageCompacted]: won.EVENT.OPEN_RECEIVED,
  [won.WONMSG.closeMessageCompacted]: won.EVENT.CLOSE_RECEIVED,
  [won.WONMSG.closeNeedMessageCompacted]: won.EVENT.CLOSE_NEED_RECEIVED,
  [won.WONMSG.connectionMessageCompacted]:
    won.EVENT.CONNECTION_MESSAGE_RECEIVED,
  [won.WONMSG.needStateMessageCompacted]: won.EVENT.NEED_STATE_MESSAGE_RECEIVED,
  [won.WONMSG.errorMessageCompacted]: won.EVENT.NOT_TRANSMITTED,
};

//UTILS
won.WONMSG.uriPlaceholder = Object.freeze({
  event: "this:eventuri",
});

won.WON.contentNodeBlankUri = Object.freeze({
  seeks: "_:seeksNeedContent",
});

/**
 * Compacts the passed string if possible.
 * e.g. "http://purl.org/webofneeds/model#Demand" -> "won:Demand"
 * @param {*} longValue
 * @param {*} context
 */
won.toCompacted = function(longValue, context = won.defaultContext) {
  if (!longValue) return;
  for (let k in context) {
    if (longValue.startsWith(context[k])) {
      return longValue.replace(context[k], k + ":"); // replace first occurance
    }
  }
  return longValue;
};

won.clone = function(obj) {
  if (obj === undefined) return undefined;
  else return JSON.parse(JSON.stringify(obj));
};

/**
 * Copies all arguments properties recursively into a
 * new object and returns that.
 */

won.merge = function(/*args...*/) {
  const o = {};
  for (const argument of arguments) {
    won.mergeIntoLast(argument, o);
  }
  return o;
};
/*
 * Recursively merge properties of several objects
 * Copies all properties from the passed objects into the last one starting
 * from the left (thus the further right, the higher the priority in
 * case of name-clashes)
 * You might prefer this function over won.merge for performance reasons
 * (e.g. if you're copying into a very large object). Otherwise the former
 * is recommended.
 * @param args merges all passed objects onto the first passed
 */
won.mergeIntoLast = function(/*args...*/) {
  let obj1;
  for (const argument of arguments) {
    obj1 = arguments[arguments.length - 1];
    const obj2 = argument;
    for (const p in obj2) {
      try {
        // Property in destination object set; update its value.
        if (obj2[p].constructor == Object) {
          obj1[p] = won.mergeRecursive(obj1[p], obj2[p]);
        } else {
          obj1[p] = obj2[p];
        }
      } catch (e) {
        // Property in destination object not set; create it and set its value.
        obj1[p] = obj2[p];
      }
    }
  }
  return obj1;
};

won.lookup = lookup;
/**
 * Traverses a path of properties over the object, where the folllowing holds:
 *
 *     o.propA[1].moreprop === lookup(o, ['propA', 1, 'moreprop'])
 *
 * @param o
 * @param propertyPath
 * @returns {*}
 */
function lookup(o, propertyPath) {
  //TODO this should be in a utils file
  if (!o || !propertyPath) {
    return undefined;
  }
  const resolvedStep = o[propertyPath[0]];
  if (propertyPath.length === 1) {
    return resolvedStep;
  } else {
    return lookup(resolvedStep, propertyPath.slice(1));
  }
}

//get the URI from a jsonld resource (expects an object with an '@id' property)
//or the value from a typed literal
won.getSafeJsonLdValue = function(dataItem) {
  if (dataItem == null) return null;
  if (typeof dataItem === "object") {
    if (dataItem["@id"]) return dataItem["@id"];
    if (dataItem["@value"]) return dataItem["@value"];
  } else {
    return dataItem;
  }
  return null;
};

won.getLocalName = function(uriOrQname) {
  if (uriOrQname == null || typeof uriOrQname !== "string") return null;
  //first, try to get the URI hash fragment (without hash)
  let pos = uriOrQname.lastIndexOf("#");
  if (pos > -1 && pos < uriOrQname.length) {
    return uriOrQname.substring(pos + 1);
  }
  //try portion after last trailing slash
  pos = uriOrQname.lastIndexOf("/");
  if (pos > -1 && pos < uriOrQname.length) {
    return uriOrQname.substring(pos + 1);
  }
  //take portion after last ':'
  pos = uriOrQname.lastIndexOf(":");
  if (pos > -1 && pos < uriOrQname.length) {
    return uriOrQname.substring(pos + 1);
  }
  return uriOrQname;
};

won.isJsonLdKeyword = function(propertyName) {
  if (propertyName == null || typeof propertyName !== "string") return false;
  return propertyName.indexOf("@") == 0;
};

won.reportError = function(message) {
  if (arguments.length == 1) {
    return function(reason) {
      console.error(message, " reason: ", reason);
    };
  } else {
    return function(reason) {
      console.error("Error! reason: ", reason);
    };
  }
};

won.isNull = function(value) {
  return typeof value === "undefined" || value == null;
};

//helper function: is x an array?
won.isArray = function(x) {
  return Object.prototype.toString.call(x) === "[object Array]";
};

won.replaceRegExp = function(string) {
  return string.replace(/([.*+?^=!:${}()|[\]/\\])/g, "\\$1");
};

/**
 * Deletes every element of the array for which the
 * test function returns true.
 * @param array
 * @param test
 */
won.deleteWhere = function(array, test) {
  array.filter(entry => !test(entry));
};

won.containsAll = function(array, subArray) {
  for (const skey in subArray) {
    let found = false;
    for (const key in array) {
      if (subArray[skey] === array[key]) {
        found = true;
        break;
      }
    }
    if (found == false) return false;
  }
  return true;
};

/**
 * Deletes all null entries in the specified array.
 * @param array
 */
won.deleteWhereNull = function(array) {
  return won.deleteWhere(array, function(x) {
    return x == null;
  });
};

/**
 * Visits the specified data structure. For each element, the callback is called
 * as callback(element, key, container) where the key is the key of the element
 * in its container or callback (element, null, null) if there is no such container).
 */
won.visitDepthFirst = function(data, callback, currentKey, currentContainer) {
  if (data == null) return;
  if (won.isArray(data) && data.length > 0) {
    for (let key in data) {
      won.visitDepthFirst(data[key], callback, key, data);
    }
    return;
  }
  if (typeof data === "object") {
    for (let key in data) {
      won.visitDepthFirst(data[key], callback, key, data);
    }
    return;
  }
  //not a container: visit value.
  callback(data, currentKey, currentContainer);
};

/**
 * Adds all elements of array2 to array1 that are not yet
 * contained in array1.
 * If both arrays are non-null, array1 is modified and returned.
 * If either one of the two is null/undefined, the other is returned
 *
 * @param array1
 * @param array2
 * @param comparatorFun (optional) comparator function to compare elements. A return value of 0 means the elements are equal.
 */
won.appendStrippingDuplicates = function(array1, array2, comparatorFun) {
  if (typeof array1 === "undefined") return array2;
  if (typeof array2 === "undefined") return array1;
  if (typeof comparatorFun === "undefined")
    comparatorFun = function(a, b) {
      return a === b;
    };
  array2
    .filter(function(item) {
      for (const entry of array1) {
        if (comparatorFun(item, entry) == 0) {
          return false;
        }
      }
      return true;
    })
    .map(function(item) {
      array1.push(item);
    });
  return array1;
};

function context2ttlPrefixes(jsonldContext) {
  return Object.entries(jsonldContext)
    .filter(([, uri]) => is("String", uri))
    .map(([prefix, uri]) => `@prefix ${prefix}: <${uri}>.`)
    .join("\n");
}

won.minimalContext = {
  msg: "http://purl.org/webofneeds/message#",
  won: "http://purl.org/webofneeds/model#",
  rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
  agr: "http://purl.org/webofneeds/agreement#",
  pay: "http://purl.org/webofneeds/payment#",
  gr: "http://purl.org/goodrelations/v1#",
  wf: "http://purl.org/webofneeds/workflow#",
  rdfg: "http://www.w3.org/2004/03/trix/rdfg-1/",
};
won.minimalTurtlePrefixes = context2ttlPrefixes(won.minimalContext);

won.defaultContext = {
  ...won.minimalContext,
  webID: "http://www.example.com/webids/",
  dc: "http://purl.org/dc/elements/1.1/",
  rdfs: "http://www.w3.org/2000/01/rdf-schema#",
  geo: "http://www.w3.org/2003/01/geo/wgs84_pos#",
  xsd: "http://www.w3.org/2001/XMLSchema#",
  gr: "http://purl.org/goodrelations/v1#",
  ldp: "http://www.w3.org/ns/ldp#",
  sig: "http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#",
  sioc: "http://rdfs.org/sioc/ns#",
  dct: "http://purl.org/dc/terms/",
  cert: "http://www.w3.org/ns/auth/cert#",
  woncrypt: "http://purl.org/webofneeds/woncrypt#",
  s: "http://schema.org/",
  sh: "http://www.w3.org/ns/shacl#",
  foaf: "http://xmlns.com/foaf/0.1/",
  "msg:hasMessageType": {
    "@id": "http://purl.org/webofneeds/message#hasMessageType",
    "@type": "@id",
  },
};
/** ttl-prefixes e.g. `@prefix msg: <http://purl.org/webofneeds/message#>.\n @prefix...` */
won.defaultTurtlePrefixes = context2ttlPrefixes(won.defaultContext);

won.JsonLdHelper = {
  /**
   * Returns all graph URIs. If none are found, an empty array is returned.
   * @returns {Array}
   */
  getGraphNames: function(data) {
    //collect graph URIs in the specified dataset
    const graphs = data["@graph"];
    const graphURIs = [];
    if (graphs == null) {
      return graphURIs;
    }
    if (won.isArray(graphs) && graphs.length > 0) {
      for (const graph of graphs) {
        const graphURI = graph["@id"];
        if (graphURI != null) {
          graphURIs.push(graphURI);
        }
      }
    } else if (typeof graphs === "object" && graphs["@graph"] != null) {
      return won.JsonLdHelper.getGraphNames(graphs["@graph"]);
    }
    return graphURIs;
  },

  getDefaultGraph: function(data) {
    if (data["@graph"] != null) {
      //graph keyword is present. It could represent the default graph
      // (in which case it contains only nodes) or a collection of
      //, which are nodes that contain th @graph keyword.
      //our naive test is: if the first node contains an '@graph' keyword
      //we assume the outermost @graph array to contain only named graphs.
      // we search for the one with '@id'='@default' or without
      // an '@id' keyword and return it (if we find it)
      //if the first node doesn't contain an @graph keyword, we assume that there
      //are no named graphs and all data is in the default graph.
      let outermostGraphContent = data["@graph"];
      for (const curNode of outermostGraphContent) {
        if (curNode["@graph"] == null) {
          //we assume there are no named graphs, the outermost graph is the default graph
          return outermostGraphContent;
        }
        if (curNode["@id"] == null || curNode["@id"] === "@default") {
          //we've found the named graph without an @id attribute - that's the default graph
          return curNode["@graph"];
        }
      }
      return null; //no default graph found
    } else {
      //there is no @graph keyword at top level:
      return data;
    }
  },
  getNamedGraph: function(data, graphName) {
    if (data["@graph"] != null) {
      if (data["@id"] != null) {
        if (data["@id"] === graphName) {
          return data["@graph"];
        }
      } else {
        //outermost node has '@graph' but no '@id'
        //--> @graph array contains named graphs. search for name.
        let outermostGraphContent = data["@graph"];
        for (const curNode of outermostGraphContent) {
          if (curNode["@id"] == null || curNode["@id"] === graphName) {
            //we've found the named graph without an @id attribute - that's the default graph
            return curNode["@graph"];
          }
        }
      }
    }
    return null;
  },
  getNodeInGraph: function(data, graphName, nodeId) {
    const graph = this.getNamedGraph(data, graphName);
    for (let key in graph["@graph"]) {
      const curNode = graph["@graph"][key];
      const curNodeId = curNode["@id"];
      if (curNodeId === nodeId) {
        return curNode;
      }
    }
    return null;
  },
  addDataToNode: function(data, graphName, nodeId, predicate, object) {
    const node = this.getNodeInGraph(data, graphName, nodeId);
    if (node != null) {
      node[predicate] = object;
    }
  },
  getContext: function(data) {
    return data["@context"];
  },
};

/**
 *  Adds a msg:hasContent triple for each specified graphURI into the message graph
 * @param messageGraph
 * @param graphURIs
 */
won.addContentGraphReferencesToMessageGraph = function(
  messageGraph,
  graphURIs
) {
  if (graphURIs != null) {
    if (won.isArray(graphURIs) && graphURIs.length > 0) {
      //if the message graph already contains content references, fetch them:
      const existingContentRefs =
        messageGraph["@graph"][0][won.WONMSG.hasContentCompacted];
      const contentGraphURIs =
        typeof existingContentRefs === "undefined" ||
        !isArray(existingContentRefs)
          ? []
          : existingContentRefs;
      for (const graphURI of graphURIs) {
        contentGraphURIs.push({ "@id": graphURI });
      }
      messageGraph["@graph"][0][
        won.WONMSG.hasContentCompacted
      ] = contentGraphURIs;
    }
  }
};

/**
 * Adds the message graph to the json-ld structure 'builder.data' with
 * the specified messageType
 * and adds all specified graph URIs (which must be URIs of the
 * graphs to be added as content of the message) with triples
 * [message] wonmsg:hasContent [graphURI]
 * @param graphURIs
 * @returns {won.CreateMessageBuilder}
 */
won.addMessageGraph = function(builder, graphURIs, messageType) {
  let graphs = builder.data["@graph"];
  let unsetMessageGraphUri = won.WONMSG.uriPlaceholder.event + "#data";
  //create the message graph, containing the message type
  const messageGraph = {
    "@graph": [
      {
        "@id": won.WONMSG.uriPlaceholder.event,
        "msg:hasMessageType": { "@id": messageType },
      },
      {
        "@id": unsetMessageGraphUri,
        "@type": "msg:EnvelopeGraph",
        "rdfg:subGraphOf": { "@id": won.WONMSG.uriPlaceholder.event },
      },
    ],
    "@id": unsetMessageGraphUri,
  };
  won.addContentGraphReferencesToMessageGraph(messageGraph, graphURIs);
  //add the message graph to the graphs of the builder
  graphs.push(messageGraph);
  //point to the messagegraph so we can later access it easily for modifications
  builder.messageGraph = messageGraph;
};

/*
 * Creates a JSON-LD stucture containing a named graph with default 'unset' event URI
 * plus the specified hashFragment
 *
 */
won.newGraph = function(hashFragement) {
  hashFragement = hashFragement || "graph1";
  return {
    "@graph": [
      {
        "@id": won.WONMSG.uriPlaceholder.event + "#" + hashFragement,
        "@graph": [],
      },
    ],
  };
};

/**
 *  Work in progress: for generating any number/structure of domain objects
 *  (need, connection container, connection ,event container, event (=wonMessage)
 *  from json-ld
 *
 */

won.WonDomainObjects = function() {};

won.WonDomainObjects.prototype = {
  constructor: won.WonDomainObjects,
  /**
   * Returns the needURIs.
   */
  getNeedUris: function() {},
  /**
   * Returns the connection URIs.
   */
  getConnectionUris: function() {},
  /**
   * Returns the event URIs.
   */
  getEventUris: function() {},
  /**
   * Returns the domain object with the specified URI.
   * @param uri
   */
  getDomainObject: function(/*uri*/) {},
};

won.DomainObjectFactory = function() {};

won.DomainObjectFactory.prototype = {
  constructor: won.DomainObjectFactory,
  /**
   * Generates domain objects with the specified JSON-LD content. Returns a WonDomainObjects
   * instance containing all domain objects found in the JSON-LD content.
   */
  jsonLdToWonDomainObjects: function(/*jsonLdContent*/) {},
};

won.wonMessageFromJsonLd = async function(wonMessageAsJsonLD) {
  const expandedJsonLd = await jsonld.promises.expand(wonMessageAsJsonLD);
  const wonMessage = new WonMessage(expandedJsonLd);

  await wonMessage.frameInPromise();
  await wonMessage.generateContentGraphTrig();
  await wonMessage.generateCompactedFramedMessage();
  await wonMessage.generateContainedForwardedWonMessages();

  if (wonMessage.hasParseErrors() && !wonMessage.isResponse()) {
    console.warn(
      "wonMessage<",
      wonMessage.getMessageUri(),
      "> msgType<",
      wonMessage.getMessageType(),
      "> ParseError: {" + wonMessage.parseErrors,
      "} wonMessage: ",
      wonMessage
    );
  }
  if (wonMessage.hasContainedForwardedWonMessages()) {
    console.debug(
      "wonMessage<",
      wonMessage.getMessageUri(),
      "> msgType<",
      wonMessage.getMessageType(),
      "> contains forwardedMessages wonMessage: ",
      wonMessage
    );
    wonMessage.getContainedForwardedWonMessages().map(forwardedWonMessage => {
      console.debug(
        "-- forwardedMessage<",
        forwardedWonMessage.getMessageUri(),
        "> msgType<",
        forwardedWonMessage.getMessageType(),
        ">, forwardedWonMessage: ",
        forwardedWonMessage
      );
    });
  }

  return wonMessage;
};

/**
 * Serializes the jsonldData into trig.
 *
 * @param {*} jsonldData
 * @param {*} addDefaultContext whether or not `won.defaultContext` should be
 *   used for shortening urls (in addition to any `@context` at the root of
 *   `jsonldData` that's always used.)
 */
won.jsonLdToTrig = async function(jsonldData, addDefaultContext = true) {
  const quadString = await jsonld.promises.toRDF(jsonldData, {
    format: "application/nquads",
  });
  const { quads } = await won.n3Parse(quadString, {
    format: "application/n-quads",
  });

  const prefixes_ = addDefaultContext
    ? Object.assign(clone(won.defaultContext), jsonldData["@context"])
    : jsonldData["@context"] || {};
  const trig = await won.n3Write(quads, {
    format: "application/trig",
    prefixes: prefixes_,
  });

  return trig;
};
window.jsonLdToTrig4dbg = won.jsonLdToTrig;

/**
 * An wrapper for N3's writer that returns a promise
 * @param {*} quads list of quads following the rdfjs interface (http://rdf.js.org/)
 *   e.g.:
 * ```
 *  [ Quad {
 *      graph: DefaultGraph {id: ""}
 *      object: NamedNode {id: "http://example.org/cartoons#Cat"}
 *      predicate: NamedNode {id: "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"}
 *      subject: NamedNode {id: "http://example.org/cartoons#Tom"}
 * }, Quad {...}, ...]
 * ```
 * See here for ways to create them using N3: https://www.npmjs.com/package/n3#creating-triplesquads
 * @param {*} writerArgs the arguments for intializing the writer.
 *   e.g. `{format: 'application/trig'}`. See the writer-documentation
 *   (https://github.com/RubenVerborgh/N3.js#writing) for more details.
 */
won.n3Write = async function(quads, writerArgs) {
  //const { namedNode, literal, defaultGraph, quad } = N3.DataFactory;
  const writer = N3.Writer(writerArgs);
  return new Promise((resolve, reject) => {
    //quads.forEach(t => writer.addQuad(t))
    writer.addQuads(quads);
    writer.end((error, result) => {
      if (error) reject(error);
      else resolve(result);
    });
  });
};

/**
 * A wrapper for N3's parse that returns a promise
 * @param {*} rdf a rdf-string to be parsed
 * @param {*} parserArgs arguments for initializing the parser,
 *   e.g. `{format: 'application/n-quads'}` if you want to make
 *   parser stricter about what it accepts. See the parser-documentation
 *   (https://github.com/RubenVerborgh/N3.js#parsing) for more details.
 */
won.n3Parse = async function(rdf, parserArgs) {
  const parser = parserArgs ? N3.Parser(parserArgs) : N3.Parser();
  return new Promise((resolve, reject) => {
    let quads = [];
    parser.parse(rdf, (error, quad, prefixes) => {
      if (error) {
        reject(error);
      } else if (quad) {
        quads.push(quad);
      } else {
        // all quads collected
        resolve({ quads, prefixes });
      }
    });
  });
};

/**
 *
 * @param {string} ttl
 * @param {boolean} prependWonPrefixes
 */
won.ttlToJsonLd = async function(ttl, prependWonPrefixes = true) {
  const ttl_ = prependWonPrefixes
    ? won.defaultTurtlePrefixes + "\n" + ttl
    : ttl;

  const tryConversion = async () => {
    const { quads /*prefixes*/ } = await won.n3Parse(ttl_);
    const placeholderGraphUri = "ignoredgraphuri:placeholder";

    // overwrite empty graphUri (ttl is just triples) with placeholder string
    quads.forEach(t => {
      t.graph.id = placeholderGraphUri;
    });

    const quadString = await won.n3Write(quads, {
      format: "application/n-quads",
    });

    const parsedJsonld = await jsonld.promises.fromRDF(quadString, {
      format: "application/nquads",
    });

    return parsedJsonld;
  };
  return tryConversion().catch(e => {
    e.message =
      "error while parsing the following turtle:\n\n" +
      ttl_ +
      "\n\n----\n\n" +
      e.message;
    throw e;
  });
};

window.ttlToJsonLd4dbg = won.ttlToJsonLd;

/**
 * Like the JSONLD-Helper, an object that wraps a won message and
 * offers convenience functions on it.
 * @param jsonLdContent
 * @constructor
 */
function WonMessage(jsonLdContent) {
  if (!(this instanceof WonMessage)) {
    return new WonMessage(jsonLdContent);
  }
  this.rawMessage = jsonLdContent;
  this.parseErrors = [];
  this.containedForwardedWonMessages = [];
  this.__init();
}

WonMessage.prototype = {
  constructor: WonMessage,

  getMessageUri: function() {
    return this.__getMessageUri(this.messageStructure);
  },

  getRemoteMessageUri: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/message#hasCorrespondingRemoteMessage"
    );
  },

  __getMessageUri: function(messageStructure) {
    if (messageStructure.messageUri) {
      return messageStructure.messageUri;
    }
    if (messageStructure.containedEnvelopes) {
      let uris = messageStructure.containedEnvelopes.map(envelope =>
        this.__getMessageUri(envelope)
      );
      if (uris.length > 1) {
        throw new Error(
          "Found more than one contained envelope in message with message uris: " +
            uris
        );
      }
      if (uris.length == 0) {
        throw new Error("Did not find any contained envelopes in message");
      }
      return uris[0];
    }
  },

  getMessageDirection: function() {
    return this.__getMessageDirection(this.messageStructure);
  },

  generateContentGraphTrig: async function() {
    if (this.contentGraphTrig) {
      return this.contentGraphTrig;
    }
    const contentGraphs = this.getContentGraphs();
    if (contentGraphs && contentGraphs.length > 0) {
      try {
        if (!is("Array", contentGraphs)) {
          throw new Error(
            "Unexpected content-graph structure: \n\n" +
              JSON.stringify(contentGraphs)
          );
        }
        const eventUriPrefix = prefixOfUri(this.getMessageUri());
        const jsonldData = {
          "@context": Object.assign(
            { event: eventUriPrefix },
            won.defaultContext
          ),
          "@graph": contentGraphs,
        };
        this.jsonldData = jsonldData;
        this.contentGraphTrig = await won.jsonLdToTrig(jsonldData);
        return this.contentGraphTrig;
      } catch (e) {
        const msg =
          "Failed to generate trig for message " +
          this.getMessageUri() +
          "\n\n" +
          e.message +
          "\n\n" +
          e.stack;
        console.error(msg);
        this.contentGraphTrigError = msg;
      }
    }
  },
  generateCompactedFramedMessage: async function() {
    //TODO: change it so it returns all the contentgraphscontent
    if (this.compactFramedMessage) {
      return this.compactFramedMessage;
    }
    if (this.framedMessage && this.rawMessage) {
      try {
        this.compactFramedMessage = await jsonld.compact(
          this.framedMessage,
          won.defaultContext
        );

        this.compactRawMessage = await jsonld.compact(
          this.rawMessage,
          won.defaultContext
        );

        return this.compactFramedMessage;
      } catch (e) {
        const msg =
          "Failed to generate jsonld for message " +
          this.getMessageUri() +
          "\n\n" +
          e.message +
          "\n\n" +
          e.stack;
        console.error(msg);
        this.compactFramedMessageError = msg;
      }
    }
  },
  generateContainedForwardedWonMessages: async function() {
    const forwardedMessageUris = this.getForwardedMessageUris();
    if (forwardedMessageUris && forwardedMessageUris.length == 1) {
      //TODO: RECURSIVELY CREATE wonMessageObjects from all the forwarded Messages within this message
      //const forwardedMessages = wonMessage.compactFramedMessage["msg:hasForwardedMessage"];
      const encapsulatingMessageUri = this.messageStructure.messageUri;
      const rawMessageWithoutEncapsulatingUri = this.rawMessage.filter(
        elem => !elem["@id"].startsWith(encapsulatingMessageUri)
      );

      /*console.debug(
        "WonMessage\n",
        this,
        "\nforwardedMessageUris:\n",
        forwardedMessageUris,
        "\nencapsulatingMessageUri\n",
        encapsulatingMessageUri,
        "\nrawMessageWithouthEncapsulatingUri\n",
        rawMessageWithoutEncapsulatingUri
      );*/

      const fwdMessage = await won.wonMessageFromJsonLd(
        rawMessageWithoutEncapsulatingUri
      );
      this.containedForwardedWonMessages.push(fwdMessage);

      return Promise.resolve(this.containedForwardedWonMessages);
    } else if (forwardedMessageUris) {
      this.parseErrors.push(
        "WonMessage contains more than one forwardedMessage on the same level: omitting forwardMessages"
      );
      return Promise.resolve(this.containedForwardedWonMessages);
    } else {
      return Promise.resolve(this.containedForwardedWonMessages);
    }
  },
  frameInPromise: function() {
    if (this.framedMessage) {
      return Promise.resolve(this.framedMessage);
    }
    const type = this.getMessageDirection();
    const that = this;
    return jsonld.promises
      .frame(this.rawMessage, {
        "@type": type,
      })
      .then(result => {
        const graphs = result["@graph"];
        if (graphs && graphs.length > 1) {
          const msgUri = that.getMessageUri();
          const msgGraphIndex = graphs.findIndex(
            elem => elem["@id"] === msgUri
          );
          if (msgGraphIndex != 0 && msgGraphIndex != -1) {
            let newGraphs = [];
            newGraphs.push(graphs[msgGraphIndex]);
            result["@graph"] = newGraphs.concat(
              graphs.filter(elem => elem["@id"] === msgUri)
            );

            that.framedMessage = result;
            return result;
          }
        }
        that.framedMessage = result;
        return result;
      });
  },

  __getFramedMessage: function() {
    return this.framedMessage;
  },

  getProperty: function(property) {
    let val = this.__getFramedMessage()["@graph"][0][property];
    if (val) {
      return this.__singleValueOrArray(val);
    }
    return this.getPropertyFromRemoteMessage(property);
  },

  getPropertyFromLocalMessage: function(property) {
    let val = this.__getFramedMessage()["@graph"][0][property];
    if (val) {
      return this.__singleValueOrArray(val);
    }
  },
  getPropertyFromRemoteMessage: function(property) {
    const remoteMessage = this.__getFramedMessage()["@graph"][0][
      "http://purl.org/webofneeds/message#hasCorrespondingRemoteMessage"
    ];
    if (remoteMessage) {
      let val = remoteMessage[property];
      if (val) {
        return this.__singleValueOrArray(val);
      }
    }
    return null;
  },

  __singleValueOrArray: function(val) {
    if (!val) return null;
    if (Array.isArray(val)) {
      if (val.length == 1) {
        return won.getSafeJsonLdValue(val);
      }
      return val.map(x => won.getSafeJsonLdValue(x));
    }
    return won.getSafeJsonLdValue(val);
  },
  getContentGraphs: function() {
    // walk over graphs, copy all graphs to result that are content graphs
    // we identify content graphs by finding their URI in messageStructure.containedContent
    return this.graphs.filter(
      graph => this.contentGraphUris.indexOf(graph["@id"]) > -1
    );
  },
  getContentGraphsAsJsonLD: function() {
    return JSON.stringify(this.getContentGraphs());
  },
  getCompactFramedMessageContent: function() {
    // Returns the compacted Framed Message depending on the message direction
    if (this.isFromOwner()) {
      return this.compactFramedMessage;
    } else {
      return this.compactFramedMessage["msg:hasCorrespondingRemoteMessage"];
    }
  },
  getCompactFramedForwardedMessageContent: function() {
    const forwardedMessage =
      this.compactFramedMessage &&
      this.compactFramedMessage["msg:hasForwardedMessage"];
    const forwardedMessageContent =
      forwardedMessage && forwardedMessage["msg:hasCorrespondingRemoteMessage"];
    return forwardedMessageContent;
  },
  getCompactRawMessage: function() {
    return this.compactRawMessage;
  },
  getMessageType: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/message#hasMessageType"
    );
  },
  getInjectIntoConnectionUris: function() {
    return createArray(
      this.getProperty(
        "http://purl.org/webofneeds/message#hasInjectIntoConnection"
      )
    );
  },
  getForwardedMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/message#hasForwardedMessage")
    );
  },
  getReceivedTimestamp: function() {
    return this.getPropertyFromLocalMessage(
      "http://purl.org/webofneeds/message#hasReceivedTimestamp"
    );
  },
  getSentTimestamp: function() {
    return this.getPropertyFromLocalMessage(
      "http://purl.org/webofneeds/message#hasSentTimestamp"
    );
  },
  /**
   * Returns the receivedTimestamp, which is the server timestamp. If that timestamp is not found in the message,
   * returns the sentTimestamp as a fallback.
   */
  getTimestamp: function() {
    const ts = this.getReceivedTimestamp();
    if (ts) {
      return ts;
    }
    return this.getSentTimestamp();
  },
  getTextMessage: function() {
    return this.getProperty("http://purl.org/webofneeds/model#hasTextMessage");
  },
  getMatchScore: function() {
    return this.getProperty("http://purl.org/webofneeds/model#hasMatchScore");
  },
  getMatchCounterpart: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/model#hasMatchCounterpart"
    );
  },
  getIsResponseTo: function() {
    return this.getProperty("http://purl.org/webofneeds/message#isResponseTo");
  },
  getIsRemoteResponseTo: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/message#isRemoteResponseTo"
    );
  },
  getIsResponseToMessageType: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/message#isResponseToMessageType"
    );
  },

  getSenderNode: function() {
    return this.getProperty("http://purl.org/webofneeds/message#hasSenderNode");
  },
  getSenderNeed: function() {
    return this.getProperty("http://purl.org/webofneeds/message#hasSenderNeed");
  },
  getSender: function() {
    return this.getProperty("http://purl.org/webofneeds/message#hasSender");
  },
  getReceiverNode: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/message#hasReceiverNode"
    );
  },
  getReceiverNeed: function() {
    return this.getProperty(
      "http://purl.org/webofneeds/message#hasReceiverNeed"
    );
  },
  getReceiver: function() {
    return this.getProperty("http://purl.org/webofneeds/message#hasReceiver");
  },

  getProposedMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/agreement#proposes")
    );
  },

  getClaimsMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/agreement#claims")
    );
  },

  getAcceptsMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/agreement#accepts")
    );
  },
  getProposedToCancelMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/agreement#proposesToCancel")
    );
  },
  getRejectsMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/agreement#rejects")
    );
  },
  getRetractsMessageUris: function() {
    return createArray(
      this.getProperty("http://purl.org/webofneeds/modification#retracts")
    );
  },

  isProposeMessage: function() {
    return !!this.getProperty("http://purl.org/webofneeds/agreement#proposes");
  },
  isAcceptMessage: function() {
    return !!this.getProperty("http://purl.org/webofneeds/agreement#accepts");
  },
  isProposeToCancel: function() {
    return !!this.getProperty(
      "http://purl.org/webofneeds/agreement#proposesToCancel"
    );
  },
  isProposal: function() {
    return !!this.getProperty("http://purl.org/webofneeds/agreement#Proposal");
  },
  isAgreement: function() {
    return !!this.getProperty("http://purl.org/webofneeds/agreement#Agreement");
  },

  isRejectMessage: function() {
    return !!this.getProperty("http://purl.org/webofneeds/agreement#rejects");
  },
  isRetractMessage: function() {
    return !!this.getProperty(
      "http://purl.org/webofneeds/modification#retracts"
    );
  },
  isOutgoingMessage: function() {
    return (
      this.isFromOwner() ||
      (this.isFromSystem() && this.getSender() !== this.getReceiver())
    );
  },
  hasContainedForwardedWonMessages: function() {
    return (
      this.containedForwardedWonMessages &&
      this.containedForwardedWonMessages.length > 0
    );
  },
  hasParseErrors: function() {
    return this.parseErrors && this.parseErrors.length > 0;
  },
  getContainedForwardedWonMessages: function() {
    return this.containedForwardedWonMessages;
  },
  isFromSystem: function() {
    let direction = this.getMessageDirection();
    return direction === "http://purl.org/webofneeds/message#FromSystem";
  },
  isFromOwner: function() {
    let direction = this.getMessageDirection();
    return direction === "http://purl.org/webofneeds/message#FromOwner";
  },
  isFromExternal: function() {
    let direction = this.getMessageDirection();
    return direction === "http://purl.org/webofneeds/message#FromExternal";
  },

  isHintMessage: function() {
    return (
      this.getMessageType() === "http://purl.org/webofneeds/message#HintMessage"
    );
  },
  isCreateMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#CreateMessage"
    );
  },
  isConnectMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#ConnectMessage"
    );
  },
  isOpenMessage: function() {
    return (
      this.getMessageType() === "http://purl.org/webofneeds/message#OpenMessage"
    );
  },
  isConnectionMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#ConnectionMessage"
    );
  },
  isCloseMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#CloseMessage"
    );
  },
  isHintFeedbackMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#HintFeedbackMessage"
    );
  },
  isActivateMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#ActivateMessage"
    );
  },
  isDeactivateMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#DeactivateMessage"
    );
  },
  isDeleteMessage: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#DeleteMessage"
    );
  },
  isNeedMessage: function() {
    return (
      this.getMessageType() === "http://purl.org/webofneeds/message#NeedMessage"
    );
  },
  isResponse: function() {
    return this.isSuccessResponse() || this.isFailureResponse();
  },
  isSuccessResponse: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#SuccessResponse"
    );
  },
  isFailureResponse: function() {
    return (
      this.getMessageType() ===
      "http://purl.org/webofneeds/message#FailureResponse"
    );
  },

  isResponseToHintMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#HintMessage"
    );
  },
  isResponseToCreateMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#CreateMessage"
    );
  },
  isResponseToConnectMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#ConnectMessage"
    );
  },
  isResponseToOpenMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#OpenMessage"
    );
  },
  isResponseToConnectionMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#ConnectionMessage"
    );
  },
  isResponseToCloseMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#CloseMessage"
    );
  },
  isResponseToHintFeedbackMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#HintFeedbackMessage"
    );
  },
  isResponseToActivateMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#ActivateMessage"
    );
  },
  isResponseToDeactivateMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#DeactivateMessage"
    );
  },
  isResponseToDeleteMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "http://purl.org/webofneeds/message#DeleteMessage"
    );
  },

  __getMessageDirection: function(messageStructure) {
    if (messageStructure.messageDirection) {
      return messageStructure.messageDirection;
    }
    if (messageStructure.containedEnvelopes) {
      let uris = messageStructure.containedEnvelopes.map(envelope =>
        this.__getMessageDirection(envelope)
      );
      if (uris.length > 1) {
        throw new Error(
          "Found more than one contained envelope in message with message uris: " +
            uris
        );
      }
      if (uris.length == 0) {
        throw new Error("Did not find any contained envelopes in message");
      }
      return uris[0];
    }
  },

  __init: function() {
    this.context = this.graphs = this.rawMessage;
    if (!Array.isArray(this.graphs)) {
      this.parseErrors.push("@graph not found or not an array");
    }
    this.graphUris = this.graphs.map(g => g["@id"]);
    if (!Array.isArray(this.graphUris)) {
      this.parseErrors.push("GraphUris not found or not an array");
    }
    const nodes = {};
    let unreferencedEnvelopes = [];
    const innermostEnvelopes = [];
    const contentGraphUris = [];
    //first pass: create one node per envelope/content graph
    this.graphs.forEach(graph => {
      let graphUri = graph["@id"];
      if (this.__isEnvelopeGraph(graph)) {
        let node = { uri: graphUri };
        unreferencedEnvelopes.push(graphUri);
        let msgUriAndDirection = this.__getMessageUriAndDirection(graph);
        if (msgUriAndDirection) {
          // it's possible that we don't find a triple <messageUri> a <type>
          // in the envelope, and we can't add it to the node here.
          node.messageUri = msgUriAndDirection.messageUri;
          node.messageDirection = msgUriAndDirection.messageDirection;
        }
        let messageUriAndCorrespondingRemoteMessageUri = this.__getMessageUriAndCorrespondingRemoteMessageUri(
          graph
        );
        if (messageUriAndCorrespondingRemoteMessageUri) {
          node.messageUri =
            messageUriAndCorrespondingRemoteMessageUri.messageUri;
          node.correspondingRemoteMessageUri =
            messageUriAndCorrespondingRemoteMessageUri.correspondingRemoteMessageUri;
        }
        let messageUriAndForwardedMessageUri = this.__getMessageUriAndForwardedMessageUri(
          graph
        );
        if (messageUriAndForwardedMessageUri) {
          node.forwardedMessageUri =
            messageUriAndForwardedMessageUri.forwardedMessageUri;
        }
        nodes[graphUri] = node;
      } else if (this.__isSignatureGraph(graph)) {
        //do nothing - we don't want to handle signatures in the client for now
      } else {
        //content graph
        nodes[graphUri] = { uri: graphUri };
      }
    });
    //second pass: connect the nodes so we get a tree
    this.graphs.forEach(graph => {
      let graphUri = graph["@id"];
      let node = nodes[graphUri];
      if (this.__isEnvelopeGraph(graph)) {
        let containedEnvelopes = this.__getContainedEnvelopeUris(graph);
        let referencesOtherGraphs = false;
        if (containedEnvelopes.length > 0) {
          referencesOtherGraphs = true;
          node.containsEnvelopes = containedEnvelopes.map(uri => nodes[uri]);
          //remember that these envelopes are now referenced
          unreferencedEnvelopes = unreferencedEnvelopes.filter(
            uri => !containedEnvelopes.includes(uri)
          );
        }
        if (node.correspondingRemoteMessageUri) {
          referencesOtherGraphs = true;
        }
        if (node.forwardedMessageUri) {
          referencesOtherGraphs = true;
        }
        if (!referencesOtherGraphs) {
          //remember that this envelope contains no envelopes (and points to no remote messages)
          innermostEnvelopes.push(graphUri);
        }
        if (node.messageUri) {
          //if we know the message uri, we can look for content in this envelope
          let containedContent = this.__getContainedContentGraphUris(
            graph,
            node.messageUri
          );
          if (containedContent.length > 0) {
            node.containedContent = containedContent.map(uri => nodes[uri]);
            //remember the content graphs
            containedContent.forEach(uri => contentGraphUris.push(uri));
          }
        }
      }
    });
    //now we should have the envelope inclusion trees for all messages
    //unreferencedEnvelopes now points to all roots.
    //walk over the roots and connect them via remoteMessage or forwardedMessage connections
    if (unreferencedEnvelopes.length > 1) {
      unreferencedEnvelopes.forEach(node => {
        if (node.correspondingRemoteMessageUri) {
          let remoteMessages = unreferencedEnvelopes.filter(
            envelope =>
              envelope.messageUri == node.correspondingRemoteMessageUri
          );
          if (remoteMessages.length == 1) {
            //we found a remote envelope. link to it from our node
            node.remoteEnvelope = remoteMessages[0];
            if (
              node.messageDirection ===
              "http://purl.org/webofneeds/message#FromExternal"
            ) {
              //both messages can link to each other, but the FromExternal one
              //is the top level one. mark the other one as referenced
              unreferencedEnvelopes = unreferencedEnvelopes.filter(
                env => env != node.remoteEnvelope
              );
            }
          } else if (remoteMessages.length > 1) {
            this.parseErrors.push(
              "more than one candidate for the outermost remoteMessage envelope found"
            );
          }
        }
      });
    }
    // if we still have more than 1 unreferenced envelope, it must be because there is
    // a forwarded message.
    if (unreferencedEnvelopes.length > 1) {
      // one more pass: we did not connect the message and the forwardedMessage so their
      // respective local and remote messages could be connected. now we connect
      // them and remove the forwarded message from the unreferenced list
      this.graphs.forEach(graph => {
        let graphUri = graph["@id"];
        let node = nodes[graphUri];
        if (node.forwardedMessageUri) {
          node.forwardedMessage = nodes[node.forwardedMessageUri];
          unreferencedEnvelopes = unreferencedEnvelopes.filter(
            uri => uri != node.forwardedMessageUri
          );
        }
      });
    }

    if (innermostEnvelopes.length == 0) {
      this.parseErrors.push("no innermost envelope found");
    }
    if (innermostEnvelopes.length > 1) {
      this.parseErrors.push("more than one innermost envelope found");
    }
    if (unreferencedEnvelopes.length == 0) {
      this.parseErrors.push("no unreferenced (i.e. outermost) envelope found");
    }
    if (unreferencedEnvelopes.length > 1) {
      this.parseErrors.push(
        "more than one unreferenced (i.e. outermost) envelope found"
      );
    }
    this.messageStructure = nodes[unreferencedEnvelopes[0]]; //set the pointer to the outermost envelope
    this.contentGraphUris = contentGraphUris;
  },

  __isEnvelopeGraph: graph => {
    let graphUri = graph["@id"];
    let graphData = graph["@graph"];
    return graphData.some(
      resource =>
        resource["@id"] === graphUri &&
        resource["@type"].includes(
          "http://purl.org/webofneeds/message#EnvelopeGraph"
        )
    );
  },
  __isSignatureGraph: graph => {
    let graphUri = graph["@id"];
    let graphData = graph["@graph"];
    return graphData.some(
      resource =>
        resource["@id"] === graphUri &&
        resource["@type"].includes(
          "http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#Signature"
        )
    );
  },
  __getContainedEnvelopeUris: graph => {
    let graphUri = graph["@id"];
    let graphData = graph["@graph"];
    let data = graphData
      .filter(resource => resource["@id"] == graphUri)
      .map(
        resource =>
          resource["http://purl.org/webofneeds/message#containsEnvelope"]
      )
      .filter(x => x);
    if (data.length > 0) {
      return data[0].map(x => x["@id"]);
    } else {
      return [];
    }
  },
  __getContainedContentGraphUris: (graph, messageUri) => {
    let graphData = graph["@graph"];
    const contentUrisArray = graphData
      .filter(resource => resource["@id"] === messageUri)
      .map(
        resource => resource["http://purl.org/webofneeds/message#hasContent"]
      )
      .filter(x => x);
    if (contentUrisArray.length > 0) {
      return contentUrisArray[0].map(x => x["@id"] || x["@value"]);
    } else {
      return [];
    }
  },
  __getMessageUriAndDirection: graph => {
    let graphData = graph["@graph"];
    let data = graphData
      .filter(
        resource =>
          resource["@type"].includes(
            "http://purl.org/webofneeds/message#FromExternal"
          ) ||
          resource["@type"].includes(
            "http://purl.org/webofneeds/message#FromOwner"
          ) ||
          resource["@type"].includes(
            "http://purl.org/webofneeds/message#FromSystem"
          )
      )
      .map(resource => ({
        messageUri: resource["@id"],
        messageDirection: resource["@type"][0], //@type is an array in expanded jsonld
      }))
      .filter(x => !!x); //if that property was not present, filter out undefineds
    if (Array.isArray(data)) {
      if (data.length == 0) {
        return null;
      }
      return data[0];
    }
    return data;
  },
  __getMessageUriAndCorrespondingRemoteMessageUri: graph => {
    let graphData = graph["@graph"];
    let data = graphData
      .filter(
        resource =>
          resource[
            "http://purl.org/webofneeds/message#hasCorrespondingRemoteMessage"
          ]
      )
      .map(resource => ({
        messageUri: resource["@id"],
        correspondingRemoteMessageUri:
          resource[
            "http://purl.org/webofneeds/message#hasCorrespondingRemoteMessage"
          ][0]["@id"],
      }))
      .filter(x => !!x); //if that property was not present, filter out undefineds
    if (Array.isArray(data)) {
      if (data.length == 0) {
        return null;
      }
      return data[0];
    }
    return data;
  },
  __getMessageUriAndForwardedMessageUri: graph => {
    let graphData = graph["@graph"];
    let data = graphData
      .filter(
        resource =>
          resource["http://purl.org/webofneeds/message#hasForwardedMessage"]
      )
      .map(resource => ({
        messageUri: resource["@id"],
        forwardedMessageUri:
          resource["http://purl.org/webofneeds/message#hasForwardedMessage"][0][
            "@id"
          ],
      }))
      .filter(x => !!x); //if that property was not present, filter out undefineds
    if (Array.isArray(data)) {
      if (data.length == 0) {
        return null;
      }
      return data[0];
    }
    return data;
  },
};

/**
 * Builds a JSON-LD WoN Message or adds the relevant data to the specified
 * JSON-LD data structure.
 * @param messageType a fully qualified URI
 * @param content a JSON-LD structure or null
 * @constructor
 */
won.MessageBuilder = function MessageBuilder(messageType, content) {
  if (messageType == null) {
    throw { message: "messageType must not be null!" };
  }
  let graphNames = null;
  if (content != null) {
    this.data = won.clone(content);
    graphNames = won.JsonLdHelper.getGraphNames(this.data);
  } else {
    this.data = {
      "@graph": [],
      "@context": won.clone(won.defaultContext),
    };
  }
  this.messageGraph = null;
  this.eventUriValue = won.WONMSG.uriPlaceholder.event;
  won.addMessageGraph(this, graphNames, messageType);
};

won.MessageBuilder.prototype = {
  constructor: won.MessageBuilder,

  eventURI: function(eventUri) {
    this.getContext()[won.WONMSG.EnvelopeGraphCompacted] = {
      "@id": "http://purl.org/webofneeds/message#EnvelopeGraph",
      "@type": "@id",
    };
    const regex = new RegExp(won.replaceRegExp(this.eventUriValue));
    won.visitDepthFirst(this.data, function(element, key, collection) {
      if (collection != null && key === "@id") {
        if (element) collection[key] = element.replace(regex, eventUri);
      }
    });
    this.eventUriValue = eventUri;
    return this;
  },
  getContext: function() {
    return this.data["@context"];
  },
  forEnvelopeData: function(envelopeData) {
    const node = this.getMessageEventNode();
    for (let key in envelopeData) {
      node[key] = { "@id": envelopeData[key] };
    }
    return this;
  },
  hasSenderNeed: function(senderNeedURI) {
    this.getMessageEventNode()[won.WONMSG.hasSenderNeedCompacted] = {
      "@id": senderNeedURI,
    };
    return this;
  },
  hasSenderNode: function(senderNodeURI) {
    this.getMessageEventNode()[won.WONMSG.hasSenderNodeCompacted] = {
      "@id": senderNodeURI,
    };
    return this;
  },
  hasSender: function(senderURI) {
    this.getMessageEventNode()[won.WONMSG.hasSenderCompacted] = {
      "@id": senderURI,
    };
    return this;
  },
  hasReceiver: function(receiverURI) {
    this.getMessageEventNode()[won.WONMSG.hasReceiverCompacted] = {
      "@id": receiverURI,
    };
    return this;
  },
  hasReceiverNeed: function(receiverNeedURI) {
    this.getMessageEventNode()[won.WONMSG.hasReceiverNeedCompacted] = {
      "@id": receiverNeedURI,
    };
    return this;
  },
  hasReceiverNode: function(receiverURI) {
    this.getMessageEventNode()[won.WONMSG.hasReceiverNodeCompacted] = {
      "@id": receiverURI,
    };
    return this;
  },
  hasOwnerDirection: function() {
    this.getMessageEventNode()["@type"] = won.WONMSG.FromOwnerCompacted;
    return this;
  },
  hasSentTimestamp: function(timestamp) {
    this.getMessageEventNode()["msg:hasSentTimestamp"] = timestamp;
    return this;
  },
  /**
   * Adds the specified facet as local facets. Only needed for connect and
   * openSuggested.
   * @param receiverURI
   * @returns {won.MessageBuilder}
   */
  hasFacet: function(facetURI) {
    this.getMessageEventNode()[won.WONMSG.hasSenderFacetCompacted] = {
      "@id": facetURI,
    };
    return this;
  },
  /**
   * Adds the specified facet as local facets. Only needed for connect and
   * openSuggested.
   * @param receiverURI
   * @returns {won.MessageBuilder}
   */
  hasRemoteFacet: function(facetURI) {
    this.getMessageEventNode()[won.WONMSG.hasReceiverFacetCompacted] = {
      "@id": facetURI,
    };
    return this;
  },
  /**
   * Adds the specified text as text message inside the content. Can be
   * used with connectMessage, openMessage and connectionMessage.
   * @param text - text of the message
   * @returns {won.MessageBuilder}
   */
  hasTextMessage: function(text) {
    if (text == null || text === "") {
      // text is either null, undefined, or empty
      // do nothing
    } else {
      this.getContentGraphNode()[won.WON.hasTextMessageCompacted] = text;
    }
    return this;
  },

  getMessageEventGraph: function() {
    return this.messageGraph;
  },
  getMessageEventNode: function() {
    return this.getMessageEventGraph()["@graph"][0];
  },
  /**
   * Fetches the content graph, creating it if it doesn't exist.
   */
  getContentGraph: function() {
    const graphs = this.data["@graph"];
    const contentGraphUri = this.eventUriValue + "#content";
    for (let key in graphs) {
      const graph = graphs[key];
      if (graph["@id"] === contentGraphUri) {
        return graph;
      }
    }
    //none found: create it
    const contentGraph = {
      "@id": this.eventUriValue + "#content",
      "@graph": [{ "@id": this.eventUriValue }],
    };
    graphs.push(contentGraph);
    //add a reference to it to the envelope
    won.addContentGraphReferencesToMessageGraph(this.messageGraph, [
      contentGraphUri,
    ]);
    return contentGraph;
  },
  getContentGraphNode: function() {
    return this.getContentGraph()["@graph"][0];
  },
  getContentGraphNodes: function() {
    return this.getContentGraph()["@graph"];
  },
  /**
   * takes a lists of json-ld-objects and merges them into the content-graph
   */
  mergeIntoContentGraph: function(jsonldPayload) {
    const contentGraph = this.getContentGraph();
    contentGraph["@graph"] = contentGraph["@graph"].concat(jsonldPayload);
  },
  addContentGraphData: function(predicate, object) {
    this.getContentGraphNode()[predicate] = object;
    return this;
  },

  addRating: function(rating, connectionUri) {
    this.getContentGraphNode()[won.WON.hasFeedback] = {
      "@id": "_:b0",
      "http://purl.org/webofneeds/model#forResource": {
        "@id": connectionUri,
      },
      "http://purl.org/webofneeds/model#hasBinaryRating": {
        "@id": rating,
      },
    };
    return this;
  },

  build: function() {
    return this.data;
  },
};

//TODO replace with `export default` after switching everything to ES6-module-syntax
export default won;
