/**
 * Created by ksinger on 18.02.2016.
 */

import { actionTypes, actionCreators } from './actions';
import { checkHttpStatus, entries } from '../utils';
import  won from '../won-es6';

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

window.entries4dbg = entries;
function fetchAllAccessibleAndRelevantData(ownNeedUris) {

    won.urisToLookupMap(ownNeedUris, won.getNeed).then(ownNeeds => {
        //ownNeeds[needUri]
        //TODO
    });

    Promise.all(ownNeedUris.map(won.getConnectionsOfNeed))
        .then(connectionsOfNeeds => connectionsOfNeeds.reduce(
            // merge the connections-per-need into a single connections object
            // this assumes that connectionUris are unique!
            (connections, connectionsForOneNeed) =>
                Object.assign(connections, connectionsForOneNeed), {})
        )
        .then(connections => {
            //connections[connectionUri]
            //TODO





            connections.map(connection => {
                connection.uri;
                connection.belongsToNeed;



            })

        });

    const connectionUris = [] //TODO get from keys of the connections object above





    //won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], needUri)

    const allAccessibleAndRelevantData = {/*...*/};
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
