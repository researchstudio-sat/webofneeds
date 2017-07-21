/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions';
import { fetchOwnedData } from '../won-message-utils';
import { registerAccount } from '../won-utils';
import {
   resetParams,
} from '../configRouting';

import {
    checkHttpStatus,
} from '../utils';

export function accountLogin(username, password) {
    return (dispatch) =>
        fetch('/owner/rest/users/signin', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({username: username, password: password})
        })
        .then(
            checkHttpStatus
        )
        .then( response =>
            fetchOwnedData(username)
        )
        .then(allThatData =>
            dispatch({
                type: actionTypes.login,
                payload: allThatData
            })
        )
        .then(() => {
            /**
             * TODO this action is part of the session-upgrade hack documented in:
             * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
             */
            dispatch(actionCreators.reconnect());
            dispatch(actionCreators.router__stateGoResetParams("feed"));
        })
        .catch(error => {
            console.log("accountLogin ErrorObject", error);
            //TODO load data of non-owned need!!!
            dispatch({
                type: actionTypes.login,
                payload: Immutable.fromJS({loggedIn: false})
            })
            dispatch(actionCreators.loginFailed({loginError: error.msg? error.msg : "Unknown Username/Password Combination", error}))
        })
}

export function accountLogout() {
    return (dispatch) =>
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
            dispatch(actionCreators.router__stateGoResetParams("landingpage"));
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
            dispatch(actionCreators.login(username, password));
        })
        .catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                error =>
                    dispatch(actionCreators.registerFailed({registerError: "Registration failed (E-Mail might already be used)", error}))
        )
}
