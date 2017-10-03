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
"format es6"; /* required to force babel to transpile this so the minifier is happy */
   var won = {};

    /**
     *  Constants
     *
     */

    won.debugmode = false; //if you set this to true, the created needs will get flagged as debug needs in order to get matches and requests from the debugbot

    won.WON = {};
    won.WON.baseUri = "http://purl.org/webofneeds/model#";
    //won.WON.matcherURI = "http://localhost:8080/matcher/search/"
    won.WON.matcherURI = "https://localhost:8443/matcher/search/";
    //won.WON.matcherURI = "http://sat001.researchstudio.at:8080/matcher/search/";
    //won.WON.matcherURI = "https://sat001.researchstudio.at:8443/matcher/search/";

    won.WON.prefix = "won";
    won.WON.hasWonNode = won.WON.baseUri+"hasWonNode";
    won.WON.hasWonNodeCompacted = won.WON.prefix+":hasWonNode";
    won.WON.Active = won.WON.baseUri + "Active";
    won.WON.ActiveCompacted = won.WON.prefix + ":Active";
    won.WON.Closed = won.WON.baseUri + "Closed";
    won.WON.ClosedCompacted = won.WON.prefix + ":Closed";

    won.WON.Inactive = won.WON.baseUri + "Inactive";
    won.WON.InactiveCompacted = won.WON.prefix + ":Inactive";

    won.WON.isInState = won.WON.baseUri+"isInState";
    won.WON.isInStateCompacted = won.WON.prefix+":isInState";
    won.WON.hasFacet= won.WON.baseUri+"hasFacet";
    won.WON.hasFacetCompacted= won.WON.prefix+":hasFacet";
    won.WON.hasFlag= won.WON.baseUri+"hasFlag";
    won.WON.hasFlagCompacted= won.WON.prefix+"hasFlag";
    won.WON.hasRemoteFacet= won.WON.baseUri+"hasRemoteFacet";
    won.WON.hasRemoteFacetCompacted= won.WON.prefix+":hasRemoteFacet";

    won.WON.hasRemoteNeed= won.WON.baseUri+"hasRemoteNeed";
    won.WON.hasRemoteNeedCompacted = won.WON.prefix+":hasRemoteNeed";
    won.WON.hasRemoteConnection = won.WON.baseUri+"hasRemoteConnection";
    won.WON.hasRemoteConnectionCompacted = won.WON.prefix+":hasRemoteConnection";
    won.WON.forResource = won.WON.baseUri+"forResource";
    won.WON.hasBinaryRating = won.WON.baseUri+"hasBinaryRating";
    won.WON.binaryRatingGood = won.WON.baseUri+"Good";
    won.WON.binaryRatingBad = won.WON.baseUri+"Bad";
    won.WON.hasFeedback = won.WON.baseUri+"hasFeedback";

    won.WON.hasConnectionState = won.WON.baseUri+"hasConnectionState";
    won.WON.hasConnectionState = won.WON.prefix+":hasConnectionState";
    won.WON.Suggested = won.WON.baseUri+"Suggested";
    won.WON.SuggestedCompacted = won.WON.baseUri+":Suggested";
    won.WON.RequestReceived = won.WON.baseUri+"RequestReceived";
    won.WON.RequestReceivedCompacted = won.WON.baseUri+":RequestReceived";
    won.WON.RequestSent = won.WON.baseUri+"RequestSent";
    won.WON.RequestSentCompacted = won.WON.baseUri+":RequestSent";

    won.WON.Connected = won.WON.baseUri+"Connected";

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
    won.WON.BasicNeedTypeCritique = won.WON.baseUri + "Critique";
    won.WON.BasicNeedTypeCritiqueCompacted = won.WON.prefix + ":Critique";
    won.WON.BasicNeedTypeWhatsAroundCompacted = won.WON.prefix + ":WhatsAround";
    won.WON.OwnerFacet = won.WON.baseUri +"OwnerFacet";
    won.WON.OwnerFacetCompacted = won.WON.prefix +":OwnerFacet";
    won.WON.GroupFacet = won.WON.baseUri +"GroupFacet";
    won.WON.GroupFacetCompacted = won.WON.prefix +":GroupFacet";
    won.WON.ParticipantFacet = won.WON.baseUri +"ParticipantFacet";
    won.WON.ParticipantFacetCompacted = won.WON.prefix +":ParticipantFacet";
    won.WON.CommentFacet = won.WON.baseUri +"CommentFacet";
    won.WON.CommentFacetCompacted = won.WON.prefix +":CommentFacet";
    won.WON.CoordinatorFacet = won.WON.baseUri +"CoordinatorFacet";
    won.WON.CoordinatorFacetCompacted = won.WON.prefix +":CoordinatorFacet";
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
    won.WON.hasContentDescriptionCompacted = won.WON.prefix + ":hasContentDescription";
    won.WON.hasEndTime = won.WON.baseUri + "hasEndTime";
    won.WON.hasEndTimeCompacted = won.WON.prefix + ":hasEndTime";
    won.WON.hasEventContainer = won.WON.baseUri + "hasEventContainer";
    won.WON.hasEventContainerCompacted = won.WON.prefix + ":hasEventContainer";
    won.WON.hasOriginator = won.WON.baseUri + "hasOriginator";
    won.WON.hasOriginatorCompacted = won.WON.prefix + ":hasOriginator";
    won.WON.hasRecurInfiniteTimes = won.WON.baseUri + "hasRecurInfiniteTimes";
    won.WON.hasRecurInfiniteTimesCompacted = won.WON.prefix + ":hasRecurInfiniteTimes";
    won.WON.hasRecursIn = won.WON.baseUri + "hasRecursIn";
    won.WON.hasRecursInCompacted = won.WON.prefix + ":hasRecurInfiniteTimes";
    won.WON.hasScore = won.WON.baseUri + "hasScore";
    won.WON.hasScoreCompacted = won.WON.prefix + ":hasScore";
    won.WON.hasStartTime = won.WON.baseUri + "hasStartTime";
    won.WON.hasStartTimeCompacted = won.WON.prefix + ":hasStartTime";
    won.WON.hasTag = won.WON.baseUri + "hasTag";
    won.WON.hasTagCompacted = won.WON.prefix + ":hasTag";

    won.WON.hasMatchScore = won.WON.baseUri + "hasMatchScore";
    won.WON.hasMatchScoreCompacted = won.WON.prefix + ":hasMatchScore";
    won.WON.hasMatchCounterpart = won.WON.baseUri + "hasMatchCounterpart";
    won.WON.hasMatchCounterpart = won.WON.prefix + ":hasMatchCounterpart";
    won.WON.hasTextMessage= won.WON.baseUri + "hasTextMessage";
    won.WON.hasTextMessageCompacted= won.WON.prefix + ":hasTextMessage";

    won.WON.searchResultURI =  won.WON.baseUri + "uri";
    won.WON.searchResultPreview =  won.WON.baseUri + "preview";
    //todo: change to SearchResult
    won.WON.searchResult =  won.WON.baseUri + "Match";

    won.WON.usedForTesting =  won.WON.prefix + "UserForTesting";



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
    won.WONMSG.hasSenderNeed = won.WONMSG.baseUri + "hasSenderNeed";
    won.WONMSG.hasSenderNeedCompacted = won.WONMSG.prefix + ":hasSenderNeed";
    won.WONMSG.hasSender = won.WONMSG.baseUri + "hasSender";
    won.WONMSG.hasSenderCompacted = won.WONMSG.prefix + ":hasSender";
    won.WONMSG.hasSenderNode = won.WONMSG.baseUri + "hasSenderNode";
    won.WONMSG.hasSenderNodeCompacted = won.WONMSG.prefix + ":hasSenderNode";
    won.WONMSG.hasMessageType = won.WONMSG.baseUri + ":hasMessageType";
    won.WONMSG.hasMessageTypeCompacted = won.WONMSG.prefix + ":hasMessageType";
    won.WONMSG.hasTimestamp = won.WONMSG.baseUri + "hasTimestamp";
    won.WONMSG.hasTimestampCompacted = won.WONMSG.prefix + ":hasTimestamp";
    won.WONMSG.refersTo = won.WONMSG.baseUri + "refersTo";
    won.WONMSG.refersToCompacted = won.WONMSG.prefix + ":refersTo";
    won.WONMSG.isResponseTo = won.WONMSG.baseUri + "isResponseTo";
    won.WONMSG.isResponseToCompacted = won.WONMSG.prefix + ":isResponseTo";
    won.WONMSG.isRemoteResponseTo = won.WONMSG.baseUri + "isRemoteResponseTo";
    won.WONMSG.isRemoteResponseToCompacted = won.WONMSG.prefix + ":isRemoteResponseTo";
    won.WONMSG.EnvelopeGraph = won.WONMSG.baseUri + "EnvelopeGraph";
    won.WONMSG.EnvelopeGraphCompacted = won.WONMSG.prefix+ ":EnvelopeGraph";

    won.WONMSG.hasContent = won.WONMSG.baseUri + "hasContent";
    won.WONMSG.hasContentCompacted = won.WONMSG.prefix+ ":hasContent";

    won.WONMSG.FromOwner = won.WONMSG.baseUri + "FromOwner";
    won.WONMSG.FromOwnerCompacted = won.WONMSG.prefix + ":FromOwner";
    won.WONMSG.FromExternal = won.WONMSG.baseUri + "FromExternal";
    won.WONMSG.FromSystem = won.WONMSG.baseUri + "FromSystem";

    //message types
    won.WONMSG.createMessage = won.WONMSG.baseUri + "CreateMessage";
    won.WONMSG.createMessageCompacted = won.WONMSG.prefix + ":CreateMessage";
    won.WONMSG.activateNeedMessage = won.WONMSG.baseUri + "ActivateMessage";
    won.WONMSG.activateNeedMessageCompacted = won.WONMSG.prefix + ":ActivateMessage";
    won.WONMSG.closeNeedMessage = won.WONMSG.baseUri + "DeactivateMessage";
    won.WONMSG.closeNeedMessageCompacted = won.WONMSG.prefix + ":DeactivateMessage";
    won.WONMSG.closeNeedSentMessage = won.WONMSG.baseUri +"DeactivateSentMessage";
    won.WONMSG.closeNeedSentMessageCompacted = won.WONMSG.prefix +":DeactivateSentMessage";
    won.WONMSG.hintMessage = won.WONMSG.baseUri + "HintMessage";
    won.WONMSG.hintMessageCompacted = won.WONMSG.prefix + ":HintMessage";
    won.WONMSG.hintFeedbackMessageCompacted = won.WONMSG.prefix + ":HintFeedbackMessage";
    won.WONMSG.connectMessage = won.WONMSG.baseUri + "ConnectMessage";
    won.WONMSG.connectMessageCompacted = won.WONMSG.prefix + ":ConnectMessage";
    won.WONMSG.connectSentMessage = won.WONMSG.baseUri + "ConnectSentMessage";
    won.WONMSG.connectSentMessageCompacted = won.WONMSG.prefix + ":ConnectSentMessage";
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
    won.WONMSG.connectionMessageCompacted = won.WONMSG.prefix + ":ConnectionMessage";
    won.WONMSG.connectionMessageSentMessage = won.WONMSG.baseUri + "ConnectionMessageSentMessage";
    won.WONMSG.connectionMessageSentMessageCompacted = won.WONMSG.prefix + ":ConnectionMessageSentMessage";
    won.WONMSG.connectionMessageReceivedMessage = won.WONMSG.baseUri + "ConnectionMessageReceivedMessage";
    won.WONMSG.connectionMessageReceivedMessageCompacted = won.WONMSG.prefix + ":ConnectionMessageReceivedMessage";


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
    won.EVENT.CONNECT_SENT ="ConnectSentEvent";
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

    won.COMMUNUCATION_STATE = {};
    won.COMMUNUCATION_STATE.NOT_CONNECTED = "NoConnectionEvent"; //own node not reachable
    won.COMMUNUCATION_STATE.NOT_TRANSMITTED = "NotTransmittedEvent";
    won.COMMUNUCATION_STATE.PENDING = "Pending";
    won.COMMUNUCATION_STATE.ACCEPTED = "Accepted";

    won.EVENT.APPSTATE_CURRENT_NEED_CHANGED = "AppState.CurrentNeedChangedEvent";

    //keys for things that can be shown in the GUI as 'unread'
    won.UNREAD = {};
    won.UNREAD.TYPE = {};
    won.UNREAD.TYPE.CREATED = "created";
    won.UNREAD.TYPE.HINT = "hint";
    won.UNREAD.TYPE.MESSAGE = "message";
    won.UNREAD.TYPE.CONNECT = "connect";
    won.UNREAD.TYPE.CLOSE = "close";
    won.UNREAD.TYPES = [won.UNREAD.TYPE.CREATED, won.UNREAD.TYPE.HINT,
        won.UNREAD.TYPE.MESSAGE, won.UNREAD.TYPE.CONNECT, won.UNREAD.TYPE.CLOSE];
    won.UNREAD.GROUP = {};
    won.UNREAD.GROUP.ALL="all";
    won.UNREAD.GROUP.BYNEED="byNeed";


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
        [won.WONMSG.hintMessageCompacted] :  won.EVENT.HINT_RECEIVED,
        [won.WONMSG.connectMessageCompacted] :  won.EVENT.CONNECT_RECEIVED,
        [won.WONMSG.connectSentMessageCompacted] :  won.EVENT.CONNECT_SENT,
        [won.WONMSG.openMessageCompacted] :  won.EVENT.OPEN_RECEIVED,
        [won.WONMSG.closeMessageCompacted] :  won.EVENT.CLOSE_RECEIVED,
        [won.WONMSG.closeNeedMessageCompacted] :  won.EVENT.CLOSE_NEED_RECEIVED,
        [won.WONMSG.connectionMessageCompacted] :  won.EVENT.CONNECTION_MESSAGE_RECEIVED,
        [won.WONMSG.needStateMessageCompacted] :  won.EVENT.NEED_STATE_MESSAGE_RECEIVED,
        [won.WONMSG.errorMessageCompacted] :  won.EVENT.NOT_TRANSMITTED ,
    };

    //UTILS
    var UNSET_URI= "no:uri";


    /**
     * Returns the "compacted" alternative of the value (e.g.
     *    "http://purl.org/webofneeds/model#Demand"
     *    ->  via 'won.WON.BasicNeedTypeDemand'
     *    ->  and 'won.WON.BasicNeedTypeDemandCompacted'
     *    ->  to: "won:Demand";
     * returns `undefined` if the compacted version couldn't be found (see `lookup(...)`)
     * @param longValue
     */
    won.toCompacted = function(longValue) {
        var propertyPath = won.clone(won.constantsReverseLookupTable[longValue]);
        propertyPath[propertyPath.length - 1] += 'Compacted';
        //console.log('toCompacted ', longValue, propertyPath, won.lookup(won, propertyPath));
        return won.lookup(won, propertyPath);
    };


    won.clone = function(obj){
        if(obj === undefined)
            return undefined;
        else
            return JSON.parse(JSON.stringify(obj));
    };


    /**
     * Copies all arguments properties recursively into a
     * new object and returns that.
     */

    won.merge = function(/*args...*/) {
        var o = {};
        for(var i = 0; i < arguments.length; i++) {
            won.mergeIntoLast(arguments[i], o);
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
        for(var i = 0; i < arguments.length -1 ; i++) {
            var obj1 = arguments[arguments.length - 1];
            var obj2 = arguments[i];
            for (var p in obj2) {
                try {
                    // Property in destination object set; update its value.
                    if ( obj2[p].constructor == Object ) {
                        obj1[p] = won.mergeRecursive(obj1[p], obj2[p]);
                    } else {
                        obj1[p] = obj2[p];
                    }
                } catch(e) {
                    // Property in destination object not set; create it and set its value.
                    obj1[p] = obj2[p];

                }
            }
        }
        return obj1;
    };

    // as the constants above should be unique (thus their mapping bijective)
    // it is possible to do a reverse lookup. The table contains former values
    // as keys and maps to arrays that define the lookup-path.
    won.constantsReverseLookupTable = {};
    for(var root of ['WON', 'UNREAD', 'WONMSG', 'EVENT', 'COMMUNUCATION_STATE' ]) {
        won.mergeIntoLast(buildReverseLookup(won[root], [root]), won.constantsReverseLookupTable);
    };


    won.buildReverseLookup = buildReverseLookup;
    /**
     * Builds a reverse lookup-table for the objects properties.
     *
     * NOTE: all properties of the object need to have unique values!
     *
     * e.g.:
     *
     *     var obj = { propA: { subProp: 'foo'}, probB: 2 }
     *
     *     buildReverseLookup(obj)
     *          ----> {foo  ['propA', 'subProp'], 2: ['probB']}
     *
     * @param obj
     * @param accumulatedPath
     * @returns {{}}
     */
    function buildReverseLookup(obj, accumulatedPath /* = [] */) { //TODO this should be in a utils file
        accumulatedPath = typeof accumulatedPath !== 'undefined' ?
            accumulatedPath : []; // to allow calling with only obj

        var lookupAcc = {};
        for(var k in obj) {
            if (obj.hasOwnProperty(k)) {
                var v = obj[k];
                var accPathAppended = accumulatedPath.concat([k])
                var foundLookups = {};
                if (typeof v === 'string' || typeof v === 'number') {
                    //terminal node
                    foundLookups[v] = accPathAppended;

                } else if (typeof v === 'object') {
                    //recurse into objects
                    foundLookups = buildReverseLookup(v, accPathAppended);
                }
                won.mergeIntoLast(foundLookups, lookupAcc);
            }
        }
        return lookupAcc;
    }

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
    function lookup(o, propertyPath){ //TODO this should be in a utils file
        if(!o || !propertyPath) {
            return undefined;
        }
        var resolvedStep = o[propertyPath[0]];
        if(propertyPath.length === 1) {
            return resolvedStep
        } else {
            return lookup(resolvedStep, propertyPath.slice(1))
        }
    }


    //get the URI from a jsonld resource (expects an object with an '@id' property)
    //or the value from a typed literal
    won.getSafeJsonLdValue = function(dataItem) {
        if (dataItem == null) return null;
        if (typeof dataItem === 'object') {
            if (dataItem['@id'] != null) return dataItem['@id'];
            if (dataItem['@type'] != null && dataItem['@value'] != null) return dataItem['@value'];
        } else {
            return dataItem;
        }
        return null;
    }

    won.getLocalName = function(uriOrQname) {
        if (uriOrQname == null || typeof uriOrQname !== 'string') return null;
        //first, try to get the URI hash fragment (without hash)
        var pos = uriOrQname.lastIndexOf('#')
        if ( pos > -1 && pos < uriOrQname.length) {
            return uriOrQname.substring(pos + 1);
        }
        //try portion after last trailing slash
        pos = uriOrQname.lastIndexOf('/')
        if ( pos > -1 && pos < uriOrQname.length) {
            return uriOrQname.substring(pos + 1);
        }
        //take portion after last ':'
        pos = uriOrQname.lastIndexOf(':')
        if ( pos > -1 && pos < uriOrQname.length) {
            return uriOrQname.substring(pos + 1);
        }
        return uriOrQname;
    }

    won.isJsonLdKeyword = function(propertyName) {
        if (propertyName == null || typeof propertyName !== 'string') return false;
        return propertyName.indexOf('@') == 0;
    }

    won.reportError = function(message) {
        if (arguments.length == 1) {
            return function(reason) {
                console.log(message, " reason: ", reason);
            }
        } else {
            return function (reason) {
                console.log("Error! reason: ", reason);
            }
        }
    }

    won.isNull = function(value){
        return typeof(value) === 'undefined' || value == null;
    }

    //helper function: is x an array?
    won.isArray = function(x){
        return Object.prototype.toString.call( x ) === '[object Array]';
    }

    won.replaceRegExp = function (string) {
        return string.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
    }

    /**
     * Deletes every element of the array for which the
     * test function returns true.
     * @param array
     * @param test
     */
    won.deleteWhere = function(array, test) {
        for (var i = 0; i< array.length; i++){
            if (test(array[i])){
                array.splice(i,1);
                i--;
            }
        }
        return array;
    }

    won.containsAll = function (array, subArray){
        for (var skey in subArray){
            var found = false;
            for (var key in array){
                if (subArray[skey] === array[key]){
                    found = true;
                    break;
                }
            }
            if (found == false) return false;
        }
        return true;
    }

    /**
     * Deletes all null entries in the specified array.
     * @param array
     */
    won.deleteWhereNull = function(array){
        return won.deleteWhere(array, function(x){ return x == null});
    }

    /**
     * Visits the specified data structure. For each element, the callback is called
     * as callback(element, key, container) where the key is the key of the element
     * in its container or callback (element, null, null) if there is no such container).
     */
    won.visitDepthFirst = function(data, callback, currentKey, currentContainer){
        if (data == null) return;
        if (won.isArray(data) && data.length > 0){
            for (let key in data) {
                won.visitDepthFirst(data[key], callback, key, data);
            }
            return;
        }
        if (typeof data === 'object'){
            for (let key in data) {
                won.visitDepthFirst(data[key], callback, key, data);
            }
            return;
        }
        //not a container: visit value.
        callback(data, currentKey, currentContainer);
    }


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
    won.appendStrippingDuplicates = function(array1, array2, comparatorFun){
        if (typeof array1 === 'undefined') return array2;
        if (typeof array2 === 'undefined') return array1;
        if (typeof comparatorFun === 'undefined') comparatorFun = function(a,b){ return a === b};
        array2.filter(function (item) {
            for (let i = 0; i < array1.length; i++){
                if (comparatorFun(item, array1[i]) == 0) {
                    return false;
                }
            }
            return true;
        }).map(function(item){array1.push(item)});
        return array1;
    }


    won.minimalContext = {
        "msg": "http://purl.org/webofneeds/message#",
        "won": "http://purl.org/webofneeds/model#",
        "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "rdfg": "http://www.w3.org/2004/03/trix/rdfg-1/"
    }

    won.defaultContext = {
            "webID": "http://www.example.com/webids/",
            "msg": "http://purl.org/webofneeds/message#",
            "dc": "http://purl.org/dc/elements/1.1/",
            "rdfs": "http://www.w3.org/2000/01/rdf-schema#",
            "geo": "http://www.w3.org/2003/01/geo/wgs84_pos#",
            "xsd": "http://www.w3.org/2001/XMLSchema#",
            "rdf": "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "won": "http://purl.org/webofneeds/model#",
            "gr": "http://purl.org/goodrelations/v1#",
            "ldp": "http://www.w3.org/ns/ldp#",
            "rdfg": "http://www.w3.org/2004/03/trix/rdfg-1/",
            "msg:hasMessageType":{
                "@id":"http://purl.org/webofneeds/message#hasMessageType",
                "@type":"@id"
            }


    }


    won.JsonLdHelper = {
        /**
         * Returns all graph URIs. If none are found, an empty array is returned.
         * @returns {Array}
         */
        getGraphNames: function(data){
            //collect graph URIs in the specified dataset
            var graphs = data["@graph"]
            var graphURIs = [];
            if (graphs == null) {
                return graphURIs;
            }
            if (won.isArray(graphs) && graphs.length > 0) {
                for (var i = 0; i < graphs.length; i++){
                    var graphURI = graphs[i]['@id'];
                    if (graphURI != null){
                        graphURIs.push(graphURI);
                    }
                }
            } else if (typeof graphs === 'object' && graphs["@graph"] != null){
                return won.JsonLdHelper.getGraphNames(graphs['@graph']);
            }
            return graphURIs;
        },

        getDefaultGraph: function(data){
            if (data['@graph'] != null) {
              //graph keyword is present. It could represent the default graph
              // (in which case it contains only nodes) or a collection of
              //, which are nodes that contain th @graph keyword.
              //our naive test is: if the first node contains an '@graph' keyword
              //we assume the outermost @graph array to contain only named graphs.
              // we search for the one with '@id'='@default' or without
              // an '@id' keyword and return it (if we find it)
              //if the first node doesn't contain an @graph keyword, we assume that there
              //are no named graphs and all data is in the default graph.
              let outermostGraphContent = data['@graph'];
              for (var i = 0; i < outermostGraphContent.length; i++) {
                var curNode = outermostGraphContent[i];
                if (curNode['@graph'] == null){
                    //we assume there are no named graphs, the outermost graph is the default graph
                    return outermostGraphContent;
                }
                if (curNode['@id'] == null || curNode['@id'] === '@default'){
                    //we've found the named graph without an @id attribute - that's the default graph
                    return curNode['@graph'];
                }
              }
              return null; //no default graph found
            } else {
                //there is no @graph keyword at top level:
                return data;
            }
        },
        getNamedGraph: function(data, graphName) {
            if (data['@graph'] != null) {
                if (data['@id'] != null) {
                    if (data['@id'] === graphName) {
                        return data['@graph'];
                    }
                } else {
                    //outermost node has '@graph' but no '@id'
                    //--> @graph array contains named graphs. search for name.
                    let outermostGraphContent = data['@graph'];
                    for (var i = 0; i < outermostGraphContent.length; i++) {
                        var curNode = outermostGraphContent[i];
                        if (curNode['@id'] == null || curNode['@id'] === graphName) {
                            //we've found the named graph without an @id attribute - that's the default graph
                            return curNode['@graph'];
                        }
                    }
                }
            }
            return null;
        },
        getNodeInGraph: function(data, graphName, nodeId){
            var graph = this.getNamedGraph(data, graphName);
            for (let key in graph['@graph']){
                var curNode = graph['@graph'][key];
                var curNodeId = node['@id'];
                if (curNodeId === nodeId){
                    return curNode;
                }
            }
            return null;
        },
        addDataToNode: function(data, graphName, nodeId, predicate, object){
            var node = this.getNodeInGraph(data, graphName, nodeId);
            if (node != null) {
                node[predicate] = object;
            }
        },
        getContext :  function (data) {
            return data["@context"];
        }
    }

    /**
     *  Adds a msg:hasContent triple for each specified graphURI into the message graph
     * @param messageGraph
     * @param graphURIs
     */
    won.addContentGraphReferencesToMessageGraph = function(messageGraph, graphURIs){
        if (graphURIs != null) {
            if (won.isArray(graphURIs) && graphURIs.length > 0) {
                //if the message graph already contains content references, fetch them:
                var existingContentRefs = messageGraph["@graph"][0][won.WONMSG.hasContentCompacted];
                var contentGraphURIs = (typeof existingContentRefs === 'undefined' || ! isArray(existingContentRefs))? []: existingContentRefs;
                for (var i = 0; i < graphURIs.length; i++) {
                    contentGraphURIs.push({'@id': graphURIs[i]});
                }
                messageGraph["@graph"][0][won.WONMSG.hasContentCompacted] = contentGraphURIs;
            }
        }
    }

    /**
     * Adds the message graph to the json-ld structure 'builder.data' with
     * the specified messageType
     * and adds all specified graph URIs (which must be URIs of the
     * graphs to be added as content of the message) with triples
     * [message] wonmsg:hasContent [graphURI]
     * @param graphURIs
     * @returns {won.CreateMessageBuilder}
     */
    won.addMessageGraph = function (builder, graphURIs, messageType) {
        let graphs = builder.data['@graph'];
        let unsetMessageGraphUri = UNSET_URI+"#data";
        //create the message graph, containing the message type
        var messageGraph = {
            "@graph": [
                {
                    "@id":UNSET_URI,
                    "msg:hasMessageType": {'@id':messageType}
                },
                {   "@id": unsetMessageGraphUri,
                    "@type": "msg:EnvelopeGraph",
                    "rdfg:subGraphOf" : {"@id":UNSET_URI}
                }
            ],
            "@id": unsetMessageGraphUri
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
    won.newGraph = function (hashFragement) {
        hashFragement = hashFragement || 'graph1';
        return {"@graph": [
                    {
                        "@id": UNSET_URI + "#" + hashFragement,
                        "@graph": []
                    }
                ]
            };
    }


    /**
     * Builds a JSON-LD WoN Message or adds the relevant data to the specified
     * JSON-LD data structure.
     * @param messageType a fully qualified URI
     * @param content a JSON-LD structure or null
     * @constructor
     */
    won.MessageBuilder = function MessageBuilder(messageType, content) {
        if (messageType == null) {
            throw { message : "messageType must not be null!" };
        }
        var graphNames = null;
        if (content != null) {
            this.data = won.clone(content);
            graphNames = won.JsonLdHelper.getGraphNames(this.data);
        } else {
            this.data =
            {
                "@graph": [

                ],
                "@context" : won.clone(won.minimalContext)
            }
        }
        this.messageGraph = null;
        this.eventUriValue = UNSET_URI;
        won.addMessageGraph(this, graphNames , messageType);
    };

    won.MessageBuilder.prototype = {
        constructor: won.MessageBuilder,

        eventURI: function (eventUri) {
            this.getContext()[won.WONMSG.EnvelopeGraphCompacted]= {
                "@id": "http://purl.org/webofneeds/message#EnvelopeGraph",
                "@type": "@id"
            };
            var regex = new RegExp(won.replaceRegExp(this.eventUriValue));
            won.visitDepthFirst(this.data, function(element, key, collection){
                if (collection != null && key === '@id'){
                    if (element)
                    collection[key] = element.replace(regex, eventUri);
                }
            });
            this.eventUriValue = eventUri;
            return this;
        },
        getContext :  function () {
            return this.data["@context"];
        },
        forEnvelopeData: function (envelopeData){
            var node = this.getMessageEventNode();
            for (let key in envelopeData){
                node[key] = {"@id":envelopeData[key]};
            }
            return this;
        },
        hasSenderNeed: function(senderNeedURI){
            this.getMessageEventNode()[won.WONMSG.hasSenderNeedCompacted]={"@id":senderNeedURI};
            return this;
        },
        hasSenderNode: function(senderNodeURI){
            this.getMessageEventNode()[won.WONMSG.hasSenderNodeCompacted]={"@id":senderNodeURI};
            return this;
        },
        hasSender: function(senderURI){
            this.getMessageEventNode()[won.WONMSG.hasSenderCompacted]={"@id":senderURI};
            return this;
        },
        hasReceiver: function(receiverURI){
            this.getMessageEventNode()[won.WONMSG.hasReceiverCompacted]={"@id":receiverURI};
            return this;
        },
        hasReceiverNeed: function(receiverNeedURI){
            this.getMessageEventNode()[won.WONMSG.hasReceiverNeedCompacted]={"@id":receiverNeedURI};
            return this;
        },
        hasReceiverNode: function(receiverURI){
            this.getMessageEventNode()[won.WONMSG.hasReceiverNodeCompacted]={"@id":receiverURI};
            return this;
        },
        hasOwnerDirection: function() {
            this.getMessageEventNode()["@type"]=won.WONMSG.FromOwnerCompacted;
            return this;
        },
        hasSentTimestamp: function(timestamp) {
            this.getMessageEventNode()["msg:hasSentTimestamp"]=timestamp;
            return this;
        },
        /**
         * Adds the specified facet as local facets. Only needed for connect and
         * openSuggested.
         * @param receiverURI
         * @returns {won.MessageBuilder}
         */
        hasFacet: function(facetURI){
            this.getContentGraphNode()[won.WON.hasFacetCompacted]={"@id":facetURI};
            return this;
        },
        /**
         * Adds the specified facet as local facets. Only needed for connect and
         * openSuggested.
         * @param receiverURI
         * @returns {won.MessageBuilder}
         */
        hasRemoteFacet: function(facetURI){
            this.getContentGraphNode()[won.WON.hasRemoteFacetCompacted]={"@id":facetURI};
            return this;
        },
        /**
         * Adds the specified text as text message inside the content. Can be
         * used with connectMessage, openMessage and connectionMessage.
         * @param text - text of the message
         * @returns {won.MessageBuilder}
         */
        hasTextMessage: function (text){
            if (text == null || text === ""){
                // text is either null, undefined, or empty
                // do nothing
            } else {
                this.getContentGraphNode()[won.WON.hasTextMessageCompacted] = text;
            }
            return this;
        },

        getMessageEventGraph: function (){
            return this.messageGraph;
        },
        getMessageEventNode: function () {
            return this.getMessageEventGraph()["@graph"][0];
        },
        /**
         * Fetches the content graph, creating it if it doesn't exist.
         */
        getContentGraph: function(){
            var graphs = this.data["@graph"];
            var contentGraphUri = this.eventUriValue + "#content";
            for (let key in graphs){
                var graph = graphs[key];
                if (graph['@id'] === contentGraphUri){
                    return graph;
                }
            }
            //none found: create it
            var contentGraph = {
                "@id": this.eventUriValue + "#content",
                "@graph" :[
                    {"@id": this.eventUriValue}
                ]
            }
            graphs.push(contentGraph);
            //add a reference to it to the envelope
            won.addContentGraphReferencesToMessageGraph(this.messageGraph, [contentGraphUri]);
            return contentGraph;
        },
        getContentGraphNode: function(){
            return this.getContentGraph()["@graph"][0];
        },
        addContentGraphData: function(predicate, object){
            this.getContentGraphNode()[predicate] = object;
            return this;
        },
        addRating: function(rating, connectionUri){
            this.getContentGraphNode()[won.WON.hasFeedback] = {
                "@id" : "_:b0",
                "http://purl.org/webofneeds/model#forResource" : {
                    "@id" : connectionUri
                },
                "http://purl.org/webofneeds/model#hasBinaryRating" : {
                    "@id" : rating
                }
            };
            return this;
        },
        build: function () {
            console.log("built this message:" + JSON.stringify(this.data));
            return this.data;
        }
    };

    //TODO replace with `export default` after switching everything to ES6-module-syntax
export default won;
