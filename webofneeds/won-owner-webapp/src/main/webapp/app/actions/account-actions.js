/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import { actionTypes, actionCreators } from './actions';

import {
    checkHttpStatus,
} from '../utils';

export function accountVerifyLogin() {
    return dispatch => {
        fetch('rest/users/isSignedIn', {credentials: 'include'}) //TODO send credentials along
            .then(checkHttpStatus)
            .then(resp => resp.json())
            /* handle data, dispatch actions */
            .then(data => {
                dispatch(actionCreators.user__loggedIn({loggedIn: true, email: data.username}));
                dispatch(actionCreators.retrieveNeedUris());
            })
            /* handle: not-logged-in */
            .catch(error =>
                dispatch(actionCreators.user__loggedIn({loggedIn: false}))
        );
        ;
    }
}

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
        }).then(checkHttpStatus)
            .then(response => {
                return response.json()
            }).then(
                data => {
                dispatch(actionCreators.user__loggedIn({loggedIn: true, email: username}));
                dispatch(actionCreators.messages__requestWsReset_Hack());
                dispatch(actionCreators.retrieveNeedUris());
                //dispatch(actionCreators.posts__load());
                dispatch(actionCreators.router__stateGo("feed"));
            }
        ).catch(
                error => dispatch(actionCreators.user__loginFailed({loginError: "No such username/password combination registered."}))
        )
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
        }).then(checkHttpStatus)
            .then(response => {
                return response.json()
            }).then(
                data => {
                dispatch(actionCreators.messages__requestWsReset_Hack());
                dispatch(actionCreators.user__loggedIn({loggedIn: false}));
                dispatch(actionCreators.needs__clean({needs: {}}));
                dispatch(actionCreators.posts__clean({}));
                dispatch(actionCreators.connections__reset({}))
                dispatch(actionCreators.router__stateGo("landingpage"));
            }
        ).catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
                error => {
                console.log(error);
                dispatch(actionCreators.user__loggedIn({loggedIn: true}))
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
        }).then(checkHttpStatus)
            .then(response => {
                return response.json()
            }).then(
                data => {
                dispatch(actionCreators.login(username, password))
                /*                    dispatch(actionCreators.user__loggedIn({loggedIn: true, email: username}));
                 dispatch(actionCreators.router__stateGo("createNeed"));*/
            }
        ).catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
                error => dispatch(actionCreators.user__registerFailed({registerError: "Registration failed"}))
        )
}
