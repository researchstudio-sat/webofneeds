/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions';
import { fetchOwnedData } from '../won-message-utils';
import {
    registerAccount,
    login,
    privateId2Credentials,
    logout,
} from '../won-utils';
import {
    stateGoCurrent,

} from './cstm-router-actions';
import {
    resetParams,
    checkAccessToCurrentRoute,
} from '../configRouting';

import {
    checkHttpStatus,
} from '../utils';

/**
 * @param privateId
 * @param options see `accountLogin`
 * @returns {*}
 */
export function anonAccountLogin(privateId, options) {
    const {email, password} = privateId2Credentials(privateId);
    return accountLogin(email, password, options);
}
/**
 *
 * @param username
 * @param password
 * @param options
 *    * fetchData(true): whether or not to fetch a users owned needs. If the account
 *    signing in is new, there's no need to fetch this and `false` can be passed here
 *    * redirectToFeed(false): whether or not to redirect to the feed after signing in.
 *    * relogIfNecessary(true):  if there's a valid session or privateId, log out from that first.
 *
 *
 * @returns {Function}
 */
export function accountLogin(username, password, options) {
    const options_ = Object.assign(
        { // defaults
            fetchData: true,
            redirectToFeed: false,
            relogIfNecessary: true, // if there's a valid session or privateId, log out from that first.
        },
        options
    );

     //= { fetchData = true
    return (dispatch, getState) => {
        const state = getState();
        return Promise.resolve()
        .then(() => {
            if (
                options_.relogIfNecessary &&
                // v--- do any re-login-actions only when privateId is added after initialPageLoad. The latter should handle any necessary logins itself.
                state.get('initialLoadFinished') &&
                state.getIn(['user', 'email']) &&
                state.getIn(['user', 'email']) !== username
            ) {
                return logoutAndResetPrivateId(dispatch, getState)
            }
        })
        .then(() =>
            login(username, password)
        )
        .then(response =>
            options_.fetchData ? fetchOwnedData(username) : Immutable.Map() // only need to fetch data for non-new accounts
        )
        .then(allThatData =>
            dispatch({
                type: actionTypes.login,
                payload: allThatData.merge({email: username, loggedIn: true})
            })
        )
        .then(() =>
        /**
         * TODO this action is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
            dispatch(actionCreators.reconnect())
        )
        .then(() => options_.redirectToFeed ?
            dispatch(actionCreators.router__stateGoResetParams("feed")) :
            checkAccessToCurrentRoute(dispatch, getState)
        )
        .catch(error => {
            console.log("accountLogin ErrorObject", error);
            return Promise.resolve()
                .then(() => dispatch(actionCreators.loginFailed({
                    loginError: error.msg ?
                        error.msg :
                        "Unknown Username/Password Combination",
                    error
                })))
                .then(() =>
                    checkAccessToCurrentRoute(dispatch, getState)
                )
        })
    }
}

function logoutAndResetPrivateId(dispatch, getState) {
    return logout()
    .then(() =>
        stateGoCurrent({privateId: ""})(dispatch, getState)
    )

}

export function accountLogout() {
    return (dispatch, getState) =>
        logoutAndResetPrivateId(dispatch, getState)
        .catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
                error => {
                console.log(error);
            }
        )
        .then(response =>
            dispatch({
                type: actionTypes.logout,
                payload: Immutable.fromJS({loggedIn: false})
            })
        )
        .then(() => {
            won.clearStore();
            /**
             * TODO this action is part of the session-upgrade hack documented in:
             * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
             */
            dispatch(actionCreators.reconnect());
        })
        .then(() => { /* finally */
            // for the case that we've been logged in to an anonymous account, we need to remove the privateId here.
            //dispatch(actionCreators.router__stateGoCurrent({privateId: undefined}));

            checkAccessToCurrentRoute(dispatch, getState);
        })
}

export function accountRegister(username, password) {
    return (dispatch) =>
        registerAccount(username, password)
        .then(response => {
            /* TODO shouldn't we already have a valid
            * session at this point and thus just need
            * to execute the data-fetching part of login
            * (the fetchDataForOwnedNeeds, redirect
            * and wsReset)
            */
            accountLogin(username, password, {
                fetchData: false,
                redirectToFeed: true,
            })(dispatch);
        })
        .catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                error =>
                    dispatch(actionCreators.registerFailed({registerError: "Registration failed (E-Mail might already be used)", error}))
        )
}
