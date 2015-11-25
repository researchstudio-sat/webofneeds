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


const serviceDependencies = ['$ngRedux', /*injections as strings here*/];
class AgentService {
    static factory (/* arguments <- serviceDependencies */) {
      return new AgentService(...arguments);
    }
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

        window.as = this; //TODO deletme; for debugging

        const selectFromState = (state) => ({
            //draftId: state.getIn(['router','currentParams','draftId']),

            //filter for drafts that need to be published and only send diff?
            // or use the referential message-que? list: [[msgtransformer, argselectors...]]

        });


        //https://github.com/tshelburne/redux-batched-actions snippet for 'pending' state

        //const unsubscribe = this.$ngRedux.connect(selectFromState, actionCreators)(this);

        const unsubscribe = this.$ngRedux.subscribe(() => {
            const state = selectFromState(this.$ngRedux.getState());

        });

        //function to put watches on interesting parts of the tree
        //watch(state, ['path','to','property'], callbackFunction)
        //watch(() => watcheObject, callbackFunction)
    }


}
AgentService.factory.$inject = serviceDependencies;



export default angular.module('won.owner.wonservice', [
    ])
    //TODO needs more expressive name (something like 'connector'? 'messagingAgent'?
    .factory('messagingAgentService', AgentService.factory)
    .run(['messagingAgentService', (messagingAgentService) => {}]) //make sure the service is initialized
    .name
