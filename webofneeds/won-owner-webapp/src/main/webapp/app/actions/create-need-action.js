/**
 * Created by ksinger on 04.08.2017.
 */

import Immutable from 'immutable';

import {
    buildCreateMessage,
} from '../won-message-utils';

import {
    actionCreators,
    actionTypes,
} from './actions';

import {
    registerAccount,
    generateAccountCredentials,
    login,
} from '../won-utils';

import {
    delay,
} from '../utils';

export function needCreate(draft, nodeUri) {
    return (dispatch, getState) => {
        const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);

        const state = getState();
        let email = state.getIn(['user', 'email']);
        let hasAccountPromise;

        if(state.getIn(['user', 'loggedIn'])){
            hasAccountPromise = Promise.resolve();
        } else {
            const {email, password, privateId} = generateAccountCredentials()
            hasAccountPromise =
                registerAccount(email, password)
                    .then(() =>
                        login(email, password))
                    .then(() => {
                        //TODO custom action-creator and -type for this?
                        // TODO unnecessary as adding the privateId will already trigger a login action
                        dispatch({
                            type: actionTypes.login,
                            payload: Immutable.fromJS({
                                email,
                                loggedIn: true,
                                events: {},
                                ownNeeds: {},
                                theirNeeds: {},
                            })
                        })

                        // reset websocket to make sure it's using the logged-in session
                        return dispatch(actionCreators.reconnect());
                    })
                    .then(() =>
                        dispatch(actionCreators.router__stateGoCurrent({ privateId })) // add anonymous id to query-params
                    )
                    .then(() =>
                        // wait for the server to process the login and the reconnect to
                        // go through, before proceeding to need-creation.
                        delay(500)
                    )
                    .catch(err => {
                        //TODO user-visible error message / error recovery mechanisms
                        console.error(`Creating temporary account ${email} has failed due to `, err);
                    })
        }

        return hasAccountPromise
            .then(() => {
                dispatch({
                    type: actionTypes.needs.create,
                    payload: {eventUri, message, needUri}
                });
            })
            .then(() =>
                dispatch(actionCreators.router__stateGoResetParams('feed'))
            );
    }
}
