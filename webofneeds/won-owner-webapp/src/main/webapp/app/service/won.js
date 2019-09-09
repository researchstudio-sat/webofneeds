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
import { clone, is, isArray } from "../utils.js";
import {
  clearDisclaimerAccepted,
  clearReadUris,
  isDisclaimerAccepted,
  setDisclaimerAccepted,
} from "../won-localstorage.js";
import jsonld from "jsonld/dist/jsonld.js";

import N3 from "n3";

window.N34dbg = N3;

let won = {};

/**
 *  Constants
 *
 */

won.debugmode = false; //if you set this to true, the created atoms will get flagged as debug atoms in order to get matches and requests from the debugbot

won.clearReadUris = clearReadUris;
won.isDisclaimerAccepted = isDisclaimerAccepted;
won.clearDisclaimerAccepted = clearDisclaimerAccepted;
won.setDisclaimerAccepted = setDisclaimerAccepted;

won.WON = {};
won.WON.baseUri = "https://w3id.org/won/core#";
won.WON.matcherURI = "https://localhost:8443/matcher/search/";
won.WON.prefix = "won";

won.WON.wonNode = won.WON.baseUri + "wonNode";
won.WON.wonNodeCompacted = won.WON.prefix + ":wonNode";
won.WON.Active = won.WON.baseUri + "Active";
won.WON.ActiveCompacted = won.WON.prefix + ":Active";
won.WON.Closed = won.WON.baseUri + "Closed";
won.WON.ClosedCompacted = won.WON.prefix + ":Closed";

won.WON.Inactive = won.WON.baseUri + "Inactive";
won.WON.InactiveCompacted = won.WON.prefix + ":Inactive";

won.WON.Deleted = won.WON.baseUri + "Deleted";
won.WON.DeletedCompacted = won.WON.prefix + ":Deleted";

won.WON.atomState = won.WON.baseUri + "atomState";
won.WON.atomStateCompacted = won.WON.prefix + ":atomState";
won.WON.socket = won.WON.baseUri + "socket";
won.WON.socketCompacted = won.WON.prefix + ":socket";
won.WON.targetSocket = won.WON.baseUri + "targetSocket";
won.WON.targetSocketCompacted = won.WON.prefix + ":targetSocket";

won.WON.targetAtom = won.WON.baseUri + "targetAtom";
won.WON.targetAtomCompacted = won.WON.prefix + ":targetAtom";
won.WON.targetConnection = won.WON.baseUri + "targetConnection";
won.WON.targetConnectionCompacted = won.WON.prefix + ":targetConnection";

won.WON.connectionState = won.WON.baseUri + "connectionState";
won.WON.connectionState = won.WON.prefix + ":connectionState";
won.WON.Suggested = won.WON.baseUri + "Suggested";
won.WON.SuggestedCompacted = won.WON.baseUri + ":Suggested";
won.WON.RequestReceived = won.WON.baseUri + "RequestReceived";
won.WON.RequestReceivedCompacted = won.WON.baseUri + ":RequestReceived";
won.WON.RequestSent = won.WON.baseUri + "RequestSent";
won.WON.RequestSentCompacted = won.WON.baseUri + ":RequestSent";

won.WON.Connected = won.WON.baseUri + "Connected";

//TOAST TYPES
won.WON.infoToast = won.WON.baseUri + "InfoToast";
won.WON.warnToast = won.WON.baseUri + "WarnToast";
won.WON.errorToast = won.WON.baseUri + "ErrorToast";

won.WON.graph = won.WON.baseUri + "graph";
won.WON.graphCompacted = won.WON.prefix + ":graph";

won.WON.Connection = won.WON.baseUri + "Connection";
won.WON.ConnectionCompacted = won.WON.prefix + ":Connection";

won.WON.Atom = won.WON.baseUri + "Atom";
won.WON.AtomCompacted = won.WON.prefix + ":Atom";
won.WON.sourceAtom = won.WON.baseUri + "sourceAtom";
won.WON.sourceAtomCompacted = won.WON.prefix + ":sourceAtom";
won.WON.connections = won.WON.baseUri + "connections";
won.WON.connectionsCompacted = won.WON.prefix + ":connections";
won.WON.connectionState = won.WON.baseUri + "connectionState";
won.WON.connectionStateCompacted = won.WON.prefix + ":connectionState";
won.WON.hasContent = won.WON.baseUri + "hasContent";
won.WON.hasContentCompacted = won.WON.prefix + ":content";
won.WON.messageContainer = won.WON.baseUri + "messageContainer";
won.WON.messageContainerCompacted = won.WON.prefix + ":messageContainer";

