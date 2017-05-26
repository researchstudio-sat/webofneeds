/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import Immutable from 'immutable';


import jsonld from 'jsonld'; //import *after* the rdfstore to shadow its custom jsonld

import { getRandomPosInt } from '../utils';

import {
    selectAllByConnections,
    selectOpenConnectionUri,
    selectOpenConnection,
    selectRemoteEvents,
} from '../selectors';

import {
    selectEventsOfConnection,
    selectTimestamp,
} from '../won-utils';

import {
    checkHttpStatus,
    is,
    urisToLookupMap,
    msStringToDate,
    getIn,
    jsonld2simpleFormat,
} from '../utils';

import {
    actionTypes,
    actionCreators,
    getConnectionRelatedData,
} from './actions';

import {
    buildCreateMessage,
    buildOpenMessage,
    buildCloseMessage,
    buildChatMessage,
    buildRateMessage,
    buildConnectMessage,
    setCommStateFromResponseForLocalNeedMessage,
    getEventsFromMessage,
} from '../won-message-utils';

export function connectionsChatMessage(chatMessage, connectionUri) {
   return dispatch => {
       console.log('connectionsChatMessage: ', chatMessage, connectionUri);

       buildChatMessage(chatMessage, connectionUri)
       .then(msgData => {

           /*
            * not sure how elegant it is to build the ld and then parse
            * it again. It uses existing utilities at least, reducing
            * redundant program logic. ^^
             */
           const optimisticEventPromise = getEventsFromMessage(msgData.message)
               .then(optimisticEvent => optimisticEvent['msg:FromOwner']);

           return Promise.all([
               Promise.resolve(msgData.message),
               optimisticEventPromise,
           ]);
       })
       .then( ([ message, optimisticEvent ]) => dispatch({
               type: actionTypes.connections.sendChatMessage,
               payload: {
                   eventUri: optimisticEvent.uri,
                   message,
                   optimisticEvent,
               }
           })
       )
   }
}

export function connectionsFetch(data) {
    return dispatch=> {
        const allConnectionsPromise = won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], data.needUri);
        allConnectionsPromise.then(function (connections) {
            console.log("fetching connections");
            dispatch(actionCreators.needs__connectionsReceived({needUri: data.needUri, connections: connections}));
            dispatch(actionCreators.events__fetch({connectionUris: connections}))
        })
    }
}

export function connectionsOpen(connectionUri, message) {
    return (dispatch, getState) => {
        buildOpenMessage(connectionUri, message)
        .then(msgData => {
            const optimisticEventPromise = getEventsFromMessage(msgData.message)
                .then(optimisticEvent => optimisticEvent['msg:FromOwner']);
            return Promise.all([
                Promise.resolve(msgData),
                optimisticEventPromise,
            ]);

            //TODO dispatch connections.open
        })
        .then( ([ msgData, optimisticEvent ]) => {
            // dispatch(actionCreators.messages__send(messageData));
            dispatch({
                type: actionTypes.connections.open,
                payload: {
                    eventUri: optimisticEvent.uri,
                    message: msgData.message,
                    optimisticEvent,
                }
            });

            dispatch(actionCreators.router__stateGo("post", {
                postUri: optimisticEvent.hasSenderNeed,
                connectionType: won.WON.Connected,
                connectionUri: optimisticEvent.hasSender,
            }));

        });
    }
}


export function connectionsConnect(connectionUri,message) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionUri).toJS(); // TODO avoid toJS;
        const messageDataP = won
            .getConnectionWithEventUris(eventData.connection.uri)
            .then(connection=> {
                let msgToOpenFor = {event: eventData, connection: connection};
                return buildConnectMessage(msgToOpenFor, message)
            });
        messageDataP.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));

            jsonld.promises.frame(
                action.message,
                {
                    '@id': action.eventUri,
                    '@context': action.message['@context']
                }
            ).then(framed => {
                let event = getIn(framed, ['@graph', 0]);
                if(event) {
                    event['@context'] = framed['@context']; // context is needed by jsonld2simpleFormat for expanding prefixes in values
                    event = jsonld2simpleFormat(event);
                }

                dispatch({
                    type: actionTypes.connections.connect,
                    payload: {
                        connectionUri,
                        message,
                        eventUri: action.eventUri,
                        event: event,
                    }
                });
            });
        })
    }
}

