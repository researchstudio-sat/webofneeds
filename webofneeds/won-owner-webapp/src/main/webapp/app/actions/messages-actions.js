/**
 * Created by ksinger on 19.02.2016.
 */


import  won from '../won-es6.js';
import { actionTypes, actionCreators, getConnectionRelatedData } from './actions.js';
import { setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils.js';

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
        console.log("got response for DEACTIVATE: " + event.hasMessageType);
        //TODO maybe refactor these response message handling
        if (getState().getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
            console.log("messages waitingForAnswer", event);
            //dispatch(actionCreators.connections__denied(event));
        }
    }
}
export function failedCloseNeed(event) {
    return (dispatch, getState) => {
        const needUri = event.hasReceiverNeed;
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
        console.log("got response for CLOSE: " + event.hasMessageType);
        let eventUri = null;
        let receiverUri = null;
        let isRemoteResponse = false;
        //TODO maybe refactor these response message handling
        if (state.getIn(['messages', 'waitingForAnswer', event.isResponseTo])) {
            console.log("messages waitingForAnswer", event);
            eventUri = event.isResponseTo;
            dispatch({
                type: actionTypes.messages.close.success,
                payload: event
            });
        } else if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
            console.log("messages waitingForAnswer", event);
            eventUri = event.isRemoteResponseTo;
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
export function successfulOpen({ events }){
    return (dispatch, getState) => {
        const state = getState();
        const event = events['msg:FromSystem'];
        dispatch({
            type: actionTypes.messages.open.successOwn,
            payload: {
                events,
            }
        });

        dispatch(actionCreators.router__stateGoAbs("post", {
            postUri: event.hasReceiverNeed,
            connectionType: won.WON.Connected,
            connectionUri: event.hasReceiver,
        }));
    }
}

export function successfulCreate(event) {
    return (dispatch) => {
        //const state = getState();
        console.log("got response for CREATE: " + event.hasMessageType);
        //TODO: if negative, use alternative need URI and send again
        //fetch need data and store in local RDF store
        //get URI of newly created need from message

        //load the data into the local rdf store and publish NeedCreatedEvent when done
        var needURI = event.hasReceiverNeed;
        won.ensureLoaded(needURI)
            .then(() => {
                var eventData = won.clone(event);
                eventData.eventType = won.EVENT.NEED_CREATED;
                setCommStateFromResponseForLocalNeedMessage(eventData);
                eventData.needURI = needURI;
                won.getNeedWithConnectionUris(needURI).then((need) => {
                    console.log("Dispatching action " + won.EVENT.NEED_CREATED);
                    dispatch(actionCreators.needs__createSuccessful({
                        publishEventUri: event.isResponseTo,
                        needUri: event.hasSenderNeed,
                        eventData: eventData,
                        need: need,
                    }));
                });
            });

        // dispatch routing change
        //TODO back-button doesn't work for returning to the draft
        //TODO instead of going to the feed, this should go back to where the user was before starting the creation process.
        dispatch(actionCreators.router__stateGoResetParams('feed'));

        //TODO add to own needs
        //  linkeddataservice.crawl(event.hasSenderNeed) //agents shouldn't directyl communicate with each other, should they?
    }
}

export function openMessageReceived(events) {
    return dispatch => {
        const eventOnRemote = events['msg:FromOwner']; // from the other person's owner application / node
        const eventOnOwn = events['msg:FromExternal']; // generated on our node
        eventOnRemote.eventType = won.messageType2EventType[eventOnRemote.hasMessageType];
        won.invalidateCacheForNewMessage(eventOnOwn.hasReceiver || eventOnRemote.hasReceiver)
        .then(() =>
                getConnectionData(eventOnRemote, eventOnOwn))
        .then(data => {
                dispatch({
                    type: actionTypes.messages.openMessageReceived,
                    payload: data
                })
            }
        )
    }
}

export function connectMessageReceived(events) {
    return (dispatch, getState) => {

        // from the other person's owner application / node
        const eventOnRemote = jsonld2simpleFormat(getIn(events, ['msg:FromOwner', 'framedMessage']));

        // generated on our node
        const eventOnOwn = jsonld2simpleFormat(getIn(events, ['msg:FromExternal', 'framedMessage']));

        eventOnRemote.eventType = won.messageType2EventType[eventOnRemote.hasMessageType];

        const ownConnectionUri = eventOnOwn.hasReceiver || eventOnRemote.hasReceiver;
        const ownNeedUri = eventOnOwn.hasReceiverNeed || eventOnRemote.hasReceiverNeed;
        const theirNeedUri = eventOnOwn.hasSenderNeed || eventOnRemote.hasSenderNeed;

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
            won.getTheirNeed(theirNeedUri),
            won.getOwnNeed(ownNeedUri),
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
                    receivedEvent: eventOnOwn.uri, // the more relevant event. used for unread-counter.
                    events: [
                        eventOnOwn,
                        eventOnRemote,
                    ],
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
function getConnectionData(eventOnRemote, eventOnOwn) {
    return won
        .getConnectionWithOwnAndRemoteNeed(eventOnRemote.hasReceiverNeed, eventOnRemote.hasSenderNeed)
        .then(connectionData =>
            getConnectionRelatedData(
                eventOnRemote.hasReceiverNeed,
                eventOnRemote.hasSenderNeed,
                connectionData.uri
            )
            .then(data => {

                if(data.events.filter(e => e.uri === eventOnOwn.uri).length === 0) {
                    //
                    /*
                     * if data.events doesn't contain the arguments-events,
                     * add them. they might not be contained in the events-list
                     * due to a race condition, i.e. if data hasn't been
                     * stored on the node when the query resolves.
                     */
                    const eventOnOwn_ = clone(eventOnOwn);
                    eventOnOwn_.hasCorrespondingRemoteMessage = clone(eventOnRemote);
                    data.events.push( eventOnOwn_ );
                }

                data.receivedEvent = eventOnOwn.uri;
                data.updatedConnection = connectionData.uri;

                return data

            })
        )
}

export function hintMessageReceived(event) {
    return dispatch=> {
        event.eventType = won.messageType2EventType[event.hasMessageType];
        won.invalidateCacheForNewConnection(event.hasReceiver, event.hasReceiverNeed)
            .then(() => {
                let needUri = event.hasReceiverNeed;
                let match = {}

                event.matchScore = event.framedMessage[won.WON.hasMatchScoreCompacted];
                event.matchCounterpartURI = won.getSafeJsonLdValue(event.framedMessage[won.WON.hasMatchCounterpart]);

                console.log('going to crawl connection related data');//deletme

                getConnectionRelatedData(needUri, event.hasMatchCounterpart, event.hasReceiver)
                .then(data => {
                        data.receivedEvent = event.uri;
                        data.updatedConnection = event.hasReceiver;
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


