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
        const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);

        const state = getState();
        let email = getIn(state, ['user', 'email']);
        let hasAccountPromise;
        const currentState = getIn(state, ['router', 'currentState', 'name']);
        const prevState = getIn(state, ['router', 'prevState', 'name']);
        const prevParams = getIn(state, ['router', 'prevParams']);

        if(state.getIn(['user', 'loggedIn'])){
            hasAccountPromise = Promise.resolve();
        } else {
            const privateId = generatePrivateId();
            hasAccountPromise = accountRegister({privateId})(dispatch, getState)
                    .then(() =>
                        // wait for the server to process the login and the reconnect to
                        // go through, before proceeding to need-creation.
                        delay(500)
                    )
                    .catch(err => {
                        console.error(`Creating temporary account (${privateId}) has failed due to `, err);
                        dispatch(actionCreators.registerFailed({privateId}));
                    });

            delete prevParams.privateId; // should there be a previous privateId, we don't want to change back to that later
        }

        return hasAccountPromise
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
            .then(() =>
                dispatch({
                    type: actionTypes.needs.create,
                    payload: {eventUri, message, needUri}
                })
            );
    }
}
