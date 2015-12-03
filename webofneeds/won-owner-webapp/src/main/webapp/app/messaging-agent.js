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
import SockJS from 'sockjs';

export function runMessagingAgent(redux) {

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
     */


    //const ws = new SockJS('owner/msg', null, {debug: true});
    const ws = openWebSocket();
    ws.onopen = () => {
        /* Set up message-queue watch */
        const unsubscribeWatch = watchImmutableRdxState(
            redux, ['enqueuedMessages'],
            (newMq, oldMq) => {
                console.log('old mq length: ', oldMq.size);
                console.log('new mq length: ', newMq.size);
                if (newMq.size > 0) {
                    // a new msg was enqueued
                    const msg = newMq.first();
                    console.log('about to send ', msg);
                    ws.send(msg);
                    redux.dispatch(actionCreators.messages__markAsSent({msg})); //might be necessary to do this async (with `delay(...,0)`)
                }
            }
        );

    };
    ws.onmessage = (msg) => {
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
        redux.dispatch(actionCreators.messages__receive({msg}));
    };
    ws.onerror = () => {
    };
    ws.onclose = () => {
    };

    window.ws4dbg = ws;//TODO deletme
}

let dummyWs = null;
class DummyWs {
    constructor(){
        delay(2000).then(() => {
            if(this.onopen) {
                this.onopen()
            }
        });
    }
    send(msg) {
        console.log('"Sending to server": ', msg);
        delay(1500).then(() => {
            if(this.onmessage) {
                this.onmessage(msg);
            }
        });
    }
}
function openWebSocket() { return dummyWs? dummyWs : new DummyWs() }
