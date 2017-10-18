/**
 * Created by ksinger on 19.02.2016.
 */


import  won from '../won-es6.js';
import { actionTypes, actionCreators, getConnectionRelatedData } from './actions.js';

import Immutable from 'immutable';

import {
    clone,
    jsonld2simpleFormat,
    getIn,
} from '../utils.js';

import {
    fetchDataForOwnedNeeds
} from '../won-message-utils.js';

import {
    makeParams,
    resetParams,
} from '../configRouting.js';

export function successfulCloseNeed(event) {
    return (dispatch, getState) => {
        console.log("got response for DEACTIVATE: " + event.getMessageType());
        //TODO maybe refactor these response message handling
        if (getState().getIn(['messages', 'waitingForAnswer', event.getIsRemoteResponseTo()])) {
            console.log("messages waitingForAnswer", event.getMessageUri());
            //dispatch(actionCreators.connections__denied(event));
        }
    }
}
export function failedCloseNeed(event) {
    return (dispatch, getState) => {
        const needUri = event.getReceiverNeed();
        /*
        * TODO not sure if it's necessary to invalidate
        * the cache here as the previous action will just have
        * been an optimistic update of the state. Invalidation
        * should happen in the action that causes the interaction
        * with the server.
        */
        won.invalidateCacheForNeed(needUri) // mark need and it's connection container dirty
            .then(() =>
                won.getConnectionUrisOfNeed(needUri)
            ).then(connectionUris =>
                Promise.all(
                    connectionUris.map(cnctUri =>
                        won.invalidateCacheForNewMessage(cnctUri, needUri) // mark connections dirty
                    )
                )
            ).then(() =>
                // as the need and it's connections have been marked dirty
                // they will be reloaded on this action.
                fetchDataForOwnedNeeds([needUri])
                //fetchAllAccessibleAndRelevantData([needUri])
            ).then(allThatData =>
                dispatch({
                    type: actionTypes.messages.closeNeed.failed,
                    payload: allThatData
                })
            );
    }
}

        /*
         hasReceiverNeed: "https://192.168.124.53:8443/won/resource/need/1741189480636743700"
         hasSenderNeed: "https://192.168.124.53:8443/won/resource/need/1741189480636743700"
         has....Connection
         event.uri


         won.WONMSG.hasReceiverNeed = won.WONMSG.baseUri + "hasReceiverNeed";
         won.WONMSG.hasReceiverNeedCompacted = won.WONMSG.prefix + ":hasReceiverNeed";
         won.WONMSG.hasReceiver = won.WONMSG.baseUri + "hasReceiver"; // connection if connection event
         won.WONMSG.hasReceiverCompacted = won.WONMSG.prefix + ":hasReceiver";
         won.WONMSG.hasReceiverNode = won.WONMSG.baseUri + "hasReceiverNode";
         won.WONMSG.hasReceiverNodeCompacted = won.WONMSG.prefix + ":hasReceiverNode";
         won.WONMSG.hasSenderNeed = won.WONMSG.baseUri + "hasSenderNeed";
         won.WONMSG.hasSenderNeedCompacted = won.WONMSG.prefix + ":hasSenderNeed";
         won.WONMSG.hasSender = won.WONMSG.baseUri + "hasSender";
         won.WONMSG.hasSenderCompacted = won.WONMSG.prefix + ":hasSender";
         won.WONMSG.hasSenderNode = won.WONMSG.baseUri + "hasSenderNode";
         won.WONMSG.hasSenderNodeCompacted = won.WONMSG.prefix + ":hasSenderNode";
         */

export function successfulCloseConnection(event) {
    return (dispatch, getState) => {
        const state = getState();
        console.log("got response for CLOSE: " + event.getMessageType());
        let eventUri = null;
        let receiverUri = null;
        let isRemoteResponse = false;
        //TODO maybe refactor these response message handling
        if (state.getIn(['messages', 'waitingForAnswer', event.getIsResponseTo()])) {
            console.log("messages waitingForAnswer", event.getMessageUri());
            eventUri = event.getIsResponseTo();
            dispatch({
                type: actionTypes.messages.close.success,
                payload: event
            });
        } else if (state.getIn(['messages', 'waitingForAnswer', event.getIsRemoteResponseTo()])) {
            console.log("messages waitingForAnswer", event.getMessageUri());
            eventUri = event.getIsRemoteResponseTo();
            dispatch({
                type: actionTypes.messages.close.success,
                payload: event
            });
        } else {
            //when a connection is closed by the node (e.g. when you close/deactivate a need all its corresponding connections will be closed)
            dispatch({
                type: actionTypes.messages.close.success,
                payload: event
            })
        }
    }
}

//TODO move redirect elsewhere (e.g. to click-handler) then remove
export function successfulOpen(event){
    return (dispatch, getState) => {
        const state = getState();
        dispatch({
            type: actionTypes.messages.open.successOwn,
            payload: {
                event,
            }
        });

        dispatch(actionCreators.router__stateGoAbs("post", {
            postUri: event.getReceiverNeed(),
            connectionType: won.WON.Connected,
            connectionUri: event.getReceiver(),
        }));
    }
}

export function successfulCreate(event) {
    return (dispatch) => {
        //const state = getState();
        console.log("got response for CREATE: " + event.getMessageType());
        //TODO: if negative, use alternative need URI and send again
        //fetch need data and store in local RDF store
        //get URI of newly created need from message

        //load the data into the local rdf store and publish NeedCreatedEvent when done
        var needURI = event.getReceiverNeed();

        won.getNeed(needURI)
            .then((need) => {
                console.log("Dispatching action " + won.EVENT.NEED_CREATED);
                dispatch(actionCreators.needs__createSuccessful({
                    publishEventUri: event.getIsResponseTo(),
                    needUri: event.getSenderNeed(),
                    need: need,
                }));
            });
    }
}

