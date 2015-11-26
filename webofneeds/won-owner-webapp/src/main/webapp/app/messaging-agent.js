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

export function runMessagingAgent(redux) {

    const ws = openWebSocket()
    //.then( (ws) => {
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
        //ws.on('receive',...)
        ws.onReceived = (msg) => {

            /* TODO this is only for demo purposes. In practice, more
             * fragmented actions should be called here. Introducing
             * an in-queue would require another agent/more agents in
             * the system that works through the queue and dispatches
             * actions, resulting in the same unpredictability that
             * the pure angular approach had. For modularization handling
             * should be broken down into layered functions in
             * multiple files.
             */
            redux.dispatch(actionCreators.messages__receive({msg}));
        }
    //});
}

let dummyWs = null;
class DummyWs {
    send(msg) {
        console.log('"Sending to server": ', msg);
        delay(1500).then(() => {
            if(this.onReceived) {
                this.onReceived(msg);
            }
        });
    }
}
function openWebSocket() { return dummyWs? dummyWs : new DummyWs() }
