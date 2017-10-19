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

export function buildRateMessage(msgToRateFor, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri, rating){
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
        won.getEnvelopeDataforConnection(msgToRateFor.connection.uri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri)
            .then(function(envelopeData){
                resolve(buildMessage(envelopeData, msgToRateFor.event));
            },
            won.reportError("cannot open connection " + msgToRateFor.connection.uri)
        );
    });
}

export function buildCloseMessage(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri){
    var buildMessage = function (envelopeData) {
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
    };

    //fetch all data needed
    return won.getEnvelopeDataforConnection(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri)
    .then( envelopeData =>
        buildMessage(envelopeData)
    )
    .catch(err => {
        won.reportError("cannot close connection " + connectionUri +": " + JSON.stringify(err))
        throw err;
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

    return won.getEnvelopeDataForNeed(needUri, wonNodeUri)
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

    return won.getEnvelopeDataForNeed(needUri, wonNodeUri)
        .then(
            envelopeData => buildMessage(envelopeData),
            err => won.reportError("cannot close need "+ needUri)
    );
}


/**
 * Creates json-ld for a connect-message, where there's no connection yet (i.e. we
 * only know which own we want to connect with which remote need)
 * @param ownNeedUri
 * @param theirNeedUri
 * @param textMessage
 * @returns {{eventUri, message}|*}
 */
export async function buildAdHocConnectMessage(ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, textMessage) {
    return won.getEnvelopeDataforNewConnection(ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri)
    .then(envelopeData =>
        buildConnectMessageForEnvelopeData(envelopeData, textMessage)
    );
}

/**
 * Builds json-ld for a connect-message in reaction to a need.
 * @param connectionUri
 * @param textMessage
 * @returns {{eventUri, message}|*}
 */
export async function buildConnectMessage(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri, textMessage){
    return won.getEnvelopeDataforConnection(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri)
    .then(envelopeData =>
        buildConnectMessageForEnvelopeData(envelopeData, textMessage)
    );
}

function buildConnectMessageForEnvelopeData(envelopeData, textMessage) {
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

export function buildChatMessage(chatMessage, connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri) {
    const messageP =
        won.getEnvelopeDataforConnection(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri)
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

export function buildOpenMessage(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri, chatMessage) {
    const messageP = won
        .getEnvelopeDataforConnection(connectionUri, ownNeedUri, theirNeedUri, ownNodeUri, theirNodeUri, theirConnectionUri)
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
        location: getIn(needData, ['location']),
        whatsAround: needData.whatsAround,
        noHints: needData.noHints,
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

export function isSuccessMessage(event) {
    return event.hasMessageType === won.WONMSG.successResponseCompacted;
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

    const allOwnNeedsPromise = urisToLookupMap(ownNeedUris, uri =>
        fetchOwnNeedAndDispatch(uri, curriedDispatch)
    );

    // wait for the own needs to be dispatched then load connections
    const allConnectionsPromise = allOwnNeedsPromise.then(() =>
        Promise.all(ownNeedUris.map(uri =>
            won.getConnectionUrisOfNeed(uri)
                .then(connectionUris =>
                    urisToLookupMap(connectionUris, uri =>
                            fetchConnectionAndDispatch(uri, curriedDispatch)
                    )
            )
        ))
    )
    .then(connectionMaps /*[{ uri -> cnct }]*/ =>
        // flatten into one lookup map
        connectionMaps.reduce((a,b) => Object.assign(a,b), {})
    );

    const allTheirNeedsPromise =
        allConnectionsPromise.then(connections => {
            const theirNeedUris = Object.values(connections)
                .map(cnct => cnct.hasRemoteNeed);

            return Immutable.Set(theirNeedUris).toArray();
        })
        .then(theirNeedUris =>
            urisToLookupMap(theirNeedUris, uri =>
                fetchTheirNeedAndDispatch(uri, curriedDispatch)
            )
        );

    const allDataRawPromise = Promise.all([
            allOwnNeedsPromise,
            allConnectionsPromise,
            //allEventsPromise, // STARTING with selective loading
            allTheirNeedsPromise
        ]);

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


function fetchOwnNeedAndDispatch(needUri, curriedDispatch = () => undefined) {
    const needP =  won.ensureLoaded(needUri, {requesterWebId: needUri, deep: true}) //ensure loaded does net seem to be necessary as it is called within getNeed also the requesterWebId is not necessary for need requests
        .then(() =>
            won.getNeed(needUri)
        );
    needP.then(need =>
        curriedDispatch(
            wellFormedPayload({ownNeeds: {[needUri]: need}})
        )
    );
    return needP
}

function fetchConnectionAndDispatch(cnctUri, curriedDispatch = () => undefined) {
    const cnctP = won.getNode(cnctUri);
    cnctP.then(connection =>
        curriedDispatch(
            wellFormedPayload({connections: {[cnctUri]: connection}})
        )
    );
    return cnctP;
}

function fetchTheirNeedAndDispatch(needUri, curriedDispatch = () => undefined) {
    const needP = won.getNeed(needUri);
    needP.then(need =>
        curriedDispatch(
            wellFormedPayload({theirNeeds: {[needUri]: need}})
        )
    );
    return needP
}
