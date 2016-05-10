/**
 * Created by ksinger on 18.02.2016.
 */

import  won from '../won-es6';
import { actionTypes, actionCreators } from './actions';
import Immutable from 'immutable';

import {
    checkHttpStatus,
    entries,
    flatten,
    flattenObj,
    urisToLookupMap
} from '../utils';

import { fetchAllAccessibleAndRelevantData } from '../won-message-utils';


export const loadAction = () => dispatch => {
    fetch('/owner/rest/needs/', {
        method: 'get',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        credentials: 'include'
    })
    .then(checkHttpStatus)
    .then(response =>
            response.json())
    .then(needUris =>
            fetchAllAccessibleAndRelevantData(needUris))
    .then(allThatData =>
            Immutable.fromJS(allThatData)) //!!!
    .then(allThatData => {
            dispatch({type: actionTypes.load, payload: allThatData})
            //dispatch({ type: actionTypes.needs.fetch, payload: needs });
    })
    .catch(error => dispatch(actionCreators.needs__failed({
                error: "user needlist retrieval failed",
                e: error
            })
        )
    );
}


/////////// THE ACTIONCREATORS BELOW SHOULD BE PART OF LOAD

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

