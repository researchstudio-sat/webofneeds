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

import { attach } from '../utils';
//import './message-service'; //TODO still uses es5
import { actionCreators }  from '../actions';

function delay2(milliseconds) {
    return new Promise((resolve, reject) =>
            window.setTimeout(() => resolve(), milliseconds)
    );
}

const serviceDependencies = ['$ngRedux', '$rootScope', /*injections as strings here*/];
class AgentService {
    static factory (/* arguments <- serviceDependencies */) {
      return new AgentService(...arguments);
    }
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

        window.mas4Dbg = this; //TODO deletme; for debugging

        const selectFromState = (state) => ({ state });


        /*
        const unsubscribe = this.$ngRedux.subscribe(() => {
            const state = selectFromState(this.$ngRedux.getState());

        });
        */
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$rootScope.$on('$destroy', disconnect);

        const ws = openWebSocket()
        /*.then( (ws) => {*/
            this.$rootScope.$watch(
                //not sure that this will be always called when the state changes. might require $applies sometimes
                (scope) => this.state.get('enqueuedMessages'),
                (newMq, oldMq) => {
                    console.log('old mq length: ', oldMq.size);
                    console.log('new mq length: ', newMq.size);
                    if (newMq.size > 0) {
                        // a new msg was enqueued
                        const msg = newMq.first();
                        console.log('about to send ', msg);
                        ws.send(msg);
                        this.messages__markAsSent({msg}); //might be necessary to do this async (with `delay(...,0)`)
                    }
                }
            )
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
                this.messages__receive({msg});
            }
        /*});*/


        //function to put watches on interesting parts of the tree
        //watch(state, ['path','to','property'], callbackFunction)
        //watch(() => watcheObject, callbackFunction)
    }
}
AgentService.factory.$inject = serviceDependencies;

let dummyWs = null;
class DummyWs {
    send(msg) {
        console.log('"Sending to server": ', msg);
        delay2(1500).then(() => {
            if(this.onReceived) {
                this.onReceived(msg);
            }
        });
    }
}
function openWebSocket() { return dummyWs? dummyWs : new DummyWs() }

export default angular.module('won.owner.wonservice', [
    ])
    //TODO needs more expressive name (something like 'connector'? 'messagingAgent'?
    .factory('messagingAgentService', AgentService.factory)
    .run(['messagingAgentService', (messagingAgentService) => {}]) //make sure the service is initialized
    .name
