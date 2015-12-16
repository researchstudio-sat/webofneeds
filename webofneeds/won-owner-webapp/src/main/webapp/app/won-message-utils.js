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





export function buildCreateMessage(need, wonNodeUri) {

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
    return [msgJson, msgUri];
}

export function getEventData(json) {
    console.log('getting data from jsonld message');

    const eventData = {};
    //call handler if there is one - it may modify the event object
    //frame the incoming jsonld to get the data that interest us
    const frame = {
        "@context" : {
            "won":"http://purl.org/webofneeds/model#",
            "msg":"http://purl.org/webofneeds/message#"
        },
        "@type": "msg:FromOwner"
    }
    jsonld.promises.frame(json, frame).then(framed => console.log('framed: ', framed))

    //copy data from the framed message to the event object
    let framedMessage = jsonld.frame(json, frame, {}, (args) => console.log('jsonld.frame: ', args));

    if (framedMessage == null){
        //not FromSystem? maybe it's FromSystem?
        frame['@type'] = "msg:FromSystem";
        //copy data from the framed message to the event object
        framedMessage = jsonld.frame(json, frame, {}, (args) => console.log('jsonld.frame: ', args));
    }

    if (framedMessage == null){
        //not FromSystem? maybe it's FromExternal?
        frame['@type'] = "msg:FromExternal";
        //copy data from the framed message to the event object
        framedMessage = jsonld.frame(json, frame, {}, (args) => console.log('jsonld.frame: ', args));
    }

    for (key in framedMessage){
        const propName = won.getLocalName(key);
        if (propName != null && ! won.isJsonLdKeyword(propName)) {
            eventData[propName] = won.getSafeJsonLdValue(framedMessage[key]);
        }
    }
    eventData.uri = won.getSafeJsonLdValue(framedMessage);
    eventData.framedMessage = framedMessage;
    console.log('done copying the data to the event object, returning the result');

    return eventData;
}

