/**
 * Created by ksinger on 19.02.2016.
 */


import  won from '../won-es6';
import { actionTypes, actionCreators, getConnectionRelatedData, messageTypeToEventType } from './actions';
import { getEventData,setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils';

import {
    checkHttpStatus,
} from '../utils';

import {
    buildCreateMessage,
    buildOpenMessage,
    buildCloseMessage,
    buildRateMessage,
    buildConnectMessage,
    isSuccessMessage
} from '../won-message-utils';

export function messagesSuccessResponseMessageReceived(event) {
    return (dispatch, getState) => {
        const state = getState()
        console.log('received response to ', event.isResponseTo, ' of ', event);
        console.log("responseType",event.isResponseToMessageType);
        //TODO do all of this in actions.js?
        if (event.isResponseToMessageType === won.WONMSG.createMessageCompacted) {
            console.log("got response for CREATE: " + event.hasMessageType);
            //TODO: if negative, use alternative need URI and send again
            //fetch need data and store in local RDF store
            //get URI of newly created need from message

            //load the data into the local rdf store and publish NeedCreatedEvent when done
            var needURI = event.hasReceiverNeed;
            won.ensureLoaded(needURI)
                .then(
                function (value) {
                    var eventData = won.clone(event);
                    eventData.eventType = won.EVENT.NEED_CREATED;
                    setCommStateFromResponseForLocalNeedMessage(eventData);
                    eventData.needURI = needURI;
                    won.getNeed(needURI)
                        .then(function (need) {

                            console.log("Dispatching action " + won.EVENT.NEED_CREATED);
                            dispatch(actionCreators.drafts__publishSuccessful({
                                publishEventUri: event.isResponseTo,
                                needUri: event.hasSenderNeed,
                                eventData: eventData
                            }));
                            dispatch(actionCreators.needs__received(need));
                            //deferred.resolve(needURI);
                        });
                });

            // dispatch routing change
            //TODO back-button doesn't work for returning to the draft
            //TODO instead of going to the feed, this should go back to where the user was before starting the creation process.
            dispatch(actionCreators.router__stateGo('feed'));

            //TODO add to own needs
            //  linkeddataservice.crawl(event.hasSenderNeed) //agents shouldn't directyl communicate with each other, should they?

        } else if (event.isResponseToMessageType === won.WONMSG.openMessageCompacted) {
            console.log("got response for OPEN: " + event.hasMessageType);
            let eventUri = null;
            let receiverUri = null;
            let isRemoteResponse = false;
            //TODO maybe refactor these response message handling
            if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
                console.log("messages waitingForAnswer",event);
                eventUri = event.isRemoteResponseTo;
                dispatch(actionCreators.connections__accepted(event));
            }

            if (!isSuccessMessage(event)) {
                console.log(event)
            }
        } else if (event.isResponseToMessageType === won.WONMSG.closeMessageCompacted) {
            console.log("got response for CLOSE: " + event.hasMessageType);
            let eventUri = null;
            let receiverUri = null;
            let isRemoteResponse = false;
            //TODO maybe refactor these response message handling
            if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
                console.log("messages waitingForAnswer",event);
                eventUri = event.isRemoteResponseTo;
                dispatch(actionCreators.connections__denied(event));
            }

            if (!isSuccessMessage(event)) {
                console.log(event)
            }
        }
    }
}

export function messagesConnectMessageReceived(data) {
    return dispatch=> {
        data.eventType = messageTypeToEventType[data.hasMessageType].eventType;
        //TODO data.hasReceiver, the connectionUri is undefined in the response message
        won.invalidateCacheForNewConnection(data.hasReceiver, data.hasReceiverNeed)
            .then(() => {
                won.getConnectionWithOwnAndRemoteNeed(data.hasReceiverNeed, data.hasSenderNeed).then(connectionData=> {
                    //TODO refactor
                    data.unreadUri = connectionData.uri;
                    dispatch(actionCreators.events__addUnreadEventUri(data));

                    getConnectionRelatedData(data.hasReceiverNeed, data.hasSenderNeed, connectionData.uri)
                        .then(data => dispatch({
                            type: actionTypes.messages.connectMessageReceived,
                            payload: data
                        }));
                })

            })
    }
}

export function messagesHintMessageReceived(data) {
    return dispatch=> {
        data.eventType = messageTypeToEventType[data.hasMessageType].eventType;
        won.invalidateCacheForNewConnection(data.hasReceiver, data.hasReceiverNeed)
            .then(() => {
                let needUri = data.hasReceiverNeed;
                let match = {}

                data.unreadUri = data.hasReceiver;
                data.matchScore = data.framedMessage[won.WON.hasMatchScoreCompacted];
                data.matchCounterpartURI = won.getSafeJsonLdValue(data.framedMessage[won.WON.hasMatchCounterpart]);

                dispatch(actionCreators.events__addUnreadEventUri(data))

                getConnectionRelatedData(needUri, data.hasMatchCounterpart, data.hasReceiver)
                    .then(data => dispatch({
                        type: actionTypes.messages.hintMessageReceived,
                        payload: data
                    }));


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