won.WON.Persona = won.WON.baseUri + "Persona";
won.WON.PersonaCompacted = won.WON.prefix + ":Persona";

won.WON.matchScore = won.WON.baseUri + "matchScore";
won.WON.matchScoreCompacted = won.WON.prefix + ":matchScore";
won.WON.matchCounterpart = won.WON.baseUri + "matchCounterpart";
won.WON.matchCounterpart = won.WON.prefix + ":matchCounterpart";

won.WON.searchResultURI = won.WON.baseUri + "uri";
won.WON.searchResultPreview = won.WON.baseUri + "preview";
//todo: change to SearchResult
won.WON.searchResult = won.WON.baseUri + "Match";

won.WON.usedForTesting = won.WON.prefix + "UserForTesting";

won.WONCON = {};
won.WONCON.baseUri = "https://w3id.org/won/content#";
won.WONCON.prefix = "con";
won.WONCON.text = won.WONCON.baseUri + "text";
won.WONCON.textCompacted = won.WONCON.prefix + ":text";
won.WONCON.tag = won.WONCON.baseUri + "tag";
won.WONCON.tagCompacted = won.WONCON.prefix + ":tag";
won.WONCON.binaryRatingGood = won.WONCON.baseUri + "Good";
won.WONCON.binaryRatingBad = won.WONCON.baseUri + "Bad";
won.WONCON.feedback = won.WONCON.baseUri + "feedback";
won.WONCON.binaryRating = won.WONCON.baseUri + "binaryRating";
won.WONCON.feedbackTarget = won.WON.baseUri + "feedbackTarget";
won.WONCON.DirectResponse = won.WON.baseUri + "DirectResponse";
won.WONCON.DirectResponseCompacted = won.WONCON.prefix + ":DirectResponse";

won.WONMATCH = {};
won.WONMATCH.baseUri = "https://w3id.org/won/matching#";
won.WONMATCH.prefix = "match";
won.WONMATCH.NoHintForCounterpartCompacted =
  won.WONMATCH.prefix + ":NoHintForCounterpart";
won.WONMATCH.UsedForTestingCompacted = won.WONMATCH.prefix + ":UsedForTesting";
won.WONMATCH.NoHintForMeCompacted = won.WONMATCH.prefix + ":NoHintForMe";
won.WONMATCH.flag = won.WONMATCH.baseUri + "flag";
won.WONMATCH.flagCompacted = won.WONMATCH.prefix + ":flag";

won.WONMSG = {};
won.WONMSG.baseUri = "https://w3id.org/won/message#";
won.WONMSG.prefix = "msg";
won.WONMSG.recipientAtom = won.WONMSG.baseUri + "recipientAtom";
won.WONMSG.recipientAtomCompacted = won.WONMSG.prefix + ":recipientAtom";
won.WONMSG.recipient = won.WONMSG.baseUri + "recipient";
won.WONMSG.recipientCompacted = won.WONMSG.prefix + ":recipient";
won.WONMSG.recipientNode = won.WONMSG.baseUri + "recipientNode";
won.WONMSG.recipientNodeCompacted = won.WONMSG.prefix + ":recipientNode";
won.WONMSG.recipientSocket = won.WONMSG.baseUri + "recipientSocket";
won.WONMSG.recipientSocketCompacted = won.WONMSG.prefix + ":recipientSocket";
won.WONMSG.senderAtom = won.WONMSG.baseUri + "senderAtom";
won.WONMSG.senderAtomCompacted = won.WONMSG.prefix + ":senderAtom";
won.WONMSG.sender = won.WONMSG.baseUri + "sender";
won.WONMSG.senderCompacted = won.WONMSG.prefix + ":sender";
won.WONMSG.senderNode = won.WONMSG.baseUri + "senderNode";
won.WONMSG.senderNodeCompacted = won.WONMSG.prefix + ":senderNode";
won.WONMSG.senderSocket = won.WONMSG.baseUri + "senderSocket";
won.WONMSG.senderSocketCompacted = won.WONMSG.prefix + ":senderSocket";
won.WONMSG.messageType = won.WONMSG.baseUri + ":messageType";
won.WONMSG.messageTypeCompacted = won.WONMSG.prefix + ":messageType";
won.WONMSG.timestamp = won.WONMSG.baseUri + "timestamp";
won.WONMSG.timestampCompacted = won.WONMSG.prefix + ":timestamp";
won.WONMSG.isResponseTo = won.WONMSG.baseUri + "isResponseTo";
won.WONMSG.isResponseToCompacted = won.WONMSG.prefix + ":isResponseTo";
won.WONMSG.isRemoteResponseTo = won.WONMSG.baseUri + "isRemoteResponseTo";
won.WONMSG.isRemoteResponseToCompacted =
  won.WONMSG.prefix + ":isRemoteResponseTo";
