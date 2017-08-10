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
    parseCredentials,
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
//export function anonAccountLogin(privateId, options) {
//    const {email, password} = privateId2Credentials(privateId);
//    return (dispatch, getState) => {
//        const state = getState();
//        const options_ = Object.assign(
//            { // defaults
//                fetchData: true,
//                redirectToFeed: false,
//                relogIfNecessary: true, // if there's a valid session or privateId, log out from that first.
//                wasLoggedIn: false,
//            },
//            options
//        );
//
//        let loggedOutPromise;
//        if(
//            options_.relogIfNecessary &&
//            // v--- do any re-login-actions only after initialPageLoad. The latter should handle any necessary logins itself.
//            state.get('initialLoadFinished') &&
//            state.getIn(['router', 'currentParams', 'privateId']) !== privateId
//        ) {
//            // privateId has changed, need to relog
//            options_.wasLoggedIn = true;
//            loggedOutPromise = logoutAndResetPrivateId(dispatch, getState)
//        } else {
//            loggedOutPromise = Promise.resolve();
//        }
//        options_.relogIfNecessary = false; // any necessary logout has been handled
//
//        return loggedOutPromise.then(() =>
//            accountLogin(email, password, options_)(dispatch, getState)
//        );
//    }
//}

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
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountLogin(credentials, options) {
    const options_ = Object.assign(
        { // defaults
            fetchData: true,
            redirectToFeed: false,
            relogIfNecessary: true, // if there's a valid session or privateId, log out from that first.
        },
        options
    );
    return (dispatch, getState) => {
        const state = getState();

        const {email} = parseCredentials(credentials);

        const prevPrivateId = state.getIn(['router', 'currentParams', 'privateId']);
        const prevEmail = state.getIn(['user', 'email']);

        const wasLoggedIn = state.get('initialLoadFinished') && (prevPrivateId || prevEmail);

        if(state.get('loginInProcessFor') === email) {
            console.info('Already logging in as ', email, '. Canceling redundant attempt.');
            return;
        }

        if(state.get('initialLoadFinished') && (
             credentials.privateId && credentials.privateId === prevPrivateId ||
             credentials.email && credentials.email === prevEmail
           )
        ) {
                console.info(
                    'Already logged into this account (' +
                    (credentials.privateId || credentials.email) +
                    '). Aborting second login attempt.');
                return;
        }



        return Promise.resolve()
        .then(() =>
            dispatch({
                type: actionTypes.loginStarted,
                payload: { email }
            })
        )
        .then(() => {
            if (wasLoggedIn) {
                //console.log('wasLoggedIn ', wasLoggedIn);
                return logoutAndResetPrivateId(dispatch, getState);
            } else {
                //console.log('wasNotLoggedIn ', wasLoggedIn);
            }
        })
        .then(() =>
            login(credentials)
        )
        .then(response =>
            options_.fetchData ? fetchOwnedData(email) : Immutable.Map() // only need to fetch data for non-new accounts
        )
        .then(allThatData =>
            dispatch({
                type: actionTypes.login,
                payload: allThatData.merge({email: email, loggedIn: true})
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
                .then(() => {
                    if(wasLoggedIn) {
                        return dispatch({
                            type: actionTypes.logout,
                            payload: Immutable.fromJS({loggedIn: false})
                        })
                    }
                })
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
    .then(() => {
        const state = getState();
        if(state.getIn(['router', 'currentParams', 'privateId'])) {
            return stateGoCurrent({privateId: ""})(dispatch, getState);
        }
    })
}

export function accountLogout() {
    return (dispatch, getState) =>
        logoutAndResetPrivateId(dispatch, getState)
        .catch( error => {
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
                console.log('Error while trying to log out: ', error);
            }
        )
        .then(() =>
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
        .then(() =>  /* finally */
            // for the case that we've been logged in to an anonymous account, we need to remove the privateId here.
            //dispatch(actionCreators.router__stateGoCurrent({privateId: undefined}));

            checkAccessToCurrentRoute(dispatch, getState)
        )
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountRegister(credentials) {
    return (dispatch) =>
        registerAccount(credentials)
        .then(response =>
            accountLogin(credentials, {
                fetchData: false,
                redirectToFeed: true,
            })(dispatch)
        )
        .catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                error =>
                    dispatch(actionCreators.registerFailed({registerError: "Registration failed (E-Mail might already be used)", error}))
        )
}
