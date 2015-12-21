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
                        redux.dispatch(actionCreators.messages__markAsSent({ eventUri, msg }));
                    }
                }
            }
        );

    };
    function onMessage(receivedMsg) {
        const parsedMsg = JSON.parse(receivedMsg.data);
        getEventData(parsedMsg).then(event => {
            // redux.dispatch(actionCreators.messages__receive(event))
            window.event4dbg = event;

            if(event.hasMessageType === won.WONMSG.successResponseCompacted) {
                redux.dispatch(actionCreators.messages__markAsSuccess({
                    eventUri: event.isResponseTo
                }));

                console.log('received response to ', event.isResponseTo, ' of ', event);

                //TODO do all of this in actions.js?
                if (event.isResponseToMessageType === won.WONMSG.createMessageCompacted) {
                    // linkeddataservice.crawl(event.hasSenderNeed) //agents shouldn't directyl communicate with each other, should they?

                    // get draftID (via event.isResponseTo?)
                    //??redux.getState().getIn(['messages', 'sent', event.isResponseToMessage, 'draftUri']);
                    // dispatch 'mark draft as successfully published'

                    // get needId
                    // > event.hasSenderNeed
                    // dispatch 'created need'
                    // dispatch routing change
                    // > redux.dispatch(actionCreators.router__stateGo('postVisitor', {postId: 1234 /* published posts id */}));

                }


                /*
                 if it's a 'successfully published that need message'
                 dispatch 'mark as successfully created'
                 else if it's a chat-msg
                 dispatch chat msg received
                 ....

                 */
            }
        });
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

