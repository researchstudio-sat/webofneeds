/**
 * Created by ksinger on 03.12.2015.
 */


import won from './won-es6';
import Immutable from 'immutable';
import {
    getRandomPosInt,
    checkHttpStatus,
    mapJoin,
    urisToLookupMap,
    flattenObj,
    flatten,
    entries,
    is,
} from './utils';

import jsonld from 'jsonld';
window.jsonld4Dbg = jsonld;

/*
    fetch('rest/users/isSignedIn', {credentials: 'include'}) //TODO send credentials along
        .then(checkStatus)
        .then(resp => resp.json())
        .then(data =>
            dispatch(actionCreators.user__receive({
                loggedIn: true,
                email: data.username
            }))
    )
*/
//TODO cached/memoized promise?
/*
var ret = buildCreateMessage(need, state.getIn['config', 'defaultNodeUri']);
var message = ret[0];
var eventUri = ret[1];

send(message);

callback.shouldHandleTest = function (event, msg) {
    var ret = event.isResponseTo == eventUri;
    $log.debug("event " + event.uri + " refers to event " + this.msgURI + ": " + ret);
    return ret;
};

messageService.sendMessage = function(msg) {
    var jsonMsg = JSON.stringify(msg);
    if (isConnected()) {
        privateData.socket.send(jsonMsg);
    } else {
        if (!isConnecting()) {
            createSocket();
        }

        if (isConnected()) {
            $log.debug("sending message instead of enqueueing");
            //just to be sure, test if the connection is established now and send instead of enqueue
            privateData.socket.send(jsonMsg);
        } else {
            $log.warn("socket not connected yet, enqueueing");
            privateData.pendingOutMessages.push(jsonMsg);
        }
    }
};

*/

export function buildRateMessage(msgToRateFor, rating){
    let deferred = Q.defer();
    var buildMessage = function(envelopeData, eventToRateFor) {
        //TODO: use event URI pattern specified by WoN node
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.feedbackMessage) //TODO: Looks like a copy-paste-leftover from connect
            .eventURI(eventUri)
            .hasOwnerDirection()
            .forEnvelopeData(envelopeData)
            .hasSentTimestamp(new Date().getTime())
            .addRating(rating, msgToRateFor.connection.uri)
            .build();
        //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.OPEN_SENT);
        return {eventUri:eventUri,message:message};
    };

    //fetch all data needed
    won.getEnvelopeDataforConnection(msgToRateFor.connection.uri)
        .then(function(envelopeData){
            deferred.resolve(buildMessage(envelopeData, msgToRateFor.event));
        },
        won.reportError("cannot open connection " + msgToRateFor.connection.uri)
    );
    return deferred.promise;
}

export function buildCloseMessage(msgToConnectFor){
    let deferred = Q.defer();
    var buildMessage = function(envelopeData, eventToConnectFor) {
        //TODO: use event URI pattern specified by WoN node
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.closeMessage)
            .eventURI(eventUri)
            .forEnvelopeData(envelopeData)
            .hasOwnerDirection()
            .hasSentTimestamp(new Date().getTime())
            .build();
        //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CLOSE_SENT);
        return {eventUri:eventUri,message:message};
    }

    //fetch all data needed
    won.getEnvelopeDataforConnection(msgToConnectFor.connection.uri)
        .then(function(envelopeData){
            deferred.resolve(buildMessage(envelopeData, msgToConnectFor.event));
        },
        won.reportError("cannot open connection " + msgToConnectFor.connection.uri)
    );
    return deferred.promise;

}
export function buildCloseNeedMessage(needUri, wonNodeUri){
    const buildMessage = function(envelopeData) {
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.closeNeedMessage)
            .eventURI(eventUri)
            .hasReceiverNode(wonNodeUri)
            .hasOwnerDirection()
            .hasSentTimestamp(new Date().getTime())
            .forEnvelopeData(envelopeData)
            .build();

        return {eventUri: eventUri, message: message};
    };

    return won.getEnvelopeDataForNeed(needUri)
        .then(
            envelopeData => buildMessage(envelopeData),
            err => won.reportError("cannot close need "+ needUri)
        );
}

