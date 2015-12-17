/**
 * Created by ksinger on 05.11.2015.
 */


/* TODO this fragment is part of an attempt to sketch a different
 * approach to asynchronity (Remove it or the thunk-based
 * solution afterwards)
 */

/*
* This redux wrapper for the old message-service consists of:
*
* * an "agent" that registers with the service, receives messages
* from it and triggers redux actions.
* * an "component" that listens to state changes and triggers
* messages to the server via the service.
 */

import { attach, delay, watchImmutableRdxState} from './utils';
//import './message-service'; //TODO still uses es5
import { actionCreators }  from './actions/actions';
import { getEventData } from './won-message-utils';
import SockJS from 'sockjs';

/*
class WONSocket extends SockJS {
    constructor() {
        super('/owner/msg', null, {debug: true});
    }

}
*/

export function runMessagingAgent(redux) {

    console.log('Starting messaging agent.');

    /* TODOs
     * + heartbeat? -> NOPE
     * + make it generic?
     *      + make the url a parameter?
     *      + extract the watch? / make the path a parameter?
     *      + registering a processor for the incoming messages (that
     *        can trigger actions but lets the messaging agent stay generic)
     *           + pass a callback
     *           + make this a signal/observable
     * + framing -> NOPE
     * + reconnecting
     * + lazy socket initialisation
     */

    let ws = newSock();
    window.ws4dbg = ws;//TODO deletme
    let unsubscribeWatch = null;

    function newSock() {
        const ws = new SockJS('/owner/msg', null, {debug: true});
        ws.onopen = onOpen;
        ws.onmessage = onMessage;
        ws.onerror = onError;
        ws.onclose = onClose;
        return ws;
    };
    function onOpen() {
        /* Set up message-queue watch */
        unsubscribeWatch = watchImmutableRdxState(
            redux, ['enqueuedMessages'],
            (newMq, oldMq) => {
                console.log('old mq length: ', oldMq.size);
                console.log('new mq length: ', newMq.size);
                if (newMq.size > 0) {
                    // a new msg was enqueued
                    const msg = newMq.first();
                    console.log('about to send ', msg);
                    ws.send(JSON.stringify(msg));
                    redux.dispatch(actionCreators.messages__markAsSent({msg})); //might be necessary to do this async (with `delay(...,0)`)
                }
            }
        );

    };
    function onMessage(msg) {
        /* TODO this is only for demo purposes. In practice, more
         * fragmented actions should be called here. Introducing
         * an in-queue would require another agent/more agents in
         * the system that works through the queue and dispatches
         * actions, resulting in the same unpredictability that
         * the pure angular approach had. For modularization handling
         * should be broken down into layered functions in
         * multiple files.
         */
        console.log('got message via websocket: ', msg);
        const parsedMsg = JSON.parse(msg.data)
        console.log('parsed msg 4 dbg: ', parsedMsg);
        window.parsedMsg4Dbg = parsedMsg;
        window.getEventData4Dbg = getEventData;
        //redux.dispatch(actionCreators.messages__receive({msg}));
    };
    function onError(e) {
        console.error('websocket error: ', e);
        this.close();
    };
    function onClose(e) {
        if(unsubscribeWatch && typeof unsubscribeWatch === 'function')
            unsubscribeWatch();

        if (e.code === 1011) {
            console.log('either your session timed out or you encountered an unexpected server condition.');
        } else {
            // posting anonymously creates a new session for each post
            // thus we need to reconnect here
            // TODO reconnect only on next message instead of straight away
            console.log('reconnecting websocket');
            ws = newSock();
        }
    };
}


/* EXAMPLE answer for creation:
*/

window.exampleCreateMsg4Dbg =
    [
        {
            "@graph":[
                {
                    "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000#need",
                    "@graph":[
                        {
                            "@id":"https://192.168.124.53:8443/won/resource/need/8340735611618984000",
                            "@type":"won:Need",
                            "won:hasContent":"_:n01",
                            "won:hasFacet":"won:OwnerFacet"
                        },
                        {
                            "@id":"_:n01",
                            "@type":"won:NeedContent",
                            "dc:title":"asdf"
                        }
                    ]
                },
                {
                    "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000#envelope",
                    "@graph":[
                        {
                            "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000",
                            "@type":"msg:FromOwner",
                            "msg:hasSentTimestamp":1449844142120,
                            "msg:hasMessageType":{
                                "@id":"http://purl.org/webofneeds/message#CreateMessage"
                            },
                            "msg:hasContent":[
                                {
                                    "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000#need"
                                }
                            ],
                            "msg:hasReceiverNode":{
                                "@id":"https://192.168.124.53:8443/won/resource"
                            },
                            "msg:hasSenderNode":{
                                "@id":"https://192.168.124.53:8443/won/resource"
                            },
                            "msg:hasSenderNeed":{
                                "@id":"https://192.168.124.53:8443/won/resource/need/8340735611618984000"
                            },
                            "msg:hasAttachment":[

                            ]
                        },
                        {
                            "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000#envelope",
                            "@type":"msg:EnvelopeGraph",
                            "rdfg:subGraphOf":{
                                "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000"
                            }
                        }
                    ]
                }
            ],
            "@context":{
                "webID":"http://www.example.com/webids/",
                "msg":"http://purl.org/webofneeds/message#",
                "dc":"http://purl.org/dc/elements/1.1/",
                "rdfs":"http://www.w3.org/2000/01/rdf-schema#",
                "geo":"http://www.w3.org/2003/01/geo/wgs84_pos#",
                "xsd":"http://www.w3.org/2001/XMLSchema#",
                "rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
                "won":"http://purl.org/webofneeds/model#",
                "gr":"http://purl.org/goodrelations/v1#",
                "ldp":"http://www.w3.org/ns/ldp#",
                "rdfg":"http://www.w3.org/2004/03/trix/rdfg-1/",
                "won:hasContent":{
                    "@id":"http://purl.org/webofneeds/model#hasContent",
                    "@type":"@id"
                },
                "msg:hasMessageType":{
                    "@id":"http://purl.org/webofneeds/message#hasMessageType",
                    "@type":"@id"
                },
                "won:hasContentDescription":{
                    "@id":"http://purl.org/webofneeds/model#hasContentDescription",
                    "@type":"@id"
                },
                "won:hasBasicNeedType":{
                    "@id":"http://purl.org/webofneeds/model#hasBasicNeedType",
                    "@type":"@id"
                },
                "won:hasCurrency":"xsd:string",
                "won:hasLowerPriceLimit":"xsd:float",
                "won:hasUpperPriceLimit":"xsd:float",
                "geo:latitude":"xsd:float",
                "geo:longitude":"xsd:float",
                "won:hasAddress":"xsd:string",
                "won:hasStartTime":"xsd:dateTime",
                "won:hasEndTime":"xsd:dateTime",
                "won:hasFacet":{
                    "@id":"http://purl.org/webofneeds/model#hasFacet",
                    "@type":"@id"
                },
                "cnt":"http://www.w3.org/2011/content#",
                "msg:EnvelopeGraph":{
                    "@id":"http://purl.org/webofneeds/message#EnvelopeGraph",
                    "@type":"@id"
                }
            }
        }
    ]

