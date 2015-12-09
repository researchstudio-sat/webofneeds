/**
 * Created by ksinger on 03.12.2015.
 */


import { getRandomPosInt, checkHttpStatus } from './utils';
import won from './won-es6';


/*
const relativePathToConfig = 'appConfig/getDefaultWonNodeUri';
fetch(relativePathToConfig).then(checkHttpStatus)
    .then(resp => resp.json())
    .catch(err => {
        const defaultNodeUri = `${location.protocol}://${location.host}/won/resource`;
        console.info(
            'Failed to fetch default node uri at the relative path `',
            relativePathToConfig,
            '` (is the API endpoint there up and reachable?) -> falling back to the default of ',
            defaultNodeUri
        );
        return defaultNodeUri
    });

*/

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
var ret = buildCreateMessage(need, wonService.getDefaultWonNodeUri());
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





function buildCreateMessage(need, wonNodeUri) {

    var publishedContentUri = wonNodeUri + '/need/' + getRandomPosInt();

    var imgs = need.images;
    var attachmentUris = []
    if(imgs) {
        imgs.forEach(function(img) { img.uri = wonNodeUri + '/attachment/' + getRandomPosInt(); })
        attachmentUris = imgs.map(function(img) { return img.uri });
    }

    //if type === create -> use needBuilder as well

    // TODO pull random generating into build-function?
    //      this would break idempotency unless a seed is passed as well (!)

    var contentRdf = won.buildNeedRdf({
        type : won.toCompacted(need.basicNeedType), //mandatory
        title: need.title, //mandatory
        description: need.textDescription, //mandatory
        publishedContentUri: publishedContentUri, //mandatory
        tags: need.tags.map(function(t) {return t.text}).join(','),
        attachmentUris: attachmentUris, //optional, should be same as in `attachments` below
    });
    var msgUri = wonNodeUri + '/event/' + getRandomPosInt(); //mandatory
    var msgJson = won.buildMessageRdf(contentRdf, {
        receiverNode : wonNodeUri, //mandatory
        senderNode : wonNodeUri, //mandatory
        msgType : won.WONMSG.createMessage, //mandatory
        publishedContentUri: publishedContentUri, //mandatory
        msgUri: msgUri,
        attachments: imgs //optional, should be same as in `attachmentUris` above
    });
    return [msgJson, msgUri];
}

    /*
let sampleResponse.data =
{
    "@graph":[
    {
        "@graph":[
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h",
                "@type":"http://purl.org/webofneeds/message#FromSystem",
                "http://purl.org/webofneeds/message#hasMessageType":{
                    "@id":"http://purl.org/webofneeds/message#SuccessResponse"
                },
                "http://purl.org/webofneeds/message#hasReceiverNeed":{
                    "@id":"https://192.168.124.53:8443/won/resource/need/996323569610784800"
                },
                "http://purl.org/webofneeds/message#hasReceiverNode":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#hasSenderNeed":{
                    "@id":"https://192.168.124.53:8443/won/resource/need/996323569610784800"
                },
                "http://purl.org/webofneeds/message#hasSenderNode":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#isResponseTo":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/6093195174983238000"
                },
                "http://purl.org/webofneeds/message#isResponseToMessageType":{
                    "@id":"http://purl.org/webofneeds/message#CreateMessage"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl",
                "@type":"http://purl.org/webofneeds/message#EnvelopeGraph",
                "http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl"
    },
    {
        "@graph":[
            {
                "@id":"_:b2",
                "@type":"signature:GraphSigningMethod",
                "signature:hasDigestMethod":{
                    "@id":"signature:dm-sha-256"
                },
                "signature:hasGraphCanonicalizationMethod":{
                    "@id":"signature:gcm-fisteus-2010"
                },
                "signature:hasGraphDigestMethod":{
                    "@id":"signature:gdm-fisteus-2010"
                },
                "signature:hasGraphSerializationMethod":{
                    "@id":"signature:gsm-trig"
                },
                "signature:hasSignatureMethod":{
                    "@id":"signature:sm-ecdsa"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl-sig",
                "@type":"signature:Signature",
                "signature:hasGraphSigningMethod":{
                    "@id":"_:b2"
                },
                "signature:hasSignatureValue":"MGUCMFvVzFK+j7Frxs3r+qmqmErYrwIeQBjx1N5FzpTaIBS/rSL/MC8WEuyPDO0VrbxigAIxAIX+k4FT6h/6CYNo7mGsOwXElgstJLUpjtdWY3B9aNGCneCdgXJtYlxuS9oAETCbKQ==",
                "signature:hasVerificationCertificate":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#hasSignedGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl-sig"
    },
    {
        "@graph":[
            {
                "@id":"_:b0",
                "http://purl.org/webofneeds/message#hasSignatureGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl-sig"
                },
                "http://purl.org/webofneeds/message#hasSignatureValue":"MGUCMFvVzFK+j7Frxs3r+qmqmErYrwIeQBjx1N5FzpTaIBS/rSL/MC8WEuyPDO0VrbxigAIxAIX+k4FT6h/6CYNo7mGsOwXElgstJLUpjtdWY3B9aNGCneCdgXJtYlxuS9oAETCbKQ==",
                "http://purl.org/webofneeds/message#hasSignedGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h",
                "@type":"http://purl.org/webofneeds/message#FromSystem",
                "http://purl.org/webofneeds/message#hasReceivedTimestamp":{
                    "@type":"http://www.w3.org/2001/XMLSchema#long",
                    "@value":"1449156395673"
                },
                "http://purl.org/webofneeds/message#referencesSignature":{
                    "@id":"_:b0"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-p1il",
                "@type":"http://purl.org/webofneeds/message#EnvelopeGraph",
                "http://purl.org/webofneeds/message#containsEnvelope":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-2jtl"
                },
                "http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-p1il"
    },
    {
        "@graph":[
            {
                "@id":"_:b1",
                "@type":"signature:GraphSigningMethod",
                "signature:hasDigestMethod":{
                    "@id":"signature:dm-sha-256"
                },
                "signature:hasGraphCanonicalizationMethod":{
                    "@id":"signature:gcm-fisteus-2010"
                },
                "signature:hasGraphDigestMethod":{
                    "@id":"signature:gdm-fisteus-2010"
                },
                "signature:hasGraphSerializationMethod":{
                    "@id":"signature:gsm-trig"
                },
                "signature:hasSignatureMethod":{
                    "@id":"signature:sm-ecdsa"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-p1il-sig",
                "@type":"signature:Signature",
                "signature:hasGraphSigningMethod":{
                    "@id":"_:b1"
                },
                "signature:hasSignatureValue":"MGUCMGmJsZR5S63LavXe6WCJq4DKeYuztmEYH89KSIU7aDYGFk5NXC6XP4X9ShE++WExoAIxAKNWyrEBR75cFohBj8hmiumYdtWNIEOBcrTh/KW1I1aCs/ijDiGm2c3bMD9h/meJKA==",
                "signature:hasVerificationCertificate":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#hasSignedGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-p1il"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/udpmqjxg8y16aelxnh7h#envelope-p1il-sig"
    }
],
    "@context":{
    "signature":"http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#"
}
}
        */