export function connectionsClose(connectionUri) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionUri).toJS();// TODO avoid toJS
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer();
        won.getConnectionWithEventUris(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection};
            buildCloseMessage(msgToOpenFor).then(messageData=> {
                deferred.resolve(messageData);
            })
        });
        deferred.promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}

export function connectionsRate(connectionUri,rating) {
    return (dispatch, getState) => {
        console.log(connectionUri);
        console.log(rating);

        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionUri).toJS();// TODO avoid toJS
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer();
        won.getConnectionWithEventUris(eventData.connection.uri).then(connection=> {
            let msgToRateFor = {event: eventData, connection: connection};
            buildRateMessage(msgToRateFor, rating).then(messageData=> {
                deferred.resolve(messageData);
            })
        });
        deferred.promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}

/**
 * @param connectionUri
 * @param numberOfEvents
 *   The approximate number of chat-message
 *   that the view needs. Note that the
 *   actual number varies due the varying number
 *   of success-responses the server includes and
 *   because the API only accepts a count of
 *   events that include the latter.
 * @return {Function}
 */
export function showLatestMessages(connectionUri, numberOfEvents){
    return (dispatch, getState) => {
        const state = getState();
        const connectionUri = selectOpenConnectionUri(state);
        const connection = selectOpenConnection(state);

        if (!connectionUri || !connection) return;

        const eventUris = connection.get('hasEvents');
        if (connection.get('loadingEvents') || !eventUris || eventUris.size > 0) return; // only start loading once.

        //TODO a `return` here might be a race condition that results in this function never being called.
        //TODO the delay solution is super-hacky (idle-waiting)
        // -----> if(!self.connection__showLatestEvent) delay(100).then(loadStuff); // we tried to call this before the action-creators where attached.

        console.log('connections-actions.js: testing for selective loading. ', connectionUri, connection);
        //TODO determine first if component is actually visible (angular calls the constructor long before that)

        dispatch({
            type: actionTypes.connections.showLatestMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });

        const requesterWebId = connection.get('belongsToNeed');

        getEvents(
            connection.get('hasEventContainer'),
            {
                requesterWebId,
                pagingSize: numOfEvts2pageSize(numberOfEvents),
                deep: true
            }
        )
        .then(events =>
            dispatch({
                type: actionTypes.connections.showLatestMessages,
                payload: Immutable.fromJS({
                    connectionUri: connectionUri,
                    events: events,
                })
            })
        )
        .catch(error => {
            console.error('Failed loading the latest events: ', error);
            dispatch({
                type: actionTypes.connections.showLatestMessages,
                payload: Immutable.fromJS({
                    connectionUri: connectionUri,
                    error: error,
                })
            })
        });
    }
}

//TODO replace the won.getEventsOfConnection with this version (and make sure it works
// for all previous uses).
/**
 * Gets the events and uses the paging-parameters
 * in a meaningful fashion.
 * @param eventContainerUri
 * @param params
 * @return {*}
 */
function getEvents(eventContainerUri, params) {
    const eventP = won
        .getNode(eventContainerUri, params)
        .then(eventContainer => is('Array', eventContainer.member) ?
            eventContainer.member :
            [eventContainer.member]
        )
        .then(eventUris => urisToLookupMap(
            eventUris,
            uri => won.getEvent(
                uri,
                { requesterWebId: params.requesterWebId }
            )
        ));

    return eventP;

}
/**
 * @param connectionUri
 * @param numberOfEvents
 *   The approximate number of chat-message
 *   that the view needs. Note that the
 *   actual number varies due the varying number
 *   of success-responses the server includes and
 *   because the API only accepts a count of
 *   events that include the latter.
 * @return {Function}
 */
