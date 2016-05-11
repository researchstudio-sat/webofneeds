/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import { actionTypes, actionCreators } from './actions';
import { fetchDataForOwnedNeeds } from '../won-message-utils';

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
            fetchDataForOwnedNeeds(username)
        )
        .then(allThatData =>
            dispatch({
                type: actionTypes.login,
                payload: allThatData
            })
        )
        .then(() => {
            dispatch(actionCreators.messages__requestWsReset_Hack());
            dispatch(actionCreators.router__stateGo("feed"));
        })
        .catch(error => {
            //TODO load data of non-owned need!!!
            dispatch({
                type: actionTypes.login,
                payload: Immutable.fromJS({loggedIn: false})
            })
            dispatch(actionCreators.loginFailed({loginError: "No such username/password combination registered."}))
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
        .then(response =>
            dispatch({
                type: actionTypes.logout,
                payload: Immutable.fromJS({loggedIn: false})
            })
        )
        .then(() => {
            dispatch(actionCreators.messages__requestWsReset_Hack());
            dispatch(actionCreators.router__stateGo("landingpage"));
        })
        .catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
            error => {
                console.log(error);
            }
        )
}

export function accountRegister(username, password) {
    return (dispatch) =>
        fetch('/owner/rest/users/', {
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
        .then(
                response => {
                    /* TODO shouldn't we already have a valid
                    * session at this point and thus just need
                    * to execute the data-fetching part of login
                    * (the fetchDataForOwnedNeeds, redirect
                    * and wsReset)
                    */
                    dispatch(actionCreators.login(username, password));
                }
            )
        .catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                error =>
                    dispatch(actionCreators.registerFailed({registerError: "Registration failed"}))
        )
}
