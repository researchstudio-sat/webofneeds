/**
 * Created by ksinger on 07.08.2017.
 */

import Immutable from 'immutable';

import {
    actionCreators,
    actionTypes
} from './actions';

import {
    makeParams,
    resetParams,
    constantParams,
    addConstParams,
} from '../configRouting';

/**
 * Action-Creator that goes back in the browser history
 * without leaving the app.
 * @param dispatch
 * @param getState
 */
export function stateBack() {
    return (dispatch, getState) => {
        const hasPreviousState = !!getState().getIn(['router', 'prevState', 'name']);
        if (hasPreviousState) {
            history.back();
        } else {
            dispatch(actionCreators.router__stateGoResetParams('landingpage'));
        }
    }
}

/**
 * reset's all parameters but the one passed as arguments
 */
export function stateGoAbs(state, queryParams) {
    return (dispatch, getState) => {
        const currentParams = getState().getIn(['router', 'currentParams']);
        dispatch(actionCreators.router__stateGo(
            state,
            addConstParams(queryParams, currentParams)
        ))
    }
}

/**
 * goes to new state and resets all parameters (except for "pervasive" ones like `privateId`)
 */
export function stateGoResetParams(state) {
    return (dispatch, getState) => {
        const currentParams = getState().getIn(['router', 'currentParams']);

        console.log('routing from',
            getState().getIn(['router', 'currentState', 'name']), currentParams,
            ' to ', state, addConstParams(resetParams, currentParams)
        );

        dispatch(actionCreators.router__stateGo(
            state,
            addConstParams(resetParams, currentParams)
        ))
    }
}

export function stateGoDefault() {
    return (dispatch, getState) => {
        const appState = getState();
        if ( appState.get('initialLoadFinished') ) {
            if (appState.getIn(['user', 'loggedIn'])) {
                dispatch(actionCreators.router__stateGoResetParams('feed'));
            } else {
                dispatch(actionCreators.router__stateGoResetParams('landingpage'));
            }
        }
    }
}

/**
 * goes to new state and keeps listed parameters at their current values
 */
export function stateGoKeepParams(state, queryParamsList) {
    return (dispatch, getState) => {
        const currentParams = getState().getIn(['router', 'currentParams']);
        const params = Immutable.Map( // [[k,v]] -> Map
            queryParamsList.map(
                    p => [p, currentParams.get(p)] // get value per param
            )
        );
        dispatch(actionCreators.router__stateGo(
            state,
            addConstParams(params, currentParams)
        ))
    }
}

/**
 * goes to current state, but changes the parameters
 * passed to this function.
 * @param queryParams
 */
export function stateGoCurrent(queryParams) {
    return (dispatch, getState) => {
        const currentState = getState().getIn(['router', 'currentState', 'name']);
        const currentParams = getState().getIn(['router', 'currentParams']);
        dispatch(actionCreators.router__stateGo(
            currentState,
            addConstParams(queryParams, currentParams)
        ));
    }
}
