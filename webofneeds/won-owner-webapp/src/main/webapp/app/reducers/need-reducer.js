/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions';
import { repeatVar } from '../utils';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import { combineReducersStable } from '../redux-utils';
import { buildCreateMessage } from '../won-message-utils';
import won from '../won-es6';

const initialState = Immutable.fromJS({
    isFetching: false,
    didInvalidate: false,
    /**
     * @deprecated as this collection uses normal js-objects instead of immutable-js objects
     * some views depend on it however :[
     * "use ownNeeds instead"
     */
    needs: {},
    ownNeeds: {},
    othersNeeds: {},
});

export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.needs.failed:
            console.log('reducers.js: failed receive needlist action');
            return Immutable.fromJS({error: error});

        //TODO @deprecated? unused?
        case actionTypes.needs.connectionsReceived:
            return state.setIn(['needs',action.payload.needUri,'connectionUris'],action.payload.connections);

        case actionTypes.needs.clean:
            return initialState;

        case actionTypes.needs.received:
            const ownNeed = action.payload;
            return setIfNew(state, ['ownNeeds', ownNeed.uri], ownNeed)
                .setIn(['needs', ownNeed.uri], Immutable.fromJS(ownNeed))//@deprecated; kept for backwards-compatibility with existing views

        case actionTypes.connections.add:
            const {ownNeed, remoteNeed, connection} = action.payload;
            //guarantee that own need is in the state
            const stateWithOwnNeed = setIfNew(state, ['ownNeeds', ownNeed.uri], ownNeed);
            const stateWithBothNeeds = setIfNew(stateWithOwnNeed, ['othersNeeds', remoteNeed.uri], remoteNeed);

            /* TODO | what if we get the connection while not online?
             * TODO | doing this here doesn't guarantee synchronicity with the rdf
             * TODO | unless we fetch all connections onLoad and onLogin
             */
            return stateWithBothNeeds.updateIn(['needs', ownNeed.uri, 'connections'], connections => connections?
                connections.push(connection.uri) :
                Immutable.List([connection.uri]) // first connection -> new List
            );

        default:
            return state;


    }
}

function setIfNew(state, path, obj){
    return state.updateIn(path, val => val?
        //we've seen this need before, no need to overwrite it
        val :
        //it's the first time we see this need -> add it
        Immutable.fromJS(obj))
}

