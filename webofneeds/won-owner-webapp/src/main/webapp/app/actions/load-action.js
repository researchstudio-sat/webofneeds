/**
 * Created by ksinger on 18.02.2016.
 */

import  won from '../won-es6.js';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions.js';
import { selectOpenPostUri } from '../selectors.js';

import {
    accessControl,
    checkAccessToCurrentRoute,
} from '../configRouting.js';

import {
    stateGoCurrent,
} from './cstm-router-actions.js';

import {
    checkLoginStatus,
    privateId2Credentials,
    login,
    logout,
} from '../won-utils.js';

import config from '../config.js';

import {
    checkHttpStatus,
    getParameterByName,
} from '../utils.js';

import {
    fetchOwnedData,
    fetchDataForNonOwnedNeedOnly,
    emptyDataset,
    wellFormedPayload,
} from '../won-message-utils.js';


export const pageLoadAction = () => (dispatch, getState) => {
    /* TODO the data fetched here should be baked into
    * the send html thus significantly improving the
    * initial page-load-speed.
    * TODO fetch config data here as well
    */

    const privateId = getParameterByName('privateId'); // as this is one of the first action-creators to be executed, we need to get the param directly from the url-bar instead of `state.getIn(['router','currentParams','privateId'])`
    if(privateId) {
        /*
         * we don't have a valid session. however the url might contain `privateId`, which means
         * we're accessing an "accountless"-account and need to sign in with that
         */
        return loadingWithAnonymousAccount(dispatch, getState, privateId)
            .catch(e => {
                loadingWhileSignedOut(dispatch, getState)
            });
    }

    checkLoginStatus()
    /* handle data, dispatch actions */
    .then(data => {
        if(data.username.endsWith('@matchat.org')) {
            // session-cookie is from privateId-session, but there's no privateId in the url-bar => logout to have consistent state again
            return logout().then(() =>
                loadingWhileSignedOut(dispatch, getState)
            );
        }
        return loadingWhileSignedIn(dispatch, getState, data.username)
    })
    .catch(error => {
        /*
         * ok, we're really not logged in -- thus we need to fetch any publicly visible, required data
         */
        return loadingWhileSignedOut(dispatch, getState);
    });
};

function loadingWhileSignedIn(dispatch, getState, username) {


    loginSuccess(username, true, dispatch, getState);
    fetchOwnedData(username, dispatchInitialPageLoad(dispatch));
}

function loadingWithAnonymousAccount(dispatch, getState, privateId) {
    // using an anonymous account. need to log in.
    const {email, password} = privateId2Credentials(privateId);
    return login({privateId})
    /* quickly dispatch log-in status, even before loading data, to
     * allow making correct access-control decisions
     */
    .then(response => {
        loginSuccess(email, true, dispatch, getState);
        return response;
    })
    .then(response =>
        fetchOwnedData(email)
    ).then(allThatData => {
        return dispatch({
            type: actionTypes.initialPageLoad,
            payload: allThatData
        });
    }).catch(e => {
        console.error('failed to sign-in with privateId ', privateId, ' because of: ', e);
        dispatch({
            type: actionTypes.loginFailed,
            payload: { loginError: 'invalid privateId', credentials: { privateId }}
        });
        throw e;
    });
    //dispatch(actionCreators.login(email, password));
    //return; // the login action should fetch the required data, so we're done here.
}

function loginSuccess(username, loginStatus, dispatch, getState) {

    // reset websocket to make sure it's using the logged-in session
    dispatch(actionCreators.reconnect());

    /* quickly dispatch log-in status, even before loading data, to
     * allow making correct access-control decisions
     */
    dispatchInitialPageLoad(dispatch)({email: username, loggedIn: loginStatus});

    checkAccessToCurrentRoute(dispatch, getState);
}

function loadingWhileSignedOut(dispatch, getState) {
    let dataPromise;
    const state = getState();
    const postUri = selectOpenPostUri(state);
    if(postUri && !state.getIn(["needs", postUri])) { //got an uri but no post loaded yet
        dataPromise = fetchDataForNonOwnedNeedOnly(postUri);
    } else {
        dataPromise = Promise.resolve(Immutable.Map());
    }
    return dataPromise.then(publicData =>
        dispatch({
            type: actionTypes.initialPageLoad,
            payload: publicData
        })
    ).then(() =>
        checkAccessToCurrentRoute(dispatch, getState)
    );

}

function dispatchInitialPageLoad(dispatch) {
    return payload => dispatch({
        type: actionTypes.initialPageLoad,
        payload: wellFormedPayload(payload),
    });
}

/////////// THE ACTIONCREATORS BELOW SHOULD BE PART OF PAGELOAD

/**
 * Anything that is load-once, read-only, global app-config
 * should be initialized in this action. Ideally all of this
 * should be baked-in/prerendered when shipping the code, in
 * future versions => TODO
 */
export function configInit() {
    return (dispatch) =>
        /* this allows the owner-app-server to dynamically switch default nodes. */
        fetch(/*relativePathToConfig=*/'appConfig/getDefaultWonNodeUri')
            .then(checkHttpStatus)
            .then(resp => resp.json())
            .catch(err => {
                const defaultNodeUri = `${location.protocol}://${location.host}/won/resource`;
                console.info(
                    'Failed to fetch default node uri at the relative path `',
                    'appConfig/getDefaultWonNodeUri',
                    '` (is the API endpoint there up and reachable?) -> falling back to the default ',
                    defaultNodeUri
                );
                return defaultNodeUri;
            })
            .then(defaultNodeUri =>
                dispatch(actionCreators.config__update({defaultNodeUri}))
        )
}