export function buildOpenNeedMessage(needUri, wonNodeUri){
    const buildMessage = function(envelopeData) {
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.activateNeedMessage)
            .eventURI(eventUri)
            .hasReceiverNode(wonNodeUri)
            .hasOwnerDirection()
            .hasSentTimestamp(new Date().getTime())
            .forEnvelopeData(envelopeData)
            .build();

        return {eventUri: eventUri, message: message};
    };

    return won.getEnvelopeDataForNeed(needUri)
        .then(
            envelopeData => buildMessage(envelopeData),
            err => won.reportError("cannot close need "+ needUri)
    );
}



export function buildConnectMessage(msgToConnectFor, textMessage){
    let deferred = Q.defer();
    var buildMessage = function(envelopeData, eventToConnectFor) {
        //TODO: use event URI pattern specified by WoN node
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.connectMessage)
            .eventURI(eventUri)
            .forEnvelopeData(envelopeData)
            .hasFacet(won.WON.OwnerFacet) //TODO: looks like a copy-paste-leftover from connect
            .hasRemoteFacet(won.WON.OwnerFacet)//TODO: looks like a copy-paste-leftover from connect
            .hasTextMessage(textMessage)
            .hasOwnerDirection()
            .hasSentTimestamp(new Date().getTime())
            .build();
        //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CONNECT_SENT);
        return {eventUri:eventUri,message:message};
    }

    //fetch all data needed
    won.getEnvelopeDataforConnection(msgToConnectFor.connection.uri)
        .then(function(envelopeData){
            deferred.resolve(buildMessage(envelopeData, msgToConnectFor.event));
        },
        won.reportError("cannot open connection " + msgToConnectFor.connection.uri)
    );
    return deferred.promise;

}


export function buildOpenMessage(msgToOpenFor, textMessage){
    let deferred = Q.defer();
    var buildMessage = function(envelopeData, eventToOpenFor) {
        //TODO: use event URI pattern specified by WoN node
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.openMessage)
            .eventURI(eventUri)
            .forEnvelopeData(envelopeData)
            .hasFacet(won.WON.OwnerFacet) //TODO: looks like a copy-paste-leftover from connect
            .hasRemoteFacet(won.WON.OwnerFacet)//TODO: looks like a copy-paste-leftover from connect
            .hasTextMessage(textMessage)
            .hasOwnerDirection()
            .hasSentTimestamp(new Date().getTime())
            .build();
        //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.OPEN_SENT);
        return {eventUri:eventUri,message:message};
    }

    //fetch all data needed
    won.getEnvelopeDataforConnection(msgToOpenFor.connection.uri)
        .then(function(envelopeData){
            deferred.resolve(buildMessage(envelopeData, msgToOpenFor.event));
        },
        won.reportError("cannot open connection " + msgToOpenFor.connection.uri)
    );
    return deferred.promise;

}

/**
 *
 * @param need
 * @param wonNodeUri
 * @return {{
 *    message: (
 *      {
 *          @id,
 *          msg:hasDestinationUri,
 *          msg:hasAttachmentGraphUri
 *      }|
 *      {@id}|
 *      {@graph, @context}
 *    ),
 *    eventUri: string,
 *    needUri: string
 * }}
 */