export function openMessageReceived(event) {
    return dispatch => {
        won.invalidateCacheForNewMessage(event.getReceiver())
        .then(() =>
                getConnectionData(event))
        .then(data => {
                dispatch({
                    type: actionTypes.messages.openMessageReceived,
                    payload: data
                })
            }
        )
    }
}

export function connectMessageReceived(event) {
    return (dispatch, getState) => {

        const ownConnectionUri = event.getReceiver();
        const ownNeedUri = event.getReceiverNeed();
        const theirNeedUri = event.getSenderNeed();

        const state = getState();
        let connectionP;
        if(state.getIn(['connections', ownConnectionUri])) {
            // already in state. invalidate the version in the rdf-store.
            connectionP = Promise.resolve(state.getIn(['connections', ownConnectionUri]))
            won.invalidateCacheForNewConnection(ownConnectionUri, ownNeedUri);
        } else {
            // need to fetch
            connectionP = won
                .getConnectionWithEventUris(ownConnectionUri, { requesterWebId: ownNeedUri })
                .then(cnct => Immutable.fromJS(cnct));
        }

        Promise.all([
            connectionP,
            won.getNeed(theirNeedUri),
            won.getNeed(ownNeedUri), //uses ownNeed (but does not need connections uris to be loaded) in connectMessageReceived
        ])
        .then(([connection, theirNeed, ownNeed]) => {
            dispatch({
                type: actionTypes.messages.connectMessageReceived,
                payload: {
                    updatedConnection: ownConnectionUri,
                    connection: connection.set('hasConnectionState', won.WON.RequestReceived),
                    ownNeedUri: ownNeedUri,
                    ownNeed: ownNeed,
                    remoteNeed: theirNeed,
                    receivedEvent: event.getMessageUri(), // the more relevant event. used for unread-counter.
                    message: event,
                }
            });
        });

    }
}

/**
 * @deprecated due to the reason given in the TODO.
 * TODO this function indirectly fetches the entire
 * connection again! It should be enough to just
 * use the two events we get in most cases and make
 * the reducers correspondingly smarter.
 * @param eventOnRemote
 * @param eventOnOwn
 * @return {*}
 */
function getConnectionData(event) {
    return won
        .getConnectionWithOwnAndRemoteNeed(event.getReceiverNeed(),event.getSenderNeed())
        .then(connectionData =>
            getConnectionRelatedData(
                event.getReceiverNeed(),
                event.getSenderNeed(),
                connectionData.uri
            )
            .then(data => {
                if(data.events.filter(e => e.uri === event.getMessageUri()).length === 0) {
                    //
                    /*
                     * if data.events doesn't contain the arguments-events,
                     * add them. they might not be contained in the events-list
                     * due to a race condition, i.e. if data hasn't been
                     * stored on the node when the query resolves.
                     */
                    const eventOnOwn_ = clone(event.getFramedMessageResourceForState());
                    eventOnOwn_.hasCorrespondingRemoteMessage = clone(event.getFramedRemoteMessageResourceForState());
                    data.events.push( eventOnOwn_ );
                }

                data.receivedEvent = event.getMessageUri();
                data.updatedConnection = connectionData.uri;
                data.message = event;

                return data

            })
        )
}


export function needMessageReceived(event) {
    return (dispatch, getState) => {
        //first check if we really have the 'own' need in the state - otherwise we'll ignore the hint
        const need = getState().getIn(['needs', event.getReceiverNeed()]);
        if (!need) {
            console.log("ignoring needMessage for a need that is not ours:", event.getReceiverNeed());
        }
        dispatch({
            type: actionTypes.messages.needMessageReceived,
            payload: {
                needUri: event.getReceiverNeed(),
                needTitle: need.get("title"),
                message: event.getTextMessage(),
            }
        });
    }

}


export function hintMessageReceived(event) {
    return (dispatch, getState) => {

        //first check if we really have the 'own' need in the state - otherwise we'll ignore the hint
        if (!getState().getIn(['needs', event.getReceiverNeed()])) {
            console.log("ignoring hint for a need that is not ours:", event.getReceiverNeed());
        }

        //event.eventType = won.messageType2EventType[event.hasMessageType]; TODO needed?
        won.invalidateCacheForNewConnection(event.getReceiver(), event.getReceiverNeed())
            .then(() => {
                let needUri = event.getReceiverNeed();
                let match = {}
                //TODO: why do add the matchscore and counterpart when we don't use the event?
                
                event.matchScore = event.getMatchScore();
                event.matchCounterpartURI = event.getMatchCounterpart();

                console.log('going to crawl connection related data');//deletme

                getConnectionRelatedData(needUri, event.getMatchCounterpart(), event.getReceiver())
                .then(data => {
                        data.receivedEvent = event.getMessageUri();
                        data.updatedConnection = event.getReceiver();
                        dispatch({
                            type: actionTypes.messages.hintMessageReceived,
                            payload: data
                        });
                    }
                );

                // /add some properties to the eventData so as to make them easily accessible to consumers
                //of the hint event
                // below is commented as it seems to cause to hint event data loaded/displayed
                //if (eventData.matchCounterpartURI != null) {
                //    //load the data of the need the hint is about, if required
                //    //linkedDataService.ensureLoaded(eventData.uri);
                //    linkedDataService.ensureLoaded(eventData.matchCounterpartURI);
                //}

                console.log("handling hint message")
            });
    }
}


