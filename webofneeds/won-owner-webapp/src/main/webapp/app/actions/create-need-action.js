/**
 * Created by ksinger on 04.08.2017.
 */

import Immutable from 'immutable';

import {
    buildCreateMessage,
} from '../won-message-utils.js';

import {
    actionCreators,
    actionTypes,
} from './actions.js';

import {
    accountRegister,
    ensureLoggedIn,
} from './account-actions.js';

import {
    registerAccount,
    generatePrivateId,
    login,
} from '../won-utils.js';

import {
    delay,
    getIn,
} from '../utils.js';

export function needCreate(draft, nodeUri) {
    return (dispatch, getState) => {

        const state = getState();

        if(!nodeUri) {
            nodeUri = getIn(state, ['config', 'defaultNodeUri']);
        }

        const currentState = getIn(state, ['router', 'currentState', 'name']);
        const prevState = getIn(state, ['router', 'prevState', 'name']);
        const prevParams = getIn(state, ['router', 'prevParams']);
        if(!state.getIn(['user', 'loggedIn'])){
            /*
             * `ensureLoggedIn` will generate a new privateId. should
             * there be a previous privateId, we don't want to change
             * back to that later.
             */
            delete prevParams.privateId;
        }

        return ensureLoggedIn(dispatch, getState)
            .then(() => {
                if (currentState === 'landingpage') {
                    return dispatch(actionCreators.router__stateGoAbs('feed'))
                } else if (currentState === 'createNeed') {
                    /*
                     * go to view that was open before the create-view was opened, but
                     * don't revert any new privateID or remove the create-gui from the
                     * history stack.
                     */
                    return dispatch(actionCreators.router__stateGoAbs(prevState, prevParams))
                }
            })
            .then(() => {
                const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);
                return dispatch({
                    type: actionTypes.needs.create,
                    payload: {eventUri, message, needUri}
                })

            });
    }
}