export function buildCreateMessage(need, wonNodeUri) {
    if(!need.type || !need.title)
        throw new Error('Tried to create post without type or title. ', need);

    const publishedContentUri = wonNodeUri + '/need/' + getRandomPosInt();

    const imgs = need.images;
    let attachmentUris = []
    if(imgs) {
        imgs.forEach(function(img) { img.uri = wonNodeUri + '/attachment/' + getRandomPosInt(); })
        attachmentUris = imgs.map(function(img) { return img.uri });
    }

    //if type === create -> use needBuilder as well

    const contentRdf = won.buildNeedRdf({
        type : won.toCompacted(need.type), //mandatory
        title: need.title, //mandatory
        description: need.description,
        publishedContentUri: publishedContentUri, //mandatory
        tags: need.tags,
        attachmentUris: attachmentUris, //optional, should be same as in `attachments` below
    });
    const msgUri = wonNodeUri + '/event/' + getRandomPosInt(); //mandatory
    const msgJson = won.buildMessageRdf(contentRdf, {
        receiverNode : wonNodeUri, //mandatory
        senderNode : wonNodeUri, //mandatory
        msgType : won.WONMSG.createMessage, //mandatory
        publishedContentUri: publishedContentUri, //mandatory
        msgUri: msgUri,
        attachments: imgs //optional, should be same as in `attachmentUris` above
    });
    return {
        message: msgJson,
        eventUri: msgUri,
        needUri: publishedContentUri,
    };
}
export  function setCommStateFromResponseForLocalNeedMessage(event) {
    if (isSuccessMessage(event)){
        event.commState = won.COMMUNUCATION_STATE.ACCEPTED;
    } else {
        event.commState = won.COMMUNUCATION_STATE.NOT_TRANSMITTED;
    }
}
export function isSuccessMessage(event) {
    return event.hasMessageType === won.WONMSG.successResponseCompacted;
}

export function getEventsFromMessage(msgJson) {
    const eventData = {};
    //call handler if there is one - it may modify the event object
    //frame the incoming jsonld to get the data that interest us

    const acceptedSources = [ 'msg:FromOwner', 'msg:FromSystem', 'msg:FromExternal' ];

    const framingAttempts = acceptedSources.map(source =>
        jsonld.promises.frame(msgJson, {
            '@context': {
                'won': 'http://purl.org/webofneeds/model#',
                'msg': 'http://purl.org/webofneeds/message#'
            },
            '@type': source
        }));

    const maybeFramedMsgs = Promise.all(framingAttempts)
        .then(framedMessages => {
            let acc = {};
            framedMessages.forEach((msg, i) => {
                //filter out failed framing attempts
                if( msg['@graph'].length > 0) {
                    acc[acceptedSources[i]] = msg;
                }
            });
            return acc;
        })
        .then(msgFramings => {
            /* check framing results for failures */
            if(Object.keys(msgFramings).length < 1) {
                /* Not a valid type */
                const e = new Error('Tried to jsond-ld-frame the message ', msgJson,
                    ' but it\'s type was neither of the following, accepted types: ',
                    acceptedSources );
                e.msgJson = msgJson;
                e.acceptedSources = acceptedSources;
                e.framedMessages = msgFramings;
                throw e;
            }
            else {
                return msgFramings;
            }
        });

    const simplifiedEvents = maybeFramedMsgs.then(framedMessages =>
        mapJoin(framedMessages, framedMessage => {
            const framedSimplifiedMessage = Object.assign(
                { '@context': framedMessage['@context'] }, //keep context
                framedMessage['@graph'][0] //use first node - the graph should only consist of one node at this point
            );
            let eventData = {};
            for (key in framedSimplifiedMessage){
                const propName = won.getLocalName(key);
                if (propName != null && ! won.isJsonLdKeyword(propName)) {
                    eventData[propName] = won.getSafeJsonLdValue(framedSimplifiedMessage[key]);
                }
            }
            eventData.uri = won.getSafeJsonLdValue(framedSimplifiedMessage);
            eventData.framedMessage = framedSimplifiedMessage;
            return eventData;
        })
    );
    return simplifiedEvents;
}

const emptyDataset = Immutable.fromJS({
    ownNeeds: {},
    connections: {},
    events: {},
    theirNeeds: {},
});

function wellFormedPayload(payload) {
    return emptyDataset.mergeDeep(Immutable.fromJS(payload));
}

export function fetchDataForNonOwnedNeedOnly(needUri) {
    return won.getNeed(needUri)
    .then(need =>
            emptyDataset
                .setIn(['theirNeeds', needUri], Immutable.fromJS(need))
                .set('loggedIn', false)
    )
}

