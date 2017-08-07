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
} from '../won-utils';
import {
    resetParams,
    checkAccessToCurrentRoute,
} from '../configRouting';

import {
    checkHttpStatus,
} from '../utils';

export function anonAccountLogin(privateId) {
    const {email, password} = privateId2Credentials(privateId);
    return accountLogin(email, password);
}
export function accountLogin(username, password, fetchData = true) {
    return (dispatch) =>
        login(username, password)
        .then( response =>
            fetchData ? fetchOwnedData(username) : Immutable.Map() // only need to fetch data for non-new accounts
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
        .then(() =>
            dispatch(actionCreators.router__stateGoResetParams("feed"))
        )
        .catch(error => {
            console.log("accountLogin ErrorObject", error);
            return dispatch(actionCreators.loginFailed({
                loginError: error.msg?
                    error.msg :
                    "Unknown Username/Password Combination",
                error
            }))
        })
}

export function accountLogout() {
    return (dispatch, getState) =>
        fetch('/owner/rest/users/signout', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({})
        })
        .then(
            checkHttpStatus
        )
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
            dispatch(actionCreators.router__stateGoCurrent({privateId: undefined}));

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
            accountLogin(username, password, false)(dispatch);
        })
        .catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                error =>
                    dispatch(actionCreators.registerFailed({registerError: "Registration failed (E-Mail might already be used)", error}))
        )
}
