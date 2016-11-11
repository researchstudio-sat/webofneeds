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

import won from './won-es6';
import {
    attach,
    delay,
    watchImmutableRdxState,
    checkHttpStatus,
    is,
} from './utils';
import { actionTypes, actionCreators } from './actions/actions';
//import './message-service'; //TODO still uses es5
import { getEventsFromMessage,setCommStateFromResponseForLocalNeedMessage } from './won-message-utils';
import SockJS from 'sockjs';
import * as messages from './actions/messages-actions';

export function runMessagingAgent(redux) {

    console.log('Starting messaging agent.');


    /**
     * The messageProcessingArray encapsulates all currently implemented message handlers with their respective redux dispatch
     * events, to process another message you add another anonymous function that checks on the necessary message properties/values
     * and dispatches the needed redux action/event
     */
    const messageProcessingArray = [
        function (events) {
            const msgFromExternal = events['msg:FromExternal'];

            /* Other clients or matcher initiated stuff: */
            if(msgFromExternal && msgFromExternal.hasMessageType === won.WONMSG.hintMessageCompacted) {
                redux.dispatch(actionCreators.messages__hintMessageReceived(msgFromExternal));
                return true;
            } else {
                return false;
            }
        },
        function (events) {
            const msgFromExternal = events['msg:FromExternal'];
            const msgFromOwner = events['msg:FromOwner'];

            /* Other clients or matcher initiated stuff: */
            if(msgFromExternal && msgFromOwner && msgFromOwner.hasMessageType === won.WONMSG.connectMessageCompacted) {
                redux.dispatch(actionCreators.messages__connectMessageReceived(events));
                return true;
            } else {
                return false;
            }
        },
        function (events) {
            const msgFromOwner = events['msg:FromOwner'];

            /* Other clients or matcher initiated stuff: */
            if(msgFromOwner && msgFromOwner.hasMessageType === won.WONMSG.openMessageCompacted) {
                //someone accepted our contact request
                redux.dispatch(actionCreators.messages__openMessageReceived(events));
                return true;
            } else {
                return false;
            }
        },
        function (events) {
            const msgFromExternal = events['msg:FromExternal'];
            const msgFromOwner = events['msg:FromOwner'];

            /* Other clients or matcher initiated stuff: */
            if(msgFromExternal && msgFromOwner && msgFromOwner.hasMessageType === won.WONMSG.connectionMessageCompacted) {
                redux.dispatch(actionCreators.messages__connectionMessageReceived({ events }));
                return true;
            } else {
                return false;
            }
        },
        function (events) {
            const msgFromExternal = events['msg:FromExternal'];
            const msgFromOwner = events['msg:FromOwner'];

            if(msgFromExternal && msgFromOwner && msgFromOwner.hasMessageType === won.WONMSG.closeMessageCompacted) {
                redux.dispatch(actionCreators.messages__close__success(msgFromOwner));
                return true;
            } else {
                return false;
            }
        },
        function (events) {
            const msgFromSystem = events['msg:FromSystem'];

            if(msgFromSystem && msgFromSystem.isResponseToMessageType === won.WONMSG.createMessageCompacted){
                if (msgFromSystem.hasMessageType === won.WONMSG.successResponseCompacted)
                    redux.dispatch(actionCreators.messages__create__success(msgFromSystem));
                //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                //    redux.dispatch(actionCreators.messages__create__failure(event));
                return true;
            } else {
                return false;
            }
        },
        function (events) {
            const msgFromSystem = events['msg:FromSystem'];

            if(msgFromSystem && msgFromSystem.isResponseToMessageType === won.WONMSG.connectMessageCompacted){
                if (msgFromSystem.hasMessageType === won.WONMSG.successResponseCompacted)
                    redux.dispatch(actionCreators.messages__connect__success(msgFromSystem));
                //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                //  redux.dispatch(actionCreators.messages__open__failure(event));
                return true;
            }else{
                return false;
            }

        },
        function (events) {
            const msgFromSystem = events['msg:FromSystem'];

            if(msgFromSystem && msgFromSystem.isResponseToMessageType === won.WONMSG.connectionMessageCompacted){
                var eventUri = msgFromSystem.isRemoteResponseTo || msgFromSystem.isResponseTo;
                var connectionUri = msgFromSystem.hasReceiver;
                if (msgFromSystem.hasMessageType === won.WONMSG.successResponseCompacted) {
                    if (msgFromSystem.isRemoteResponseTo) {
                        // got the second success-response (from the remote-node) - 2nd ACK
                        redux.dispatch(actionCreators.messages__chatMessage__successRemote({ events }));
                    } else {
                        // got the first success-response (from our own node) - 1st ACK
                        redux.dispatch(actionCreators.messages__chatMessage__successOwn({ events }));
                    }
                } else if(msgFromSystem.hasMessageType === won.WONMSG.failureResponseCompacted) {
                    redux.dispatch(actionCreators.messages__chatMessage__failure({ events }));
                }
                return true;
            }else{
                return false;
            }

        },
        function (events) {
            const msgFromSystem = events['msg:FromSystem'];

            if(msgFromSystem && msgFromSystem.isResponseToMessageType === won.WONMSG.openMessageCompacted){
                if (msgFromSystem.hasMessageType === won.WONMSG.successResponseCompacted)
                    redux.dispatch(actionCreators.messages__open__success(msgFromSystem));
                //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                //  redux.dispatch(actionCreators.messages__open__failure(event));
                return true;
            }else{
                return false;
            }

        },
        function (events) {
            const msgFromSystem = events['msg:FromSystem'];

            if(msgFromSystem && msgFromSystem.isResponseToMessageType === won.WONMSG.closeMessageCompacted){
                if (msgFromSystem.hasMessageType === won.WONMSG.successResponseCompacted) //JUMP HERE AND ONLY HERE WHEN CLOSE MESSAGES COME IN!
                    redux.dispatch(actionCreators.messages__close__success(msgFromSystem));
                //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                //  redux.dispatch(actionCreators.messages__close__failure(event));
                return true;
            }else{
                return false;
            }

        },
        function (events) {
            const msgFromSystem = events['msg:FromSystem'];

            if(msgFromSystem && msgFromSystem.isResponseToMessageType === won.WONMSG.closeNeedMessageCompacted){
                if (msgFromSystem.hasMessageType === won.WONMSG.successResponseCompacted)
                    redux.dispatch(actionCreators.messages__closeNeed__success(msgFromSystem));
                else if (msgFromSystem.hasMessageType === won.WONMSG.failureResponseCompacted)
                    redux.dispatch(actionCreators.messages__closeNeed__failure(msgFromSystem));
                return true;
            }else{
                return false;
            }
        }
    ];

    function onMessage(receivedMsg) {
        const data = JSON.parse(receivedMsg.data);

        console.log('onMessage: ', data);
        getEventsFromMessage(data).then(events => {
            console.log('onMessage - events: ', events);

            var messageProcessed = false;

            for(var i = 0; i < messageProcessingArray.length; i++) {
                messageProcessed = messageProcessed || messageProcessingArray[i](events);
            }

            if(!messageProcessed){
                console.warn("MESSAGE WASN'T PROCESSED DUE TO MISSING HANDLER FOR ITS TYPE: ", events);
            }
        })
    };


    /* TODOs
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
    let unsubscribeWatches = [];

    let reconnectAttempts = 0; // should be increased when a socket is opened, reset to 0 after opening was successful

    let missedHeartbeats = 0; // deadman-switch variable. should count up every 30s and gets reset onHeartbeat
    setInterval(checkHeartbeat, 30000); // heartbeats should arrive roughly every 30s

    function newSock() {
        const ws = new SockJS('/owner/msg', null, {debug: true});
        ws.onopen = onOpen;
        ws.onmessage = onMessage;
        ws.onerror = onError;
        ws.onclose = onClose;
        ws.onheartbeat = onHeartbeat;
        missedHeartbeats = 0;

        reconnectAttempts++;

        return ws;
    };

    function onHeartbeat(e) {
        console.log('messaging-agent.js: received heartbeat',e);
        missedHeartbeats = 0; // reset the deadman count
    }

    function checkHeartbeat() {
        console.log("messaging-agent.js: checking heartbeat presence: ", missedHeartbeats, " full intervals of 30s have passed since the last heartbeat.");

        if(++missedHeartbeats > 3) {
            console.error("messaging-agent.js: no websocket-heartbeat present. closing socket.");
            this.close();
        }
    }

    function onOpen() {
        /* Set up message-queue watch */

        reconnectAttempts = 0; // successful opening of socket. we can reset the reconnect counter.

        if(unsubscribeWatches.length === 0) {
            const unsubscribeMsgQWatch = watchImmutableRdxState(
                redux, ['messages', 'enqueued'],
                (newMsgBuffer, oldMsgBuffer) => {
                    if (newMsgBuffer) {
                        const firstEntry = newMsgBuffer.entries().next().value;
                        if (firstEntry) { //undefined if queue is empty
                            const [eventUri, msg] = firstEntry;
                            ws.send(JSON.stringify(msg));
                            console.log("messaging-agent.js: sent message: " + JSON.stringify(msg));
                            redux.dispatch(actionCreators.messages__waitingForAnswer({eventUri, msg}));
                        }
                    }
                }
            );
            /**
             * TODO this watch is part of the session-upgrade hack documented in:
             * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
             */
            const unsubscribeResetWatch = watchImmutableRdxState(
                redux, ['messages', 'resetWsRequested_Hack'],
                (newRequestState, oldRequestState) => {
                    if (newRequestState) {
                        redux.dispatch(actionCreators.messages__requestWsReset_Hack(false));
                        console.log("messaging-agent.js: Resetting websocket (a hack necessary for upgrading it after login)");
                        ws.close();
                        // a new ws-connection should be opened automatically in onClose
                    }
                }
            );

            unsubscribeWatches.push(unsubscribeMsgQWatch);
            unsubscribeWatches.push(unsubscribeResetWatch);
        }

    };
    function onError(e) {
        console.error('messaging-agent.js: websocket error: ', e);
        this.close();
    };

    function onClose(e) {
        if(e.wasClean){
            console.log('messaging-agent.js: websocket closed cleanly. reconnectAttempts = ',reconnectAttempts);
        } else {
            console.error('messaging-agent.js: websocket crashed. reconnectAttempts = ',reconnectAttempts);
        }

        if (e.code === 1011 || reconnectAttempts > 1) {
            fetch('rest/users/isSignedIn', {credentials: 'include'}) // attempt to get a new session
                .then(checkHttpStatus) // will reject if not logged in
                .then(() => {
                    // session is still valid -- reopen the socket
                    ws = newSock();
                }).catch(error => {
                    // cleanup - unsubscribe all watches and empty the array
                    for(let unsubscribe; unsubscribe = unsubscribeWatches.pop(); !!unsubscribe) {
                        if(unsubscribe && is("Function", unsubscribe))
                            unsubscribe();
                    }

                    console.error('messaging-agent.js: either your session timed out or you encountered an unexpected server condition: \n', e.reason);
                    // TODO instead show a slide-in "Lost connection" with a reload button (that allows to copy typed text out)
                    redux.dispatch(actionCreators.logout())
                });
        } else {
            /*
             * first reconnect happens immediately (to facilitate
             * anonymous posting and the reset-hack necessary for
             * login atm)
             */
            ws = newSock();
        }
    };
}