export function fetchOwnedData(email, curriedDispatch) {
    return fetchOwnedNeedUris()
        .then(needUris =>
            fetchDataForOwnedNeeds(needUris, email, curriedDispatch)
        );
}
export function fetchDataForOwnedNeeds(needUris, email, curriedDispatch) {

    const dataPromise =
        fetchAllAccessibleAndRelevantData(needUris, curriedDispatch)
        .catch(error => {
            throw({msg: 'user needlist retrieval failed', error});
        });

    if(email) {
        const userData = {loggedIn: true, email};
        if(curriedDispatch) {
            curriedDispatch(wellFormedPayload(userData));
        }
        return dataPromise.then(allThatData =>
            allThatData.merge(Immutable.fromJS(userData))
        )
    } else {
        return dataPromise;
    }
}
function fetchOwnedNeedUris() {
    return fetch('/owner/rest/needs/', {
            method: 'get',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        })
        .then(checkHttpStatus)
        .then(response =>
            response.json()
        )
}

window.fetchAll4dbg = fetchAllAccessibleAndRelevantData;
function fetchAllAccessibleAndRelevantData(ownNeedUris, curriedDispatch = () => undefined) {
    if(!is('Array', ownNeedUris) || ownNeedUris.length === 0 ) {
        return Promise.resolve(emptyDataset);
    }

    dispatchWellFormed = (payload) => curriedDispatch(wellFormedPayload(payload));

    const allLoadedPromise = Promise.all(
        ownNeedUris.map(uri => won.ensureLoaded(uri, uri, deep = true))
    );

    const allDataRawPromise = allLoadedPromise.then(() => {
        const allOwnNeedsPromise = urisToLookupMap(ownNeedUris,
            won.getNeedWithConnectionUris);

        const allConnectionUrisPromise =
            Promise.all(ownNeedUris.map(won.getconnectionUrisOfNeed))
                .then(connectionUrisPerNeed =>
                    flatten(connectionUrisPerNeed));

        const allConnectionsPromise = allConnectionUrisPromise
            .then(connectionUris => //delivers an obj<idx, string>
                urisToLookupMap(connectionUris, won.getConnection));

        const allEventsPromise = allConnectionUrisPromise
            .then(connectionUris => //expects an array
                urisToLookupMap(connectionUris, connectionUri =>
                        won.getConnection(connectionUri)
                            .then(connection =>
                                won.getEventsOfConnection(connectionUri,connection.belongsToNeed)
                        )
                )
        ).then(eventsOfConnections =>
                //eventsPerConnection[connectionUri][eventUri]
                flattenObj(eventsOfConnections)
        );

        const allTheirNeedsPromise =
            allConnectionsPromise.then(connections => {
                const theirNeedUris = [];
                for(const [connectionUri, connection] of entries(connections)) {
                    theirNeedUris.push(connection.hasRemoteNeed);
                }
                return theirNeedUris;
            })
                .then(theirNeedUris =>
                    urisToLookupMap(theirNeedUris, won.getNeed));

        //dispatch to the curried-in action as soon as any part of the data arrives
        allOwnNeedsPromise.then(ownNeeds => dispatchWellFormed({ownNeeds}));
        allConnectionsPromise.then(connections => dispatchWellFormed({connections}));
        allEventsPromise.then(events => dispatchWellFormed({events}));
        allTheirNeedsPromise.then(theirNeeds => dispatchWellFormed({theirNeeds}));

        return Promise.all([
            allOwnNeedsPromise,
            allConnectionsPromise,
            allEventsPromise,
            allTheirNeedsPromise
        ]);
    });

    return allDataRawPromise
        .then(([ ownNeeds, connections, events, theirNeeds ]) =>
            wellFormedPayload({ ownNeeds, connections, events, theirNeeds, })
        );

    /**
     const allAccessibleAndRelevantData = {
        ownNeeds: {
            <needUri> : {
                *:*,
                connections: [<connectionUri>, <connectionUri>]
            }
            <needUri> : {
                *:*,
                connections: [<connectionUri>, <connectionUri>]
            }
        },
        theirNeeds: {
            <needUri>: {
                *:*,
                connections: [<connectionUri>, <connectionUri>] <--?
            }
        },
        connections: {
            <connectionUri> : {
                *:*,
                events: [<eventUri>, <eventUri>]
            }
            <connectionUri> : {
                *:*,
                events: [<eventUri>, <eventUri>]
            }
        }
        events: {
            <eventUri> : { *:* },
            <eventUri> : { *:* }
        }
     }
     */
}
