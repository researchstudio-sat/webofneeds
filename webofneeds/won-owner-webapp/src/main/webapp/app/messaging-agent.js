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
import { getEventsFromMessage,setCommStateFromResponseForLocalNeedMessage } from './won-message-utils';
import SockJS from 'sockjs';
import * as messages from './actions/messages-actions';

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
            redux, ['messages', 'enqueued'],
            (newMsgBuffer, oldMsgBuffer) => {
                if(newMsgBuffer) {
                    const firstEntry = newMsgBuffer.entries().next().value;
                    if(firstEntry) { //undefined if queue is empty
                        const [eventUri, msg] = firstEntry;
                        ws.send(JSON.stringify(msg));
                        console.log("sent message: "+JSON.stringify(msg));
                        redux.dispatch(actionCreators.messages__waitingForAnswer({ eventUri, msg }));
                    }
                }
            }
        );
        /**
         * TODO this watch is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
        unsubscribeWatch = watchImmutableRdxState(
            redux, ['messages', 'resetWsRequested_Hack'],
            (newRequestState, oldRequestState) => {
                if(newRequestState) {
                    ws.close();
                    // a new ws-connection should be opened automatically in onClose
                    redux.dispatch(actionCreators.messages__requestWsReset_Hack(false));
                }
            }
        );

    };

    function onMessage(receivedMsg) {
        const data = JSON.parse(receivedMsg.data);

        console.log('onMessage: ', data);
        getEventsFromMessage(data).then(events => {
            console.log('onMessage - events: ', events);

            /* Other clients or matcher initiated stuff: */
            if (events['msg:FromExternal'] &&
                events['msg:FromExternal'].hasMessageType === won.WONMSG.hintMessageCompacted){
                    redux.dispatch(actionCreators.messages__hintMessageReceived(events['msg:FromExternal']));
            }
            if(events['msg:FromExternal'] &&
               events['msg:FromOwner'] &&
               events['msg:FromOwner'].hasMessageType === won.WONMSG.connectMessageCompacted ){
                    redux.dispatch(actionCreators.messages__connectMessageReceived(events));
            }

            if(events['msg:FromExternal'] &&
                events['msg:FromOwner'] &&
                events['msg:FromOwner'].hasMessageType === won.WONMSG.connectionMessageCompacted ){
                //got a chat message on a connection
                redux.dispatch(actionCreators.messages__connectionMessageReceived(events));
            }

            /* responses to own actions: */
            if(events['msg:FromSystem']) {
                switch (events['msg:FromSystem'].isResponseToMessageType) {
                    case won.WONMSG.createMessageCompacted:
                        if (events['msg:FromSystem'].hasMessageType === won.WONMSG.successResponseCompacted)
                            redux.dispatch(actionCreators.messages__create__success(events['msg:FromSystem']));
                        //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                        //    redux.dispatch(actionCreators.messages__create__failed(event));
                        break;

                    case won.WONMSG.connectMessageCompacted:
                        if (events['msg:FromSystem'].hasMessageType === won.WONMSG.successResponseCompacted)
                            redux.dispatch(actionCreators.messages__connect__success(events['msg:FromSystem']));
                        //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                        //  redux.dispatch(actionCreators.messages__open__failed(event));
                        break;

                    case won.WONMSG.openMessageCompacted:
                        if (events['msg:FromSystem'].hasMessageType === won.WONMSG.successResponseCompacted)
                            redux.dispatch(actionCreators.messages__open__success(events['msg:FromSystem']));
                        //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                        //  redux.dispatch(actionCreators.messages__open__failed(event));
                        break;

                    case won.WONMSG.closeMessageCompacted:
                        if (events['msg:FromSystem'].hasMessageType === won.WONMSG.successResponseCompacted)
                            redux.dispatch(actionCreators.messages__close__success(events['msg:FromSystem']));
                        //else if(event.hasMessageType === won.WONMSG.failureResponseCompacted)
                        //  redux.dispatch(actionCreators.messages__close__failed(event));
                        break;

                    case won.WONMSG.closeNeedMessageCompacted:
                        if (events['msg:FromSystem'].hasMessageType === won.WONMSG.successResponseCompacted)
                            redux.dispatch(actionCreators.messages__closeNeed__success(events['msg:FromSystem']));
                        else if (events['msg:FromSystem'].hasMessageType === won.WONMSG.failureResponseCompacted)
                            redux.dispatch(actionCreators.messages__closeNeed__failed(events['msg:FromSystem']));
                        break;

                    case won.WONMSG.connectionMessageCompacted:
                        //TODO handle succesful posting
                        break;
                }
            }
        })

    };
    function onError(e) {
        console.error('websocket error: ', e);
        this.close();
    };
    function onClose(e) {
        if(e.wasClean){
            console.log('websocket closed.');
        } else {
            console.error('websocket closed.')
        }
        if(unsubscribeWatch && typeof unsubscribeWatch === 'function')
            unsubscribeWatch();

        if (e.code === 1011) {
            console.log('either your session timed out or you encountered an unexpected server condition. \n', e.reason);
        } else {
            // posting anonymously creates a new session for each post
            // thus we need to reconnect here
            // TODO reconnect only on next message instead of straight away <-- bad idea, prevents push notifications
            // TODO add a delay if first reconnect fails
            ws = newSock();
        }
    };
}

