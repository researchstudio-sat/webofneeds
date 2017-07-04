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
    errors: []
});

export default function(allNeeds = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
        case actionTypes.needs.clean:
            return initialState;

        case actionTypes.needs.failed:
            console.log('reducers.js: failed receive needlist action');
            return allNeeds.update('errors', errors => errors.push(action.payload.error));

        case actionTypes.initialPageLoad:
        case actionTypes.login:
            let ownNeeds = action.payload.get('ownNeeds');
            ownNeeds = ownNeeds? ownNeeds : Immutable.Set();
            let theirNeeds = action.payload.get('theirNeeds');
            theirNeeds = theirNeeds? theirNeeds : Immutable.Set();
            const stateWithOwnNeeds = ownNeeds.reduce(
                (updatedState, ownNeed) => addOwnNeed(updatedState, ownNeed),
                allNeeds
            );
            const stateWithOwnAndTheirNeeds = theirNeeds.reduce(
                (updatedState, theirNeed) => addTheirNeed(updatedState, theirNeed),
                stateWithOwnNeeds

            );
            return stateWithOwnAndTheirNeeds;
            /*
            return allNeeds
                .mergeIn(['ownNeeds'], ownNeeds)
                .mergeIn(['theirNeeds'], theirNeeds);
                */

        case actionTypes.router.accessedNonLoadedPost:
            const theirNeed = action.payload.get('theirNeed');
            return addTheirNeed(allNeeds, theirNeed);

        case actionTypes.needs.fetch:
            //TODO needs supplied by this action don't have a list of already associated connections
            return action.payload.reduce(
                (updatedState, ownNeed) => addOwnNeed(updatedState, ownNeed),
                allNeeds
            );

        case actionTypes.needs.reopen:
            return allNeeds.setIn([
                "ownNeeds", action.payload.ownNeedUri, 'won:isInState'
            ], won.WON.ActiveCompacted);

        case actionTypes.needs.close:
            return allNeeds.setIn([
                "ownNeeds", action.payload.ownNeedUri, 'won:isInState'
            ], won.WON.InactiveCompacted);

        case actionTypes.needs.createSuccessful:
            return addOwnNeed(allNeeds, action.payload.need);

        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                allNeeds);

        case actionTypes.messages.connectMessageReceived:
            const {ownNeedUri, remoteNeed, updatedConnection } = action.payload;
            const stateWithBothNeeds = addTheirNeed(allNeeds, remoteNeed); // guarantee that remoteNeed is in state
            return addConnection(stateWithBothNeeds, ownNeedUri, updatedConnection);

        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(allNeeds, action.payload);

        default:
            return allNeeds;
    }
}

function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    const {ownNeed, remoteNeed, connection} = connectionWithRelatedData;
    const stateWithOwnNeed = addOwnNeed(state, ownNeed); // guarantee that ownNeed is in state
    const stateWithBothNeeds = addTheirNeed(stateWithOwnNeed, remoteNeed); // guarantee that remoteNeed is in state
    return addConnection(stateWithBothNeeds, ownNeed['@id'], connection.uri);
}

function addOwnNeed(allNeeds, ownNeed) {
    const ownNeed_ = Immutable.fromJS(ownNeed);
    if(ownNeed_ && ownNeed_.get('@id')) {
        return setIfNew(allNeeds, ['ownNeeds', ownNeed_.get('@id')], ownNeed_);
    } else {
        console.error('Tried to add invalid need-object: ', ownNeed_);
        return allNeeds;
    }
    //return setIfNew(allNeeds, ['ownNeeds', ownNeed_.get('@id')], ownNeed_);
}

function addTheirNeed(allNeeds, theirNeed) {
    const theirNeedImm = Immutable.fromJS(theirNeed);
    if(theirNeedImm && theirNeedImm.get('@id')) {
        return setIfNew(allNeeds, ['theirNeeds', theirNeedImm.get('@id')], theirNeedImm);
    } else {
        console.error('Tried to add invalid need-object: ', theirNeedImm);
        return allNeeds;
    }
}

/**
 * Add's the connectionUri to the needs connections. Makes
 * sure the same uri doesn't get added twice.
 * NOTE: As this function goes through all previous connections
 * to make sure that there are no duplicates, avoid using it
 * when adding a bunch of connections at once.
 * @param state
 * @param needUri
 * @param connectionUri
 * @return {*}
 */
function addConnection(state, needUri, connectionUri) {
    const pathToConnections = ['ownNeeds', needUri, 'won:hasConnections', 'rdfs:member'];
    if(!state.getIn(pathToConnections)) {
        //make sure the rdfs:member array exists
        state = state.setIn(pathToConnections, Immutable.List());
    }
    const connections = state.getIn(pathToConnections);
    if( connections.filter(c => c && c.get('@id') === connectionUri).size > 0) {
        // connection's already been added to the need before
        return state;
    } else {
        // new connection, add it to the need
        return state.updateIn(
            pathToConnections,
            connections => connections.push(
                Immutable.fromJS({ '@id': connectionUri })
            )
        );

    }
}

function setIfNew(state, path, obj){
    return state.updateIn(path, val => val?
        //we've seen this need before, no need to overwrite it
        val :
        //it's the first time we see this need -> add it
        Immutable.fromJS(obj))
}

