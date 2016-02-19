/**
 * Created by ksinger on 18.02.2016.
 */

import { actionTypes, actionCreators } from './actions';
import { checkHttpStatus, entries, flatten, flattenObj } from '../utils';
import  won from '../won-es6';

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
    .then(response => response.json())
    .then(needUris => fetchAllAccessibleAndRelevantData(needUris))
    .then(allThatData => dispatch({type: actionTypes.load, payload: allThatData}))
    .catch(error => dispatch(actionCreators.needs__failed({
                error: "user needlist retrieval failed"
            })
        )
    );
}

function fetchAllAccessibleAndRelevantData(ownNeedUris) {

    const allOwnNeedsPromise = won.urisToLookupMap(ownNeedUris, won.getNeed);

    const allConnectionUrisPromise =
        Promise.all(ownNeedUris.map(won.getconnectionUrisOfNeed))
        .then(connectionUrisPerNeed => flatten(connectionUrisPerNeed));

    const allConnectionsPromise = allConnectionUrisPromise
        .then(connectionUris => won.urisToLookupMap(connectionUris, won.getConnection));

    const allEventsPromise = allConnectionUrisPromise
        .then(connectionUris =>
           won.urisToLookupMap(connectionUris, connectionUri =>
               won.getConnection(connectionUri)
               .then(connection =>
                       won.getEventsOfConnection(connectionUri,connection.belongsToNeed)
               )
           )
        ).then(eventsOfConnections =>
            //eventsPerConnection[connectionUri][eventUri]
            flattenObj(eventsOfConnections)
        );

    const allTheirNeedsPromise =
        allConnectionsPromise.then(connections => {
            const theirNeedUris = [];
            for(const [connectionUri, connection] of entries(connections)) {
                theirNeedUris.push(connection.hasRemoteNeed);
            }
            return theirNeedUris;
        })
        .then(theirNeedUris => won.urisToLookupMap(theirNeedUris, won.getNeed));

    Promise.all([
        allOwnNeedsPromise,
        allConnectionsPromise,
        allEventsPromise,
        allTheirNeedsPromise
    ]).then(allAccessibleAndRelevantData =>
        console.log('\n\n\n', allAccessibleAndRelevantData, '\n\n\n')
    )

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
