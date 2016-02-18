/**
 * Created by ksinger on 18.02.2016.
 */

import { actionTypes, actionCreators } from './actions';
import { checkHttpStatus } from '../utils';


export const loadAction = () => dispatch => {
    window.needUris4dbg = needUris;
    fetch('/owner/rest/needs/', {
        method: 'get',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
        },
        credentials: 'include'
    })
    .then(checkHttpStatus)
    .then(response => response.json())
    .then(needUris => fetchAllAccessibleAndRelevantData(needUris))
    .then(allThatData => dispatch({type: actionTypes.load, payload: allThatData}))
    .catch(error => dispatch(actionCreators.needs__failed({
                error: "user needlist retrieval failed"
            })
        )
    );
}

function fetchAllAccessibleAndRelevantData(needUris) {

    const allAccessibleAndRelevantData = {/*...*/}
    /**
     const allAccessibleAndRelevantData = {
                ownNeeds: {
                    <needUri> : {
                        *:*,
                        connections: [<connectionUri>, <connectionUri>]
                    }
                    <needUri> : {
                        *:*,
                        connections: [<connectionUri>, <connectionUri>]
                    }
                },
                theirNeeds: {
                    <needUri>: {
                        *:*,
                        connections: [<connectionUri>, <connectionUri>] <--?
                    }
                },
                connections: {
                    <connectionUri> : {
                        *:*,
                        events: [<eventUri>, <eventUri>]
                    }
                    <connectionUri> : {
                        *:*,
                        events: [<eventUri>, <eventUri>]
                    }
                }
                events: {
                    <eventUri> : { *:* },
                    <eventUri> : { *:* }
                }
             }


     */



    //return promiseForAllData
}
