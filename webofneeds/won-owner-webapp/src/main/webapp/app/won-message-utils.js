/**
 * Created by ksinger on 03.12.2015.
 */


import { getRandomPosInt, checkHttpStatus } from './utils';
import won from './won-es6';

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
        description: need.textDescription,
        publishedContentUri: publishedContentUri, //mandatory
        tags: need.tags? need.tags.map(function(t) {return t.text}).join(',') : undefined,
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
var isSuccessMessage = function isSuccessMessage(event) {
    return event.hasMessageType === won.WONMSG.successResponseCompacted;
}
export function getEventData(msgJson) {
    console.log('getting data from jsonld message');

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

    const maybeFramedMsg = Promise.all(framingAttempts)
        .then(framedMessages =>
            //filter out failed framing attempts
            framedMessages.filter(msg => msg['@graph'].length > 0)
        )
        .then(msgFramings => {
            /* check framing results for failures */
            if(msgFramings.length < 1) {
                /* Not a valid type */
                const e = new Error('Tried to jsond-ld-frame the message ', msgJson,
                    ' but it\'s type was neither of the following, accepted types: ',
                    acceptedSources );
                e.msgJson = msgJson;
                e.acceptedSources = acceptedSources;
                e.framedMessages = msgFramings;
                throw e;
            }
/*            else if(msgFramings.length > 1) {

                /!* Multiple type declarations -> not valid json-ld *!/
                const e = new Error('The framing found ' + msgFramings.length +
                    'message types. Either the message wasn\'t valid json-ld or the ' +
                    'framing has a bug. Please open a github issue at ' +
                    'https://github.com/researchstudio-sat/webofneeds/issues/ in that ' +
                    'case with all message. \n message before framing: ' + msgJson,
                    '\nmessage after framing: ' + msgFramings
                    );
                e.msgJson = msgJson;
                e.acceptedSources = acceptedSources;
                e.framedMessages = msgFramings;
                throw e;
            } */
            else {
                return msgFramings[0];
            }
        });

    return maybeFramedMsg.then(framedMessage => {
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
    }) ;
}