won.WONMSG.EnvelopeGraph = won.WONMSG.baseUri + "EnvelopeGraph";
won.WONMSG.EnvelopeGraphCompacted = won.WONMSG.prefix + ":EnvelopeGraph";

won.WONMSG.hasContent = won.WONMSG.baseUri + "hasContent";
won.WONMSG.hasContentCompacted = won.WONMSG.prefix + ":content";

won.WONMSG.FromOwner = won.WONMSG.baseUri + "FromOwner";
won.WONMSG.FromOwnerCompacted = won.WONMSG.prefix + ":FromOwner";
won.WONMSG.FromExternal = won.WONMSG.baseUri + "FromExternal";
won.WONMSG.FromSystem = won.WONMSG.baseUri + "FromSystem";

//message types
won.WONMSG.createMessage = won.WONMSG.baseUri + "CreateMessage";
won.WONMSG.createMessageCompacted = won.WONMSG.prefix + ":CreateMessage";
won.WONMSG.replaceMessage = won.WONMSG.baseUri + "ReplaceMessage";
won.WONMSG.replaceMessageCompacted = won.WONMSG.prefix + ":ReplaceMessage";
won.WONMSG.activateAtomMessage = won.WONMSG.baseUri + "ActivateMessage";
won.WONMSG.activateAtomMessageCompacted =
  won.WONMSG.prefix + ":ActivateMessage";
won.WONMSG.deleteAtomMessage = won.WONMSG.baseUri + "DeleteMessage";
won.WONMSG.deleteAtomMessageCompacted = won.WONMSG.prefix + ":DeleteMessage";
won.WONMSG.deleteAtomSentMessage = won.WONMSG.baseUri + "DeleteSentMessage";
won.WONMSG.deleteAtomSentMessageCompacted =
  won.WONMSG.prefix + ":DeleteSentMessage";
won.WONMSG.closeAtomMessage = won.WONMSG.baseUri + "DeactivateMessage";
won.WONMSG.closeAtomMessageCompacted = won.WONMSG.prefix + ":DeactivateMessage";
won.WONMSG.closeAtomSentMessage = won.WONMSG.baseUri + "DeactivateSentMessage";
won.WONMSG.closeAtomSentMessageCompacted =
  won.WONMSG.prefix + ":DeactivateSentMessage";
won.WONMSG.atomHintMessage = won.WONMSG.baseUri + "AtomHintMessage";
won.WONMSG.atomHintMessageCompacted = won.WONMSG.prefix + ":AtomHintMessage";
won.WONMSG.socketHintMessage = won.WONMSG.baseUri + "SocketHintMessage";
won.WONMSG.socketHintMessageCompacted =
  won.WONMSG.prefix + ":SocketHintMessage";
won.WONMSG.hintFeedbackMessage = won.WONMSG.baseUri + "HintFeedbackMessage";
won.WONMSG.hintFeedbackMessageCompacted =
  won.WONMSG.prefix + ":HintFeedbackMessage";
won.WONMSG.connectMessage = won.WONMSG.baseUri + "ConnectMessage";
won.WONMSG.connectMessageCompacted = won.WONMSG.prefix + ":ConnectMessage";
won.WONMSG.connectSentMessage = won.WONMSG.baseUri + "ConnectSentMessage";
won.WONMSG.connectSentMessageCompacted =
  won.WONMSG.prefix + ":ConnectSentMessage";
