/**
 * Created by ksinger on 18.02.2016.
 */

import  won from '../won-es6';
import { actionTypes, actionCreators } from './actions';
import Immutable from 'immutable';
import { selectOpenPostUri } from '../selectors';

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
    */
    fetch('rest/users/isSignedIn', {credentials: 'include'}) //TODO send credentials along
    .then(checkHttpStatus)
    .then(resp => resp.json())
    /* handle data, dispatch actions */
    .then(data =>
        fetchDataForOwnedNeeds(data.username)
    )
    .then(allThatData =>
        dispatch({
            type: actionTypes.initialPageLoad,
            payload: allThatData
        })
    )
    /* handle: not-logged-in */
    .catch(error => {
        const postUri = selectOpenPostUri(getState());
        fetchDataForNonOwnedNeedOnly(postUri)
        .then(publicData =>
            dispatch({
                type: actionTypes.initialPageLoad,
                payload: publicData
            })
        );
    })
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

