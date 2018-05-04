/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6.js';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions.js';
import { fetchOwnedData } from '../won-message-utils.js';
import {
    registerAccount,
    login,
    privateId2Credentials,
    logout,
    parseCredentials,
    generatePrivateId,
} from '../won-utils.js';
import {
    stateGoCurrent,

} from './cstm-router-actions.js';
import {
    resetParams,
    checkAccessToCurrentRoute,
} from '../configRouting.js';

import {
    checkHttpStatus,
    getIn,
    delay,
} from '../utils.js';

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
 * Makes sure user is either logged in
 * or creates a private-ID account as fallback.
 */
export async function ensureLoggedIn(dispatch, getState) {
    const state = getState();
    if(state.getIn(['user', 'loggedIn'])){
        return;
    }

    const privateId = generatePrivateId();
    try {
        await accountRegister({privateId})(dispatch, getState)
    } catch(err) {
        console.error(`Creating temporary account (${privateId}) has failed due to `, err);
        dispatch(actionCreators.registerFailed({privateId}));
    }
}

let _loginInProcessFor;
/**
 *
 * @param username
 * @param password
 * @param options
 *    * fetchData(true): whether or not to fetch a users owned needs. If the account
 *    signing in is new, there's no need to fetch this and `false` can be passed here
 *    * doRedirects(true): whether or not to do any redirects at all (e.g. if an invalid route was accessed)
 *    * redirectToFeed(false): whether or not to redirect to the feed after signing in (needs `redirects` to be true)
 *    * relogIfNecessary(true):  if there's a valid session or privateId, log out from that first.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountLogin(credentials, options) {
    const options_ = Object.assign(
        { // defaults
            fetchData: true,
            doRedirects: true,
            redirectToFeed: false,
            relogIfNecessary: true, // if there's a valid session or privateId, log out from that first.
        },
        options
    );
    return (dispatch, getState) => {
        const state = getState();

        const {email} = parseCredentials(credentials);

        const prevPrivateId = getIn(state, ['router', 'currentParams', 'privateId']);
        const prevEmail = state.getIn(['user', 'email']);

        const wasLoggedIn = state.get('initialLoadFinished') && (prevPrivateId || prevEmail);

        if(state.get('loginInProcessFor') === email || _loginInProcessFor === email) {
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


        const curriedDispatch = data => dispatch({
            type: actionTypes.login,
            payload: Immutable.fromJS(data).merge({email: email, loggedIn: true})
        });

        return Promise.resolve()
        .then(() => {
            _loginInProcessFor = email;
            return dispatch({
                type: actionTypes.loginStarted,
                payload: {email}
            })
        })
        .then(() => {
            if (wasLoggedIn) {
                return logout()
                .then(() => {
                    if (options_.doRedirects && getIn(state, ['router', 'currentParams', 'privateId'])) {
                        return stateGoCurrent({privateId: ""})(dispatch, getState);
                    }
                });
            }
        })
        .then(() => {
            if(options_.doRedirects && credentials.privateId) {
                return stateGoCurrent({privateId: credentials.privateId})(dispatch, getState);
            }
        })
        .then(() =>
            login(credentials)
        )
        .then(() =>
            curriedDispatch({})
        )
        .then(() => {
            if(!options_.doRedirects) {
                return;
            } else if (options_.redirectToFeed) {
                return dispatch(actionCreators.router__stateGoResetParams('connections'))
            } else {
                return checkAccessToCurrentRoute(dispatch, getState);
            }
        })
        .then(response => {
                if(options_.fetchData) {
                    return fetchOwnedData(email, curriedDispatch);
                } else {
                    return Immutable.Map(); // only need to fetch data for non-new accounts
                }
            }
        )
        .then(() =>
            curriedDispatch({loginFinished: true})
        )
        .then(() =>
        /**
         * TODO this action is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
            dispatch(actionCreators.reconnect())
        )
        .catch(error => {
            console.error("accountLogin ErrorObject", error);
            return Promise.resolve()
                .then(() => {
                    if(wasLoggedIn) {
                        return dispatch({
                            type: actionTypes.logout,
                            payload: Immutable.fromJS({loggedIn: false})
                        })
                    }
                })
                .then(() => {
                    const loginError = credentials.privateId ?
                        'invalid privateId' :
                        'unknown username/password combination';

                    dispatch(actionCreators.loginFailed({
                        loginError,
                        error,
                        credentials
                    }));
                })
                .then(() =>
                    options_.doRedirects && checkAccessToCurrentRoute(dispatch, getState)
                )
        })
        .then(() => {
            _loginInProcessFor = undefined;
        })
        .then(() => {
            if(credentials.privateId) {
                localStorage.setItem('privateId', credentials.privateId);
            }
        })
    }
}

let _logoutInProcess;
/**
 *
 * @param options
 *    * doRedirects(true): whether or not to do any redirects at all (e.g. if an invalid route was accessed)
 *
 * @returns {Function}
 */
export function accountLogout(options) {
    const options_ = {
        doRedirects: true,
        ...options,
    };

    localStorage.removeItem('privateId');

    return (dispatch, getState) => {
        const state = getState();

        if(state.get('logoutInProcess') || _logoutInProcess) {
            console.info('There\'s already a logout in process. Aborting redundant attempt.');
            return;
        }
        _logoutInProcess = true;

        return Promise.resolve()
        .then(() =>
            dispatch({
                type: actionTypes.logoutStarted,
                payload: {}
            })
        )
        .then(() =>
            logout()
        )
        .catch(error => {
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
            console.log('Error while trying to log out: ', error);
        })
        .then(() => {
            // for the case that we've been logged in to an anonymous account, we need to remove the privateId here.
            if (options_.doRedirects && getIn(state, ['router', 'currentParams', 'privateId'])) {
                return stateGoCurrent({privateId: null})(dispatch, getState);
            }
        })
        /* finally */
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
        .then(() =>
            options_.doRedirects && checkAccessToCurrentRoute(dispatch, getState)
        )
        .then(() => {
            _logoutInProcess = false;
        })
    }
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountRegister(credentials) {
    return (dispatch, getState) =>
        registerAccount(credentials)
        .then(response =>
            accountLogin(credentials, {
                fetchData: false,
                redirectToFeed: true,
            })(dispatch, getState)
        )
        .catch(
            error => {
                //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                const registerError = "Registration failed (E-Mail might already be used)";
                console.error(registerError, error);
                dispatch(actionCreators.registerFailed({ registerError, error }));
            }
        )
}