won.WONMSG.atomStateMessage = won.WONMSG.baseUri + "AtomStateMessage";
won.WONMSG.atomStateMessageCompacted = won.WONMSG.prefix + ":AtomStateMessage";
won.WONMSG.closeMessage = won.WONMSG.baseUri + "CloseMessage";
won.WONMSG.closeMessageCompacted = won.WONMSG.prefix + ":CloseMessage";
won.WONMSG.openMessage = won.WONMSG.baseUri + "OpenMessage";
won.WONMSG.feedbackMessage = won.WONMSG.baseUri + "HintFeedbackMessage";
won.WONMSG.openMessageCompacted = won.WONMSG.prefix + ":OpenMessage";
won.WONMSG.openSentMessage = won.WONMSG.baseUri + "OpenSentMessage";
won.WONMSG.openSentMessageCompacted = won.WONMSG.prefix + ":OpenSentMessage";
won.WONMSG.changeNotificationMessage =
  won.WONMSG.baseUri + "ChangeNotificationMessage";
won.WONMSG.changeNotificationMessageCompacted =
  won.WONMSG.prefix + ":ChangeNotificationMessage";
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

won.HOLD = {};
won.HOLD.baseUri = "https://w3id.org/won/ext/hold#";
won.HOLD.prefix = "hold";
won.HOLD.HoldableSocket = won.HOLD.baseUri + "HoldableSocket";
won.HOLD.HoldableSocketCompacted = won.HOLD.prefix + ":HoldableSocket";
won.HOLD.HolderSocket = won.HOLD.baseUri + "HolderSocket";
won.HOLD.HolderSocketCompacted = won.HOLD.prefix + ":HolderSocket";

won.CHAT = {};
won.CHAT.baseUri = "https://w3id.org/won/ext/chat#";
won.CHAT.prefix = "chat";
won.CHAT.ChatSocket = won.CHAT.baseUri + "ChatSocket";
won.CHAT.ChatSocketCompacted = won.CHAT.prefix + ":ChatSocket";

won.GROUP = {};
won.GROUP.baseUri = "https://w3id.org/won/ext/group#";
won.GROUP.prefix = "group";
won.GROUP.GroupSocket = won.GROUP.baseUri + "GroupSocket";
won.GROUP.GroupSocketCompacted = won.GROUP.prefix + ":GroupSocket";

won.REVIEW = {};
won.REVIEW.baseUri = "https://w3id.org/won/ext/review#";
won.REVIEW.prefix = "review";
won.REVIEW.ReviewSocket = won.REVIEW.baseUri + "ReviewSocket";
won.REVIEW.ReviewSocketCompacted = won.REVIEW.prefix + ":ReviewSocket";

won.BUDDY = {};
won.BUDDY.baseUri = "https://w3id.org/won/ext/buddy#";
won.BUDDY.prefix = "buddy";
won.BUDDY.BuddySocket = won.BUDDY.baseUri + "BuddySocket";
won.BUDDY.BuddySocketCompacted = won.BUDDY.prefix + ":BuddySocket";

won.EVENT = {};
won.EVENT.WON_MESSAGE_RECEIVED = "WonMessageReceived";
won.EVENT.WON_SEARCH_RECEIVED = "SearchReceivedEvent";
won.EVENT.ATOM_CREATED = "AtomCreatedEvent";
won.EVENT.HINT_RECEIVED = "HintReceivedEvent";
won.EVENT.CONNECT_SENT = "ConnectSentEvent";
won.EVENT.CONNECT_RECEIVED = "ConnectReceivedEvent";
won.EVENT.OPEN_SENT = "OpenSentEvent";
won.EVENT.ACTIVATE_ATOM_SENT = "ActivateAtomSentEvent";
won.EVENT.ACTIVATE_ATOM_RECEIVED = "ActivateAtomReceivedEvent";
won.EVENT.CLOSE_ATOM_SENT = "DeactivateSentEvent";
won.EVENT.CLOSE_ATOM_RECEIVED = "Deactivate_Received_Event";
won.EVENT.OPEN_RECEIVED = "OpenReceivedEvent";
won.EVENT.CLOSE_SENT = "CloseSentEvent";
won.EVENT.CLOSE_RECEIVED = "CloseReceivedEvent";
won.EVENT.CONNECTION_MESSAGE_RECEIVED = "ConnectionMessageReceivedEvent";
won.EVENT.CONNECTION_MESSAGE_SENT = "ConnectionMessageSentEvent";
won.EVENT.ATOM_STATE_MESSAGE_RECEIVED = "AtomStateMessageReceivedEvent";
won.EVENT.NO_CONNECTION = "NoConnectionErrorEvent";
won.EVENT.NOT_TRANSMITTED = "NotTransmittedErrorEvent";
won.EVENT.USER_SIGNED_IN = "UserSignedInEvent";
won.EVENT.USER_SIGNED_OUT = "UserSignedOutEvent";
//TODO: this temp event, before we find out how to deal with session timeout
won.EVENT.WEBSOCKET_CLOSED_UNEXPECTED = "WebSocketClosedUnexpected";

