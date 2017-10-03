/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6.js';
import Immutable from 'immutable';
import jsonld from 'jsonld'; //import *after* the rdfstore to shadow its custom jsonld

import {
    selectAllByConnectionUri,
    selectOpenConnectionUri,
    selectRemoteEvents,
} from '../selectors.js';

import {
    is,
    urisToLookupMap,
    msStringToDate,
    getIn,
    jsonld2simpleFormat,
} from '../utils.js';

import {
   makeParams,
} from '../configRouting.js';

import {
    actionTypes,
    actionCreators,
} from './actions.js';

import {
    buildOpenMessage,
    buildCloseMessage,
    buildChatMessage,
    buildRateMessage,
    buildConnectMessage,
    getEventsFromMessage,
} from '../won-message-utils.js';

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

            dispatch(actionCreators.router__stateGoAbs("post", {
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
        const eventData = selectAllByConnectionUri(state, connectionUri).toJS(); // TODO avoid toJS; UPDATE TO NEW STRUCTURE
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
                        optimisticEvent: event,
                    }
                });
            });
        })
    }
}

export function connectionsClose(connectionUri) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnectionUri(state, connectionUri).toJS();// TODO avoid toJS; UPDATE TO NEW STRUCTURE
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;

        const promise = new Promise((resolve, defer) => {
            won.getConnectionWithEventUris(eventData.connection.uri).then(connection=> {
                let msgToOpenFor = {event: eventData, connection: connection};
                buildCloseMessage(msgToOpenFor).then(messageData=> {
                    resolve(messageData);
                })
            });
        });

        promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
            dispatch({
                type: actionTypes.connections.close,
                payload: { connectionUri }
            });
        })
    }
}

export function connectionsRate(connectionUri,rating) {
    return (dispatch, getState) => {
        console.log(connectionUri);
        console.log(rating);

        const state = getState();
        const eventData = selectAllByConnectionUri(state, connectionUri).toJS();// TODO avoid toJS; UPDATE TO NEW STRUCTURE
        let messageData = null;

        won.getConnectionWithEventUris(eventData.connection.uri)
            .then(connection=> {
                let msgToRateFor = {event: eventData, connection: connection};
                return buildRateMessage(msgToRateFor, rating)
            }).then(action =>
                dispatch(
                    actionCreators.messages__send({
                        eventUri: action.eventUri,
                        message: action.message
                    })
                )
            );
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
        const allByConnectionUri = connectionUri && selectAllByConnectionUri(state, connectionUri);

        const connection = allByConnectionUri && allByConnectionUri.get("connection"); //TODO: UPDATE METHOD ACCORDING TO NEW STRUCTURE

        if (!connectionUri || !connection) return;

        const events = allByConnectionUri && allByConnectionUri.get("events");
        if (connection.get('loadingEvents') || !events || events.size > 0) return; // only start loading once. //TODO: PENDING IS CURRENTLY NOT IMPLEMENTED IN THE NEW STATE

        //TODO a `return` here might be a race condition that results in this function never being called.
        //TODO the delay solution is super-hacky (idle-waiting)
        // -----> if(!self.connection__showLatestEvent) delay(100).then(loadStuff); // we tried to call this before the action-creators where attached.

        console.log('connections-actions.js: testing for selective loading. ', connectionUri, connection);
        //TODO determine first if component is actually visible (angular calls the constructor long before that)

        dispatch({
            type: actionTypes.connections.showLatestMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });

        const requesterWebId = allByConnectionUri && allByConnectionUri.getIn(["ownNeed", "uri"]);

        getEvents(
            connection.get('uri'),
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

//TODO replace the won.getEventsOfConnection with this version (and make sure it works for all previous uses).
/**
 * Gets the events and uses the paging-parameters
 * in a meaningful fashion.
 * @param eventContainerUri
 * @param params
 * @return {*}
 */
function getEvents(connectionUri, params) {
    const eventP = won
        .getNode(connectionUri, params)
        .then(cnct =>
            won.getNode(cnct.hasEventContainer, params)
        )
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
        const allByConnectionUri = connectionUri && selectAllByConnectionUri(state, connectionUri);
        const connection = allByConnectionUri && allByConnectionUri.get("connection"); //TODO: UPDATE METHOD ACCORDING TO NEW STRUCTURE
        const requesterWebId = allByConnectionUri && allByConnectionUri.getIn(["ownNeed", "uri"]);
        const ownEvents = allByConnectionUri.get("events");
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
            connection.get('uri'),
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

function numOfEvts2pageSize(numberOfEvents) {
     // `*3*` to compensate for the *roughly* 2 additional success events per chat message
    return numberOfEvents * 3;
}
