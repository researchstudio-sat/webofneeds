/**
 * Created by ksinger on 19.02.2016.
 */


import  won from '../won-es6';
import { actionTypes, actionCreators, getConnectionRelatedData, messageTypeToEventType } from './actions';
import { getEventsFromMessage,setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils';

import Immutable from 'immutable';

import {
    checkHttpStatus,
} from '../utils';

import {
    buildCreateMessage,
    buildOpenMessage,
    buildCloseMessage,
    buildRateMessage,
    buildConnectMessage,
    isSuccessMessage,
    fetchAllAccessibleAndRelevantData
} from '../won-message-utils';

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
                fetchAllAccessibleAndRelevantData([needUri])
            ).then(allThatData =>
                dispatch({
                    type: actionTypes.messages.closeNeed.failed,
                    payload: Immutable.fromJS(allThatData)
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

export function successfulClose(event) {
    return (dispatch, getState) => {
        const state = getState();
        console.log("got response for CLOSE: " + event.hasMessageType);
        let eventUri = null;
        let receiverUri = null;
        let isRemoteResponse = false;
        //TODO maybe refactor these response message handling
        if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
            console.log("messages waitingForAnswer", event);
            eventUri = event.isRemoteResponseTo;
            dispatch({
                type: actionTypes.messages.close.success,
                payload: event
            });
        }
    }
}

export function successfulConnect(event) {
    return (dispatch, getState) => {
        const state = getState();
        console.log("got response for CONNECT: " + event.hasMessageType);
        let eventUri = null;
        let receiverUri = null;
        let isRemoteResponse = false;
        //TODO maybe refactor these response message handling
        if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
            console.log("messages waitingForAnswer", event);
            eventUri = event.isRemoteResponseTo;
            dispatch({
                type: actionTypes.messages.connect.success,
                payload: event
            });
        }
    }
}

export function successfulOpen(event){
    return (dispatch, getState) => {
        const state = getState();
        console.log("got response for OPEN: " + event.hasMessageType);
        let eventUri = null;
        let receiverUri = null;
        let isRemoteResponse = false;
        //TODO maybe refactor these response message handling
        if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
            console.log("messages waitingForAnswer", event);
            eventUri = event.isRemoteResponseTo;
            dispatch(actionCreators.connections__accepted(event));
        }
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
                won.getNeed(needURI).then((need) => {
                    console.log("Dispatching action " + won.EVENT.NEED_CREATED);
                    dispatch(actionCreators.drafts__publishSuccessful({
                        publishEventUri: event.isResponseTo,
                        needUri: event.hasSenderNeed,
                        eventData: eventData
                    }));
                    dispatch(actionCreators.needs__received(need));
                });
            });

        // dispatch routing change
        //TODO back-button doesn't work for returning to the draft
        //TODO instead of going to the feed, this should go back to where the user was before starting the creation process.
        dispatch(actionCreators.router__stateGo('feed'));

        //TODO add to own needs
        //  linkeddataservice.crawl(event.hasSenderNeed) //agents shouldn't directyl communicate with each other, should they?
    }
}

export function connectionMessageReceived(events) {
    return dispatch => {
        const eventOnRemote = events['msg:FromOwner'];
        const eventOnOwn = events['msg:FromExternal'];
        eventOnRemote.eventType = messageTypeToEventType[eventOnRemote.hasMessageType].eventType;
        //TODO data.hasReceiver, the connectionUri is undefined in the response message
        won.invalidateCacheForNewMessage(eventOnOwn.hasReceiver)
            .then(() => {
                won.getConnectionWithOwnAndRemoteNeed(eventOnRemote.hasReceiverNeed, eventOnRemote.hasSenderNeed).then(connectionData=> {
                    //TODO refactor
                    getConnectionRelatedData(eventOnRemote.hasReceiverNeed, eventOnRemote.hasSenderNeed, connectionData.uri)
                        .then(data => {
                            //making sure it's the same thing. trying to approach a point,
                            // where this is *just* a uri and everythinng else is in the state.
                            data.receivedEvent = eventOnOwn.uri;
                            data.updatedConnection = connectionData.uri;
                            dispatch({
                                type: actionTypes.messages.connectionMessageReceived,
                                payload: data
                            });
                        }
                    );
                })

            })
    }
}

export function connectMessageReceived(events) {
    //return reloadConnectionAndDispatch(events, actionTypes.messages.connectMessageReceived);
    const actionType = actionTypes.messages.connectMessageReceived;
    return dispatch => {
        const eventOnRemote = events['msg:FromOwner'];
        const eventOnOwn = events['msg:FromExternal'];
        eventOnRemote.eventType = messageTypeToEventType[eventOnRemote.hasMessageType].eventType;
        won.invalidateCacheForNewConnection(eventOnOwn.hasReceiver, eventOnRemote.hasReceiverNeed)
        .then(() => {
            won.getConnectionWithOwnAndRemoteNeed(eventOnRemote.hasReceiverNeed, eventOnRemote.hasSenderNeed).then(connectionData=> {
                //TODO refactor
                getConnectionRelatedData(eventOnRemote.hasReceiverNeed, eventOnRemote.hasSenderNeed, connectionData.uri)
                    .then(data => {
                        //making sure it's the same thing. trying to approach a point,
                        // where this is *just* a uri and everythinng else is in the state.
                        data.receivedEvent = eventOnOwn.uri;
                        data.updatedConnection = connectionData.uri;
                        won.getNode(data.receivedEvent, data.ownNeed.uri).then(x =>
                            console.log('aaaaaaaa ', x));
                        dispatch({
                            type: actionTypes.messages.connectMessageReceived,
                            payload: data
                        });
                    }
                );
            })

        })
    }

}



export function hintMessageReceived(event) {
    return dispatch=> {
        event.eventType = messageTypeToEventType[event.hasMessageType].eventType;
        won.invalidateCacheForNewConnection(event.hasReceiver, event.hasReceiverNeed)
            .then(() => {
                let needUri = event.hasReceiverNeed;
                let match = {}

                event.unreadUri = event.hasReceiver;//TODO should be deprecated (as it's set in the event-reducer)
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
