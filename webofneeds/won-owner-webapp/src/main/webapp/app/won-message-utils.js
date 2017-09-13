/**
 * Created by ksinger on 03.12.2015.
 */


import won from './won-es6.js';
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
    getIn,
} from './utils.js';

import {
    actionTypes,
} from './actions/actions.js';

import jsonld from 'jsonld';

export const emptyDataset = Immutable.fromJS({
    ownNeeds: {},
    connections: {},
    events: {},
    theirNeeds: {},
});

export function wellFormedPayload (payload) {
    return emptyDataset.mergeDeep(Immutable.fromJS(payload));
}

export function buildRateMessage(msgToRateFor, rating){
    return new Promise((resolve, reject) => {
        var buildMessage = function(envelopeData, eventToRateFor) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
            var message = new won.MessageBuilder(won.WONMSG.feedbackMessage) //TODO: Looks like a copy-paste-leftover from connect
                .eventURI(eventUri)
                .hasOwnerDirection()
                .forEnvelopeData(envelopeData)
                .hasSentTimestamp(new Date().getTime().toString())
                .addRating(rating, msgToRateFor.connection.uri)
                .build();
            //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.OPEN_SENT);
            return {eventUri:eventUri,message:message};
        };

        //fetch all data needed
        won.getEnvelopeDataforConnection(msgToRateFor.connection.uri)
            .then(function(envelopeData){
                resolve(buildMessage(envelopeData, msgToRateFor.event));
            },
            won.reportError("cannot open connection " + msgToRateFor.connection.uri)
        );
    });
}