won.EVENT.APPSTATE_CURRENT_ATOM_CHANGED = "AppState.CurrentAtomChangedEvent";

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
won.UNREAD.GROUP.BYATOM = "byAtom";

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
  EXPORT_NOT_VERIFIED: 7403,

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
  [won.WON.Suggested]: won.WONMSG.socketHintMessage,
  [won.WON.RequestReceived]: won.WONMSG.connectMessage,
  [won.WON.RequestSent]: won.WONMSG.connectSentMessage,
  [won.WON.Connected]: won.WONMSG.connectionMessage,
  [won.WON.Closed]: won.WONMSG.closeMessage,
});

won.messageType2EventType = {
  [won.WONMSG.atomHintMessageCompacted]: won.EVENT.HINT_RECEIVED,
  [won.WONMSG.socketHintMessageCompacted]: won.EVENT.HINT_RECEIVED,
  [won.WONMSG.connectMessageCompacted]: won.EVENT.CONNECT_RECEIVED,
  [won.WONMSG.connectSentMessageCompacted]: won.EVENT.CONNECT_SENT,
  [won.WONMSG.openMessageCompacted]: won.EVENT.OPEN_RECEIVED,
  [won.WONMSG.closeMessageCompacted]: won.EVENT.CLOSE_RECEIVED,
  [won.WONMSG.closeAtomMessageCompacted]: won.EVENT.CLOSE_ATOM_RECEIVED,
  [won.WONMSG.connectionMessageCompacted]:
    won.EVENT.CONNECTION_MESSAGE_RECEIVED,
  [won.WONMSG.atomStateMessageCompacted]: won.EVENT.ATOM_STATE_MESSAGE_RECEIVED,
  [won.WONMSG.errorMessageCompacted]: won.EVENT.NOT_TRANSMITTED,
};

//UTILS
won.WONMSG.uriPlaceholder = Object.freeze({
  event: "this:eventuri",
});

won.WON.contentNodeBlankUri = Object.freeze({
  seeks: "_:seeksAtomContent",
});

/**
 * Compacts the passed string if possible.
 * e.g. "https://w3id.org/won/core#Demand" -> "won:Demand"
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

/**
 * Method that checks if the given element is already an array, if so return it, if not
 * return the element as a single element array, if element is undefined return undefined
 * @param elements
 * @returns {*}
 */
