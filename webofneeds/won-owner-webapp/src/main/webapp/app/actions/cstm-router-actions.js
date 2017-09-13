/**
 * Created by ksinger on 07.08.2017.
 */

import Immutable from 'immutable';

import {
    getIn,
} from '../utils.js';

import {
    actionCreators,
    actionTypes
} from './actions.js';

import {
    makeParams,
    resetParams,
    resetParamsImm,
    constantParams,
    addConstParams,
} from '../configRouting.js';

/**
 * Action-Creator that goes back in the browser history
 * without leaving the app.
 * @param dispatch
 * @param getState
 */
export function stateBack() {
    return (dispatch, getState) => {
        const hasPreviousState = !!getIn(getState(), ['router', 'prevState', 'name']);
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
        const currentParams = getIn(getState(), ['router', 'currentParams']);
        return dispatch(actionCreators.router__stateGo(
            state,
            addConstParams(resetParamsImm.merge(queryParams), currentParams)
        ))
    }
}

/**
 * goes to new state and resets all parameters (except for "pervasive" ones like `privateId`)
 */
export function stateGoResetParams(state) {
    return (dispatch, getState) => {
        const currentParams = getIn(getState(), ['router', 'currentParams']);
        return dispatch(actionCreators.router__stateGo(
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
                return dispatch(actionCreators.router__stateGoResetParams('feed'));
            } else {
                return dispatch(actionCreators.router__stateGoResetParams('landingpage'));
            }
        }
    }
}

/**
 * goes to new state and keeps listed parameters at their current values
 */
export function stateGoKeepParams(state, queryParamsList) {
    return (dispatch, getState) => {
        const currentParams = getIn(getState(), ['router', 'currentParams']);
        const params = Immutable.Map( // [[k,v]] -> Map
            queryParamsList.map(
                    p => [p, currentParams[p]] // get value per param
            )
        );
        return dispatch(actionCreators.router__stateGo(
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
        const currentState = getIn(getState(), ['router', 'currentState', 'name']);
        const currentParams = getIn(getState(), ['router', 'currentParams']);
        return dispatch(actionCreators.router__stateGo(
            currentState,
            addConstParams(queryParams, currentParams)
        ));
    }
}