export function showMoreMessages(connectionUri, numberOfEvents) {
    return (dispatch, getState) => {

        const state = getState();
        const connectionUri = selectOpenConnectionUri(state);
        const connection = selectOpenConnection(state);
        const requesterWebId = connection.get('belongsToNeed');
        const ownEvents = selectEventsOfConnection(state, connectionUri);
        const remoteEvents =
            selectRemoteEvents(state)
            .filter(e =>
                e.get('hasReceiver') === connectionUri ||
                e.get('hasSender') === connectionUri
            );

        /* TODO expand set of uris from latest chat message via daisy-chaining?
         * they have multiple predecessors, i.e. success response and previous chat message
         * and this way we can find holes in the loaded messages.
         * Prob: when daisy-chaining: make sure to look into the correspondingRemoteMessage to get links between their messages
         * store normalized and write a selector to get event+remote? (for old code)
         * or look through *all* events here to find the event we're looking for.
         */
        // determine the oldest loaded event
        //alternative approach sort everything together
        //getOldestEventInChain(state, connectionUri)

        // determine the oldest loaded event
        const sortByTime = (someEvents, pathToMsString) =>
            someEvents.sort((e1, e2) =>
                msStringToDate(e1.getIn(pathToMsString)) -
                msStringToDate(e2.getIn(pathToMsString))
            );

        const sortedOwnEvents = sortByTime(ownEvents, ['hasReceivedTimestamp']);

        const oldestEvent = sortedOwnEvents.first();
        const eventHashValue = oldestEvent
                .get('uri')
                .replace(/.*\/event\/(.*)/, '$1'); // everything following the `/event/`

        // chain is more of a cycle-free graph
        // find all the ones w/o predecessors
        // then take the oldest of these
        // make sure to always take the timestamp on the own node


        dispatch({
            type: actionTypes.connections.showMoreMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });

        getEvents(
            connection.get('hasEventContainer'),
            {
                requesterWebId,
                pagingSize: numOfEvts2pageSize(numberOfEvents),
                deep: true,
                resumebefore: eventHashValue,
            }
        )
        .then(events =>
            dispatch({
                type: actionTypes.connections.showMoreMessages,
                payload: Immutable.fromJS({
                    connectionUri: connectionUri,
                    events: events,
                })
            })
        )
        .catch(error => {
            console.error('Failed loading more events: ', error);
            dispatch({
                type: actionTypes.connections.showMoreMessages,
                payload: Immutable.fromJS({
                    connectionUri: connectionUri,
                    error: error,
                })
            })
        });






    }
}

function getOldestEventInChain(state, connectionUri) {

    const ownEvents = selectEventsOfConnection(state, connectionUri);
    const remoteEvents =
        selectRemoteEvents(state)
            .filter(e =>
            e.get('hasReceiver') === connectionUri ||
            e.get('hasSender') === connectionUri
        );

    const sortByTime = (someEvents, pathToMsString) =>
        someEvents.sort((e1, e2) =>
            msStringToDate(e1.getIn(pathToMsString)) -
            msStringToDate(e2.getIn(pathToMsString))
        );

    const sortedOwnEvents = sortByTime(ownEvents, ['hasReceivedTimestamp']);
    const sortedRemoteEvents = sortByTime(remoteEvents, ['correspondsToOwnMsg', 'hasReceivedTimestamp']);

    const timestamp = e => msStringToDate(
        e.getIn(['correspondsToOwnMsg', 'hasReceivedTimestamp']) || //remoteEvent
        e.get('hasReceivedTimestamp') // ownEvent
    );
    const allEvents = ownEvents.merge(remoteEvents);
    const allEventsSorted = allEvents.sort((e1, e2) => timestamp(e1) - timestamp(e2));

    //start with the latest event
    const latestEvent = allEventsSorted.last();
    let latestEvents = Immutable.Map()
        .set(latestEvent.get('uri'), latestEvent);

    const prevMsgs = e => {
        const prev =
            e.getIn(['correspondsToOwnMsg', 'hasPreviousMessage']) || //remoteEvent
            e.get('hasPreviousMessage'); // ownEvent or remote's own predecessors
        //make sure we get an array
        if(is('Array', prev)) {
            return prev;
        } else {
            return [prev];
        }
    };

    //recursively add all connected events
    //let frontier = Immutable.Set()
    let acc = Immutable.Map()
        .add('frontier', Immutable.Set())
        .add('earliestLoaded', Immutable.Set());
    for([uri, e] of latestEvents.entries()) {
        const previous = prevMsgs(e);
        if(!previous) continue;
    }

}

function numOfEvts2pageSize(numberOfEvents) {
     // `*3*` to compensate for the *roughly* 2 additional success events per chat message
    return numberOfEvents * 3;
}