function createArray(elements) {
  return !elements || Array.isArray(elements) ? elements : [elements];
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
  msg: "https://w3id.org/won/message#",
  won: "https://w3id.org/won/core#",
  con: won.WONCON.baseUri,
  match: won.WONMATCH.baseUri,
  demo: "https://w3id.org/won/ext/demo#",
  hold: won.HOLD.baseUri,
  chat: won.CHAT.baseUri,
  group: won.GROUP.baseUri,
  review: won.REVIEW.baseUri,
  buddy: won.BUDDY.baseUri,
  rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
  agr: "https://w3id.org/won/agreement#",
  pay: "https://w3id.org/won/payment#",
  gr: "http://purl.org/goodrelations/v1#",
  wf: "https://w3id.org/won/workflow#",
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
  s: "http://schema.org/",
  sh: "http://www.w3.org/ns/shacl#",
  foaf: "http://xmlns.com/foaf/0.1/",
  "msg:messageType": {
    "@id": "https://w3id.org/won/message#messageType",
    "@type": "@id",
  },
};
/** ttl-prefixes e.g. `@prefix msg: <https://w3id.org/won/message#>.\n @prefix...` */
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
 *  Adds a msg:content triple for each specified graphURI into the message graph
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
 * [message] wonmsg:content [graphURI]
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
        "msg:messageType": { "@id": messageType },
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
 *  (atom, connection container, connection ,event container, event (=wonMessage)
 *  from json-ld
 *
 */

won.WonDomainObjects = function() {};

won.WonDomainObjects.prototype = {
  constructor: won.WonDomainObjects,
  /**
   * Returns the atomURIs.
   */
  getAtomUris: function() {},
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
  const writer = new N3.Writer(writerArgs);
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
  const parser = parserArgs ? new N3.Parser(parserArgs) : new N3.Parser();
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
won.ttlToJsonLd = async function(ttl) {
  const tryConversion = async () => {
    const { quads /*prefixes*/ } = await won.n3Parse(ttl);

    const quadString = await won.n3Write(quads, {
      format: "application/n-quads",
    });

    const parsedJsonld = await jsonld.promises.fromRDF(quadString, {
      format: "application/n-quads",
    });

    return parsedJsonld;
  };
  return tryConversion().catch(e => {
    e.message =
      "error while parsing the following turtle:\n\n" +
      ttl +
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
      "https://w3id.org/won/message#correspondingRemoteMessage"
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
        /**
         * Parses an rdf-uri and gets the base-uri, i.e.
         * the part before and including the fragment identifier
         * ("#") or last slash ("/").
         * @param {*} uri
         */
        const prefixOfUri = uri => {
          // if there's hash-tags, the first of these
          // is the fragment identifier and everything
          // after is the id. remove everything following it.
          let prefix = uri.replace(/#.*/, "#");

          // if there's no fragment-identifier, the
          // everything after the last slash is removed.
          if (!prefix.endsWith("#")) {
            prefix = prefix.replace(/\/([^/]*)$/, "/");
          }

          return prefix;
        };

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
        console.error(
          "Failed to generate trig for message ",
          this.getMessageUri(),
          "\n\n",
          e
        );
        const msg =
          "Failed to generate trig for message " +
          this.getMessageUri() +
          "\n\n" +
          e.message +
          "\n\n" +
          e.stack;
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
        console.error(
          "Failed to generate jsonld for message ",
          this.getMessageUri(),
          "\n\n",
          e
        );
        const msg =
          "Failed to generate jsonld for message " +
          this.getMessageUri() +
          "\n\n" +
          e.message +
          "\n\n" +
          e.stack;
        this.compactFramedMessageError = msg;
      }
    }
  },
  generateContainedForwardedWonMessages: async function() {
    const forwardedMessageUris = this.getForwardedMessageUris();
    if (forwardedMessageUris && forwardedMessageUris.length == 1) {
      //TODO: RECURSIVELY CREATE wonMessageObjects from all the forwarded Messages within this message
      //const forwardedMessages = wonMessage.compactFramedMessage["msg:forwardedMessage"];
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
      "https://w3id.org/won/message#correspondingRemoteMessage"
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
      return this.compactFramedMessage["msg:correspondingRemoteMessage"];
    }
  },
  getCompactFramedForwardedMessageContent: function() {
    const forwardedMessage =
      this.compactFramedMessage &&
      this.compactFramedMessage["msg:forwardedMessage"];
    const forwardedMessageContent =
      forwardedMessage && forwardedMessage["msg:correspondingRemoteMessage"];
    return forwardedMessageContent;
  },
  getCompactRawMessage: function() {
    return this.compactRawMessage;
  },
  getMessageType: function() {
    return this.getProperty("https://w3id.org/won/message#messageType");
  },
  getInjectIntoConnectionUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/message#injectIntoConnection")
    );
  },
  getForwardedMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/message#forwardedMessage")
    );
  },
  getReceivedTimestamp: function() {
    return this.getPropertyFromLocalMessage(
      "https://w3id.org/won/message#receivedTimestamp"
    );
  },
  getSentTimestamp: function() {
    return this.getPropertyFromLocalMessage(
      "https://w3id.org/won/message#sentTimestamp"
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
    return this.getProperty("https://w3id.org/won/content#text");
  },
  getHintScore: function() {
    return this.getProperty("https://w3id.org/won/core#hintScore");
  },
  getHintTargetAtom: function() {
    return this.getProperty("https://w3id.org/won/core#hintTargetAtom");
  },
  getHintTargetSocket: function() {
    return this.getProperty("https://w3id.org/won/core#hintTargetSocket");
  },
  getIsResponseTo: function() {
    return this.getProperty("https://w3id.org/won/message#isResponseTo");
  },
  getIsRemoteResponseTo: function() {
    return this.getProperty("https://w3id.org/won/message#isRemoteResponseTo");
  },
  getIsResponseToMessageType: function() {
    return this.getProperty(
      "https://w3id.org/won/message#isResponseToMessageType"
    );
  },

  getSenderNode: function() {
    return this.getProperty("https://w3id.org/won/message#senderNode");
  },
  getSenderAtom: function() {
    return this.getProperty("https://w3id.org/won/message#senderAtom");
  },
  getSenderConnection: function() {
    return this.getProperty("https://w3id.org/won/message#sender");
  },
  getRecipientNode: function() {
    return this.getProperty("https://w3id.org/won/message#recipientNode");
  },
  getRecipientAtom: function() {
    return this.getProperty("https://w3id.org/won/message#recipientAtom");
  },
  getRecipientConnection: function() {
    return this.getProperty("https://w3id.org/won/message#recipient");
  },

  getProposedMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/agreement#proposes")
    );
  },

  getClaimsMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/agreement#claims")
    );
  },

  getAcceptsMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/agreement#accepts")
    );
  },
  getProposedToCancelMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/agreement#proposesToCancel")
    );
  },
  getRejectsMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/agreement#rejects")
    );
  },
  getRetractsMessageUris: function() {
    return createArray(
      this.getProperty("https://w3id.org/won/modification#retracts")
    );
  },

  isProposeMessage: function() {
    return !!this.getProperty("https://w3id.org/won/agreement#proposes");
  },
  isAcceptMessage: function() {
    return !!this.getProperty("https://w3id.org/won/agreement#accepts");
  },
  isProposeToCancel: function() {
    return !!this.getProperty(
      "https://w3id.org/won/agreement#proposesToCancel"
    );
  },
  isProposal: function() {
    return !!this.getProperty("https://w3id.org/won/agreement#Proposal");
  },
  isAgreement: function() {
    return !!this.getProperty("https://w3id.org/won/agreement#Agreement");
  },

  isRejectMessage: function() {
    return !!this.getProperty("https://w3id.org/won/agreement#rejects");
  },
  isRetractMessage: function() {
    return !!this.getProperty("https://w3id.org/won/modification#retracts");
  },
  isOutgoingMessage: function() {
    return (
      this.isFromOwner() ||
      (this.isFromSystem() &&
        this.getSenderConnection() !== this.getRecipientConnection())
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
    return direction === "https://w3id.org/won/message#FromSystem";
  },
  isFromOwner: function() {
    let direction = this.getMessageDirection();
    return direction === "https://w3id.org/won/message#FromOwner";
  },
  isFromExternal: function() {
    let direction = this.getMessageDirection();
    return direction === "https://w3id.org/won/message#FromExternal";
  },

  isAtomHintMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#AtomHintMessage"
    );
  },
  isSocketHintMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#SocketHintMessage"
    );
  },
  isCreateMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#CreateMessage"
    );
  },
  isConnectMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#ConnectMessage"
    );
  },
  isOpenMessage: function() {
    return this.getMessageType() === "https://w3id.org/won/message#OpenMessage";
  },
  isConnectionMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#ConnectionMessage"
    );
  },
  isCloseMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#CloseMessage"
    );
  },
  isHintFeedbackMessage: function() {
    return (
      this.getMessageType() ===
      "https://w3id.org/won/message#HintFeedbackMessage"
    );
  },
  isActivateMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#ActivateMessage"
    );
  },
  isDeactivateMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#DeactivateMessage"
    );
  },
  isDeleteMessage: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#DeleteMessage"
    );
  },
  isAtomMessage: function() {
    return this.getMessageType() === "https://w3id.org/won/message#AtomMessage";
  },
  isResponse: function() {
    return this.isSuccessResponse() || this.isFailureResponse();
  },
  isSuccessResponse: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#SuccessResponse"
    );
  },
  isFailureResponse: function() {
    return (
      this.getMessageType() === "https://w3id.org/won/message#FailureResponse"
    );
  },
  isResponseToReplaceMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#ReplaceMessage"
    );
  },
  isResponseToAtomHintMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#AtomHintMessage"
    );
  },
  isResponseToCreateMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#CreateMessage"
    );
  },
  isResponseToConnectMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#ConnectMessage"
    );
  },
  isResponseToOpenMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#OpenMessage"
    );
  },
  isResponseToConnectionMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#ConnectionMessage"
    );
  },
  isResponseToCloseMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#CloseMessage"
    );
  },
  isResponseToHintFeedbackMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#HintFeedbackMessage"
    );
  },
  isResponseToActivateMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#ActivateMessage"
    );
  },
  isResponseToDeactivateMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#DeactivateMessage"
    );
  },
  isResponseToDeleteMessage: function() {
    return (
      this.getIsResponseToMessageType() ===
      "https://w3id.org/won/message#DeleteMessage"
    );
  },
  isChangeNotificationMessage: function() {
    return (
      this.getMessageType() ===
      "https://w3id.org/won/message#ChangeNotificationMessage"
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
              "https://w3id.org/won/message#FromExternal"
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
        resource["@type"].includes("https://w3id.org/won/message#EnvelopeGraph")
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
        resource => resource["https://w3id.org/won/message#containsEnvelope"]
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
      .map(resource => resource["https://w3id.org/won/message#content"])
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
            "https://w3id.org/won/message#FromExternal"
          ) ||
          resource["@type"].includes(
            "https://w3id.org/won/message#FromOwner"
          ) ||
          resource["@type"].includes("https://w3id.org/won/message#FromSystem")
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
          resource["https://w3id.org/won/message#correspondingRemoteMessage"]
      )
      .map(resource => ({
        messageUri: resource["@id"],
        correspondingRemoteMessageUri:
          resource[
            "https://w3id.org/won/message#correspondingRemoteMessage"
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
        resource => resource["https://w3id.org/won/message#forwardedMessage"]
      )
      .map(resource => ({
        messageUri: resource["@id"],
        forwardedMessageUri:
          resource["https://w3id.org/won/message#forwardedMessage"][0]["@id"],
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
      "@id": "https://w3id.org/won/message#EnvelopeGraph",
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
  senderAtom: function(senderAtomURI) {
    this.getMessageEventNode()[won.WONMSG.senderAtomCompacted] = {
      "@id": senderAtomURI,
    };
    return this;
  },
  senderNode: function(senderNodeURI) {
    this.getMessageEventNode()[won.WONMSG.senderNodeCompacted] = {
      "@id": senderNodeURI,
    };
    return this;
  },
  sender: function(senderURI) {
    this.getMessageEventNode()[won.WONMSG.senderCompacted] = {
      "@id": senderURI,
    };
    return this;
  },
  recipient: function(recipientURI) {
    this.getMessageEventNode()[won.WONMSG.recipientCompacted] = {
      "@id": recipientURI,
    };
    return this;
  },
  recipientAtom: function(recipientAtomURI) {
    this.getMessageEventNode()[won.WONMSG.recipientAtomCompacted] = {
      "@id": recipientAtomURI,
    };
    return this;
  },
  recipientNode: function(recipientURI) {
    this.getMessageEventNode()[won.WONMSG.recipientNodeCompacted] = {
      "@id": recipientURI,
    };
    return this;
  },
  ownerDirection: function() {
    this.getMessageEventNode()["@type"] = won.WONMSG.FromOwnerCompacted;
    return this;
  },
  sentTimestamp: function(timestamp) {
    this.getMessageEventNode()["msg:sentTimestamp"] = timestamp;
    return this;
  },
  /**
   * Adds the specified socket as local sockets. Only needed for connect and
   * openSuggested.
   * @param recipientURI
   * @returns {won.MessageBuilder}
   */
  socket: function(socketURI) {
    this.getMessageEventNode()[won.WONMSG.senderSocketCompacted] = {
      "@id": socketURI,
    };
    return this;
  },
  /**
   * Adds the specified socket as local sockets. Only needed for connect and
   * openSuggested.
   * @param recipientURI
   * @returns {won.MessageBuilder}
   */
  targetSocket: function(socketURI) {
    this.getMessageEventNode()[won.WONMSG.recipientSocketCompacted] = {
      "@id": socketURI,
    };
    return this;
  },
  /**
   * Adds the specified text as text message inside the content. Can be
   * used with connectMessage, openMessage and connectionMessage.
   * @param text - text of the message
   * @returns {won.MessageBuilder}
   */
  textMessage: function(text) {
    if (text == null || text === "") {
      // text is either null, undefined, or empty
      // do nothing
    } else {
      this.getContentGraphNode()[won.WONCON.textCompacted] = text;
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
    this.getContentGraphNode()[won.WONCON.feedback] = {
      "@id": "_:b0",
      "https://w3id.org/won/content#feedbackTarget": {
        "@id": connectionUri,
      },
      "https://w3id.org/won/content#binaryRating": {
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
