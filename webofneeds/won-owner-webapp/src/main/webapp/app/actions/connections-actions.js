/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6';
import Immutable from 'immutable';


import { getRandomPosInt } from '../utils';

import {
    selectAllByConnections,
    selectOpenConnectionUri,
    selectOpenConnection,
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
    buildRateMessage,
    buildConnectMessage,
    setCommStateFromResponseForLocalNeedMessage,
    getEventsFromMessage,
} from '../won-message-utils';

export function connectionsChatMessage(chatMessage, connectionUri) {
   return dispatch => {
       console.log('connectionsChatMessage: ', chatMessage, connectionUri);

       won.getEnvelopeDataforConnection(connectionUri)
       .then(envelopeData => {
           const eventUri = envelopeData[won.WONMSG.hasSenderNode] + "/event/" +  getRandomPosInt(1,9223372036854775807);

           /*
            * Build the json-ld message that's signed on the owner-server
            * and then send to the won-node.
            */
           const message = new won.MessageBuilder(won.WONMSG.connectionMessage)
               .eventURI(eventUri)
               .forEnvelopeData(envelopeData)
               .addContentGraphData(won.WON.hasTextMessage, chatMessage)
               .hasOwnerDirection()
               .hasSentTimestamp(new Date().getTime().toString())
               .build();

           /*
            * not sure how elegant it is to build the ld and then parse
            * it again. It uses existing utilities at least, reducing
            * redundant program logic. ^^
             */
           const optimisticEventPromise = getEventsFromMessage(message)
               .then(optimisticEvent => optimisticEvent['msg:FromOwner']);

           return Promise.all([
               Promise.resolve(message),
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

export function connectionsOpen(connectionUri,message) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionUri).toJS(); // TODO avoid toJS;
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer();
        won.getConnectionWithEventUris(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection};
            buildOpenMessage(msgToOpenFor, message).then(messageData=> {
                console.log("built open message");
                deferred.resolve(messageData);
            })
        });
        deferred.promise.then((action)=> {
            console.log("dispatching messages__send action"+ action);
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
        })
    }
}

export function connectionsConnect(connectionUri,message) {
    return (dispatch, getState) => {
        const state = getState();
        const eventData = selectAllByConnections(state).get(connectionUri).toJS(); // TODO avoid toJS;
        //let eventData = state.getIn(['connections', 'connectionsDeprecated', connectionData.connection.uri])
        let messageData = null;
        let deferred = Q.defer();
        won.getConnectionWithEventUris(eventData.connection.uri).then(connection=> {
            let msgToOpenFor = {event: eventData, connection: connection};
            buildConnectMessage(msgToOpenFor, message).then(messageData=> {
                deferred.resolve(messageData);
            })
        });
        deferred.promise.then((action)=> {
            dispatch(actionCreators.messages__send({eventUri: action.eventUri, message: action.message}));
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

        won.getNode(
            connection.get('hasEventContainer'),
            {
                requesterWebId,
                pagingSize: numOfEvts2pageSize(numberOfEvents),
                deep: true
            }
        )
        .then(eventContainer => {
            const eventUris =  is('Array', eventContainer.member) ?
                eventContainer.member :
                [eventContainer.member];

            return urisToLookupMap(
                eventUris,
                    uri => won.getEvent(uri, {requesterWebId})
            )
        })
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
        const events = selectEventsOfConnection(state, connectionUri);

        /* TODO expand set of uris from latest chat message via daisy-chaining?
         * they have multiple predecessors, i.e. success response and previous chat message
         * and this way we can find holes in the loaded messages.
         * Prob: when daisy-chaining: make sure to look into the correspondingRemoteMessage to get links between their messages
         * store normalized and write a selector to get event+remote? (for old code)
         * or look through *all* events here to find the event we're looking for.
         */
        // determine the oldest loaded event
        const sortedEvents = events.sort((e1, e2) =>
            msStringToDate(e1.get('hasReceivedTimestamp')) -
            msStringToDate(e2.get('hasReceivedTimestamp'))
        );
        const oldestEvent = sortedEvents.first();
        const latestEvent = sortedEvents.last();
        const eventHashValue = oldestEvent
                .get('uri')
                .replace(/.*\/event\/(.*)/, '$1'); // everything following the `/event/`

        won.getNode(
            connection.get('hasEventContainer'),
            {
                requesterWebId,
                pagingSize: numOfEvts2pageSize(numberOfEvents),
                deep: true,
                resumebefore: eventHashValue,
            }
        ).then(eventContainer =>
            console.log('showMoreMessages: ', eventContainer)
        )






    }
}

function numOfEvts2pageSize(numberOfEvents) {
     // `*3*` to compensate for the *roughly* 2 additional success events per chat message
    return numberOfEvents * 3;
}
