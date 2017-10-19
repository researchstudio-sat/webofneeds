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

import won from './won-es6.js';
import {
    attach,
    delay,
    watchImmutableRdxState,
    checkHttpStatus,
    is,
} from './utils.js';

import {
    makeParams,
} from './configRouting.js';

import { actionTypes, actionCreators } from './actions/actions.js';
//import './message-service.js'; //TODO still uses es5
import SockJS from 'sockjs-client';
import * as messages from './actions/messages-actions.js';

export function runMessagingAgent(redux) {

    console.log('Starting messaging agent.');


    /**
     * The messageProcessingArray encapsulates all currently implemented message handlers with their respective redux dispatch
     * events, to process another message you add another anonymous function that checks on the necessary message properties/values
     * and dispatches the needed redux action/event
     */
    const messageProcessingArray = [
        function (message) {
            /* Other clients or matcher initiated stuff: */
            if(message.isFromExternal() && message.isHintMessage()) {
                redux.dispatch(actionCreators.messages__hintMessageReceived(message));
                return true;
            }
            return false;
        },
        function (message) {
            if(message.isFromExternal() && message.isConnectMessage()) {
                redux.dispatch(actionCreators.messages__connectMessageReceived(message));
                return true;
            }
            return false;
        },
        function (message) {
            if(message.isFromExternal() && message.isOpenMessage()) {
                //someone accepted our contact request
                redux.dispatch(actionCreators.messages__openMessageReceived(message));
                return true;
            }
            return false;
        },
        function (message) {
            if(message.isFromExternal() && message.isConnectionMessage()) {
                redux.dispatch(actionCreators.messages__connectionMessageReceived(message));
                return true;
            }
            return false;
        },
        function (message) {
            if(message.isFromExternal() && message.isCloseMessage()) {
                redux.dispatch(actionCreators.messages__close__success(message));
                return true;
            }
            return false;
        },
        function (message) {
            if(message.isFromSystem() && message.isSuccessResponse()
                && message.isResponseToCreateMessage()) {
                    redux.dispatch(actionCreators.messages__create__success(message));
                return true;
            }
            return false;
        },
        function (message) {
            if (message.isResponseToConnectMessage()) {
                if (message.isSuccessResponse()) {
                    if (message.isFromExternal()) {
                        // got the second success-response (from the remote-node) - 2nd ACK
                        redux.dispatch(actionCreators.messages__connect__successRemote(message));
                        return true;
                    } else {
                        // got the first success-response (from our own node) - 1st ACK
                        redux.dispatch(actionCreators.messages__connect__successOwn(message));
                        return true;
                    }
                } else if (message.isFailureResponse()) {
                    redux.dispatch(actionCreators.messages__connect__failure(message));
                    return true;
                }
            }
            return false;
        },
        function (message) {
            if (message.isResponseToConnectionMessage()) {
                if (message.isSuccessResponse()) {
                    if (message.isFromExternal()) {
                        // got the second success-response (from the remote-node) - 2nd ACK
                        redux.dispatch(actionCreators.messages__chatMessage__successRemote(message));
                        return true;
                    } else {
                        // got the first success-response (from our own node) - 1st ACK
                        redux.dispatch(actionCreators.messages__chatMessage__successOwn(message));
                        return true;
                    }
                } else if (message.isFailureResponse()) {
                    redux.dispatch(actionCreators.messages__chatMessage__failure(message));
                    return true;
                }
            }
            return false;
        },
        function (message) {
            if (message.isResponseToOpenMessage()) {
                if (message.isSuccessResponse()) {
                    if (message.isFromExternal()) {
                        // got the second success-response (from the remote-node) - 2nd ACK
                        redux.dispatch(actionCreators.messages__open__successRemote(message));
                        return true;
                    } else {
                        // got the first success-response (from our own node) - 1st ACK
                        redux.dispatch(actionCreators.messages__open__successOwn(message));
                        return true;
                    }
                } else if (message.isFailureResponse()) {
                    redux.dispatch(actionCreators.messages__open__failure(message));

                    /*
                     * as the failure should hit right after the open went out
                     * and should be rather rare, we can redirect in the optimistic
                     * case (see connection-actions.js) and go back if it fails.
                     */
                    redux.dispatch(actionCreators.router__stateGoAbs("post", {
                        postUri: message.getSenderNeed(),
                        connectionType: won.WON.RequestReceived,
                        connectionUri: message.getSender(),
                    }));
                    return true;
                }
            }
            return false;
        },
        function (message) {
            if (message.isResponseToCloseMessage()) {
                if (message.isSuccessResponse()) {
                    //JUMP HERE AND ONLY HERE WHEN CLOSE MESSAGES COME IN!
                    redux.dispatch(actionCreators.messages__close__success(message));
                    //  redux.dispatch(actionCreators.messages__close__failure(event));
                    return true;
                }
            }
            return false;
        },
        function (message) {
            if (message.isResponseToDeactivateMessage()) {
                if (message.isSuccessResponse()){
                    redux.dispatch(actionCreators.messages__closeNeed__success(message));
                    return true;                    
                } else {
                    redux.dispatch(actionCreators.messages__closeNeed__failure(message));
                    return true;                    
                }
            }
            return false;
        },
        function (message) {
            if(message.isFromSystem() && message.isNeedMessage()) {
                redux.dispatch(actionCreators.messages__needMessageReceived(message));
                return true;
            }
            return false;
        },
        function (message) {
            if(message.isFromSystem() && message.isCloseMessage()) {
                redux.dispatch({
                    type: actionTypes.messages.close.success,
                    payload: message
                });
                return true;
            }
            return false;
        },
        function (message) {
            if (message.isFromSystem() && message.isDeactivateMessage()) {
                //dispatch an action that is suitable for displaying a toast
                redux.dispatch(actionCreators.needs__closedBySystem(message));
                //adapt the state and GUI
                redux.dispatch({
                    type: actionTypes.needs.close,
                    payload:{
                        ownNeedUri: message.getReceiverNeed()
                    },
                });
                return true;
            }
            return false;
        }
    ];

    function onMessage(receivedMsg) {
        const data = JSON.parse(receivedMsg.data);

        console.log('onMessage (jsonld)    : ', data);
        won.wonMessageFromJsonLd(data).then(message => {
            console.log('onMessage (wonMessage): ', message);
            won.addJsonLdData(message.getMessageUri(), data);

            var messageProcessed = false;
    
            for(var i = 0; i < messageProcessingArray.length; i++) {
                messageProcessed = messageProcessed || messageProcessingArray[i](message);
            }
    
            if(!messageProcessed){
                console.warn("MESSAGE WASN'T PROCESSED DUE TO MISSING HANDLER FOR ITS TYPE: ", message);
            }
        });
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
    let reconnecting = false; // true after the user has requested a reconnect

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
        console.debug('messaging-agent.js: received heartbeat',e);
        missedHeartbeats = 0; // reset the deadman count
    }

    function checkHeartbeat() {
        console.debug("messaging-agent.js: checking heartbeat presence: ", missedHeartbeats, " full intervals of 30s have passed since the last heartbeat.");

        if(++missedHeartbeats > 3) {
            console.error("messaging-agent.js: no websocket-heartbeat present. closing socket.");
            ws.close();
        }
    }

    function onOpen() {
        /* Set up message-queue watch */

        reconnectAttempts = 0; // successful opening of socket. we can reset the reconnect counter.

        if(reconnecting) { // successful reconnect (failure is handled via connectionLost)
            reconnecting = false;
            redux.dispatch(actionCreators.reconnectSuccess());
        }

        if(unsubscribeWatches.length === 0) {
            const unsubscribeMsgQWatch = watchImmutableRdxState(
                redux, ['messages', 'enqueued'],
                (newMsgBuffer, oldMsgBuffer) => {
                    if (newMsgBuffer) {
                        const firstEntry = newMsgBuffer.entries().next().value;
                        if (firstEntry && ws.readyState === SockJS.OPEN) { //undefined if queue is empty
                            const [eventUri, msg] = firstEntry;
                            ws.send(JSON.stringify(msg));
                            //console.log("messaging-agent.js: sent message: " + JSON.stringify(msg));

                            // move message to next stat ("waitingForAnswer"). Also triggers this watch again as a result.
                            redux.dispatch(actionCreators.messages__waitingForAnswer({eventUri, msg}));
                        }
                    }
                }
            );
            const unsubscribeReconnectWatch = watchImmutableRdxState(
                redux, ['messages', 'reconnecting'],
                (newState, oldState) => {
                    reconnecting = newState;
                    if (!oldState && newState) {
                        console.log("messaging-agent.js: Resetting web-socket connection");
                        reconnectAttempts = 0;
                        if(ws.readyState !== WebSocket.CLOSED) {
                            ws.close(); // a new ws-connection should be opened automatically in onClose
                        } else {
                            ws = newSock(); // onClose won't trigger for already closed websockets, so create a new one here.
                        }
                    }
                }
            );

            unsubscribeWatches.push(unsubscribeMsgQWatch);
            unsubscribeWatches.push(unsubscribeReconnectWatch);
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

            console.error('messaging-agent.js: either your session timed out or you encountered an unexpected server condition: \n', e.reason);
            // TODO instead show a slide-in "Lost connection" with a reload button (that allows to copy typed text out)
            // TODO recovery from timed out session
            //redux.dispatch(actionCreators.logout())
            redux.dispatch(actionCreators.lostConnection());
            /*
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
                    //redux.dispatch(actionCreators.logout())
                    redux.dispatch(actionCreators.lostConnection())
                });
                */
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