window.exampleAnswer4Dbg =
{
    "@graph":[
    {
        "@graph":[
            {
                "@id":"_:b0",
                "http://purl.org/webofneeds/message#hasSignatureGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw-sig"
                },
                "http://purl.org/webofneeds/message#hasSignatureValue":"MGYCMQDKqXyCol0CbvQw+hGDjlD2/KVZG8nEyn4teGM/F74fU3SQMsWSA5zkRpZHsYsvcz8CMQDkQaRCjd8KGWEmBdxI8JeFCsXJir1VKQxN/8b/Vibve2gxKhmvNl518QlzV48gpkw=",
                "http://purl.org/webofneeds/message#hasSignedGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u",
                "@type":"http://purl.org/webofneeds/message#FromSystem",
                "http://purl.org/webofneeds/message#hasReceivedTimestamp":{
                    "@type":"http://www.w3.org/2001/XMLSchema#long",
                    "@value":"1449844142326"
                },
                "http://purl.org/webofneeds/message#referencesSignature":{
                    "@id":"_:b0"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-8okn",
                "@type":"http://purl.org/webofneeds/message#EnvelopeGraph",
                "http://purl.org/webofneeds/message#containsEnvelope":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw"
                },
                "http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-8okn"
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
                "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-8okn-sig",
                "@type":"signature:Signature",
                "signature:hasGraphSigningMethod":{
                    "@id":"_:b1"
                },
                "signature:hasSignatureValue":"MGUCMQCL8VfKTXXfU94zHQivCYWXM/krzKS5cyI77DHEhK8/6kipF7gdJbLt8/wSAmvdyCsCMBTgWIf/PIy96EQeqLBRaAjcHpCXONDaRyd5WrD6114ffJvqdCDtw9vHI/bDaM698Q==",
                "signature:hasVerificationCertificate":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#hasSignedGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-8okn"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-8okn-sig"
    },
    {
        "@graph":[
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u",
                "@type":"http://purl.org/webofneeds/message#FromSystem",
                "http://purl.org/webofneeds/message#hasMessageType":{
                    "@id":"http://purl.org/webofneeds/message#SuccessResponse"
                },
                "http://purl.org/webofneeds/message#hasReceiverNeed":{
                    "@id":"https://192.168.124.53:8443/won/resource/need/8340735611618984000"
                },
                "http://purl.org/webofneeds/message#hasReceiverNode":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#hasSenderNeed":{
                    "@id":"https://192.168.124.53:8443/won/resource/need/8340735611618984000"
                },
                "http://purl.org/webofneeds/message#hasSenderNode":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#isResponseTo":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/5811012696899846000"
                },
                "http://purl.org/webofneeds/message#isResponseToMessageType":{
                    "@id":"http://purl.org/webofneeds/message#CreateMessage"
                }
            },
            {
                "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw",
                "@type":"http://purl.org/webofneeds/message#EnvelopeGraph",
                "http://www.w3.org/2004/03/trix/rdfg-1/subGraphOf":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw"
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
                "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw-sig",
                "@type":"signature:Signature",
                "signature:hasGraphSigningMethod":{
                    "@id":"_:b2"
                },
                "signature:hasSignatureValue":"MGYCMQDKqXyCol0CbvQw+hGDjlD2/KVZG8nEyn4teGM/F74fU3SQMsWSA5zkRpZHsYsvcz8CMQDkQaRCjd8KGWEmBdxI8JeFCsXJir1VKQxN/8b/Vibve2gxKhmvNl518QlzV48gpkw=",
                "signature:hasVerificationCertificate":{
                    "@id":"https://192.168.124.53:8443/won/resource"
                },
                "http://purl.org/webofneeds/message#hasSignedGraph":{
                    "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw"
                }
            }
        ],
        "@id":"https://192.168.124.53:8443/won/resource/event/3nx0tcra98xscm6il22u#envelope-cytw-sig"
    }
],
    "@context":{
        "signature":"http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#"
    }
}
