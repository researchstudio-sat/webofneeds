/**
 * Created by ksinger on 18.02.2016.
 */

import  won from '../won-es6';
import Immutable from 'immutable';
import { actionTypes, actionCreators } from './actions';
import { selectOpenPostUri, selectOpenPost } from '../selectors';

import {
    checkHttpStatus,
    entries,
    flatten,
    flattenObj,
    urisToLookupMap
} from '../utils';

import {
    fetchDataForOwnedNeeds,
    fetchDataForNonOwnedNeedOnly,
} from '../won-message-utils';

export const pageLoadAction = () => (dispatch, getState) => {
    /* TODO the data fetched here should be baked into
    * the send html thus significantly improving the
    * initial page-load-speed.
    * TODO fetch config data here as well
    */
    fetch('rest/users/isSignedIn', {credentials: 'include'})
    .then(checkHttpStatus)
    .then(resp => resp.json())
    /* handle data, dispatch actions */
    .then(data => loadingWhileSignedIn(dispatch, data.username))
    /* handle: not-logged-in */
    .catch(error => loadingWhileSignedOut(dispatch, getState));
}

function loadingWhileSignedIn(dispatch, username) {
    dispatch({
        type: actionTypes.initialPageLoad,
        payload: Immutable.fromJS({
            email: username,
            loggedIn: true
        })
    });
    fetchDataForOwnedNeeds(username)
    .then(allThatData =>
        dispatch({
            type: actionTypes.initialPageLoad,
            payload: allThatData
        })
    )
}

function loadingWhileSignedOut(dispatch, getState) {
    const state = getState();
    const postUri = selectOpenPostUri(state);
    if(postUri && !selectOpenPost(state)) { //got an uri but no post loaded yet
        fetchDataForNonOwnedNeedOnly(postUri)
            .then(publicData =>
                dispatch({
                    type: actionTypes.initialPageLoad,
                    payload: publicData
                })
        );
    } else {
        dispatch({
            type: actionTypes.initialPageLoad,
            payload: Immutable.Map()
        })
    }

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

