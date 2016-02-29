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
    ownNeeds: {},
    theirNeeds: {},
});

export default function(allNeeds = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.needs.clean:
            return initialState;

        case actionTypes.needs.failed:
            console.log('reducers.js: failed receive needlist action');
            return Immutable.fromJS({error: action.payload.error});

        case actionTypes.load:
            const ownNeeds = action.payload.get('ownNeeds');
            const theirNeeds = action.payload.get('theirNeeds');
            return allNeeds
                .mergeIn(['ownNeeds'], ownNeeds)
                .mergeIn(['theirNeeds'], theirNeeds);

        case actionTypes.needs.fetch:
            //TODO needs supplied by this action don't have a list of already associated connections
            return action.payload.reduce(
                (updatedState, ownNeed) => setIfNew(updatedState, ['ownNeeds', ownNeed.uri], ownNeed),
                allNeeds
            );

        case actionTypes.needs.deactivate:
            const deactivateEvent = action.payload;
            const deactivateNeedUri = deactivateEvent.hasReceiver;
            return state.setIn(["ownNeeds", deactivateNeedUri, 'state'], won.WON.Inactive);

        case actionTypes.needs.received:
            const ownNeed = action.payload;
            return setIfNew(allNeeds, ['ownNeeds', ownNeed.uri], ownNeed)

        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                allNeeds);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(allNeeds, action.payload);

        default:
            return allNeeds;
    }
}

function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    const {ownNeed, remoteNeed, connection} = connectionWithRelatedData;
    //guarantee that own need is in the state
    const stateWithOwnNeed = setIfNew(
        state,
        ['ownNeeds', ownNeed.uri],
        Immutable.fromJS(ownNeed));

    const stateWithBothNeeds = setIfNew(
        stateWithOwnNeed,
        ['theirNeeds', remoteNeed.uri],
        Immutable.fromJS(remoteNeed));

    /* TODO | what if we get the connection while not online?
     * TODO | doing this here doesn't guarantee synchronicity with the rdf
     * TODO | unless we fetch all connections onLoad and onLogin
     */
    return stateWithBothNeeds.updateIn(['ownNeeds', ownNeed.uri, 'hasConnections'], connections => connections?
            connections.push(connection.uri) :
            Immutable.List([connection.uri]) // first connection -> new List
    );
}

function setIfNew(state, path, obj){
    return state.updateIn(path, val => val?
        //we've seen this need before, no need to overwrite it
        val :
        //it's the first time we see this need -> add it
        Immutable.fromJS(obj))
}