export function buildCloseMessage(msgToConnectFor){
    return new Promise((resolve, reject) => {
        var buildMessage = function (envelopeData, eventToConnectFor) {
            //TODO: use event URI pattern specified by WoN node
            var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomPosInt();
            var message = new won.MessageBuilder(won.WONMSG.closeMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasOwnerDirection()
                .hasSentTimestamp(new Date().getTime().toString())
                .build();
            //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CLOSE_SENT);
            return {eventUri: eventUri, message: message};
        }

        //fetch all data needed
        won.getEnvelopeDataforConnection(msgToConnectFor.connection.uri)
            .then(function (envelopeData) {
                resolve(buildMessage(envelopeData, msgToConnectFor.event));
            },
            won.reportError("cannot open connection " + msgToConnectFor.connection.uri)
        );
    });

}
export function buildCloseNeedMessage(needUri, wonNodeUri){
    const buildMessage = function(envelopeData) {
        var eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
        var message = new won.MessageBuilder(won.WONMSG.closeNeedMessage)
            .eventURI(eventUri)
            .hasReceiverNode(wonNodeUri)
            .hasOwnerDirection()
            .hasSentTimestamp(new Date().getTime().toString())
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
            .hasSentTimestamp(new Date().getTime().toString())
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
    return new Promise((resolve, reject) => {
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
                .hasSentTimestamp(new Date().getTime().toString())
                .build();
            //var callback = createMessageCallbackForRemoteNeedMessage(eventUri, won.EVENT.CONNECT_SENT);
            return {eventUri:eventUri,message:message};
        }

        //fetch all data needed
        won.getEnvelopeDataforConnection(msgToConnectFor.connection.uri)
            .then(function(envelopeData){
                resolve(buildMessage(envelopeData, msgToConnectFor.event));
            },
            won.reportError("cannot open connection " + msgToConnectFor.connection.uri)
        );
    })

}

export function buildChatMessage(chatMessage, connectionUri) {
    const messageP =
        won.getEnvelopeDataforConnection(connectionUri)
        .then(envelopeData => {
            const eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" + getRandomPosInt();

            /*
             * Build the json-ld message that's signed on the owner-server
             * and then send to the won-node.
             */
            const message = new won.MessageBuilder(won.WONMSG.connectionMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .addContentGraphData(won.WON.hasTextMessage, chatMessage)
                .hasOwnerDirection()
                .hasSentTimestamp(new Date().getTime().toString())
                .build();

            return {
                eventUri,
                message
            }
        })
    return messageP;

}

export function buildOpenMessage(connectionUri, chatMessage) {
    const messageP = won
        .getEnvelopeDataforConnection(connectionUri)
        .then(envelopeData => {

            //TODO: use event URI pattern specified by WoN node
            const eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt();
            const message = new won.MessageBuilder(won.WONMSG.openMessage)
                .eventURI(eventUri)
                .forEnvelopeData(envelopeData)
                .hasFacet(won.WON.OwnerFacet) //TODO: check. looks like a copy-paste-leftover from connect
                .hasRemoteFacet(won.WON.OwnerFacet)//TODO: check. looks like a copy-paste-leftover from connect
                .hasTextMessage(chatMessage)
                .hasOwnerDirection()
                .hasSentTimestamp(new Date().getTime().toString())
                .build();

            return {
                eventUri,
                message,
            }

        });

    return messageP;
}

/**
 *
 * @param needData
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
export function buildCreateMessage(needData, wonNodeUri) {
    if(!needData.type || !needData.title)
        throw new Error('Tried to create post without type or title. ', needData);

    const publishedContentUri = wonNodeUri + '/need/' + getRandomPosInt();

    const imgs = needData.images;
    let attachmentUris = []
    if(imgs) {
        imgs.forEach(function(img) { img.uri = wonNodeUri + '/attachment/' + getRandomPosInt(); })
        attachmentUris = imgs.map(function(img) { return img.uri });
    }

    //if type === create -> use needBuilder as well

    const contentRdf = won.buildNeedRdf({
        type : won.toCompacted(needData.type), //mandatory
        title: needData.title, //mandatory
        description: needData.description,
        publishedContentUri: publishedContentUri, //mandatory
        tags: needData.tags,
        attachmentUris: attachmentUris, //optional, should be same as in `attachments` below
        longitude: getIn(needData, ['location', 'lon']),
        latitude: getIn(needData, ['location', 'lat']),
        address: getIn(needData, ['location', 'name']),
        bounds: getIn(needData, ['location', 'bounds']),
        whatsAround: needData.whatsAround,
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
            for (var key in framedSimplifiedMessage){
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

export function fetchDataForNonOwnedNeedOnly(needUri) {
    return won.getNeedWithConnectionUris(needUri)
    .then(need =>
            emptyDataset
                .setIn(['theirNeeds', needUri], Immutable.fromJS(need))
                .set('loggedIn', false)
    )
}

export function fetchOwnedData(email, curriedDispatch) {
    return fetchOwnedNeedUris()
        .then(needUris =>
            fetchDataForOwnedNeeds(needUris, curriedDispatch)
        );
}
//export function fetchDataForOwnedNeeds(needUris, curriedDispatch) {
//    return fetchAllAccessibleAndRelevantData(needUris, curriedDispatch)
//        .catch(error => {
//            throw({msg: 'user needlist retrieval failed', error});
//        });
//}
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
export const fetchDataForOwnedNeeds = fetchAllAccessibleAndRelevantData;
function fetchAllAccessibleAndRelevantData(ownNeedUris, curriedDispatch = () => undefined) {
    if(!is('Array', ownNeedUris) || ownNeedUris.length === 0 ) {
        return Promise.resolve(emptyDataset);
    }

    const allLoadedPromise = Promise.all(
        ownNeedUris.map(uri => won.ensureLoaded(uri, { requesterWebId: uri, deep: true }))
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
                urisToLookupMap(connectionUris, won.getNode));

        /*
        // STARTING with selective loading
        const allEventsPromise = allConnectionUrisPromise
            .then(connectionUris => //expects an array
                urisToLookupMap(connectionUris, connectionUri =>
                        won.getConnectionWithEventUris(connectionUri)
                            .then(connection =>
                                won.getEventsOfConnection(connectionUri,connection.belongsToNeed)
                        )
                )
        ).then(eventsOfConnections =>
                //eventsPerConnection[connectionUri][eventUri]
                flattenObj(eventsOfConnections)
        );
        */

        const allTheirNeedsPromise =
            allConnectionsPromise.then(connections => {
                const theirNeedUris = [];
                for(const [connectionUri, connection] of entries(connections)) {
                    theirNeedUris.push(connection.hasRemoteNeed);
                }
                return Immutable.Set(theirNeedUris).toArray();
            })
                .then(theirNeedUris =>
                    urisToLookupMap(theirNeedUris, won.getTheirNeed));

        //dispatch to the curried-in action as soon as any part of the data arrives
        const ownNeedsDispatchedP = allOwnNeedsPromise.then(ownNeeds => curriedDispatch({ownNeeds}));

        //Is needed for the reducer to make sure that all needs have already been put in the state
        Promise.all([
            ownNeedsDispatchedP,
            allConnectionsPromise,
        ]).then(([x, connections]) => curriedDispatch({connections}));

        //allEventsPromise.then(events => dispatchWellFormed({events})); // STARTING with selective loading
        allTheirNeedsPromise.then(theirNeeds => curriedDispatch({theirNeeds}));

        return Promise.all([
            allOwnNeedsPromise,
            allConnectionsPromise,
            //allEventsPromise, // STARTING with selective loading
            allTheirNeedsPromise
        ]);
    });

    return allDataRawPromise
        .then(([ ownNeeds, connections, /* events, */ theirNeeds ]) =>
            wellFormedPayload({
                ownNeeds, connections,
                events: {/* will be loaded later when connection is accessed */},
                theirNeeds,
            })
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

