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
    buildConnectMessage
} from '../won-message-utils';

export function messagesMessageReceived(data) {
    return dispatch=> {
        //TODO move this switch-case to the messaging agent
        console.log('messages__messageReceived: ', data)
        getEventData(data).then(event=> {
            console.log('messages__messageReceived: event.hasMessageType === ', event.hasMessageType)
            window.event4dbg = event;
            if (event.hasMessageType === won.WONMSG.successResponseCompacted) {
                dispatch(actionCreators.messages__successResponseMessageReceived(event))
            }
            else if (event.hasMessageType === won.WONMSG.hintMessageCompacted) {
                dispatch(actionCreators.messages__hintMessageReceived(event))
            }
            else if (event.hasMessageType === won.WONMSG.connectMessageCompacted) {
                dispatch(actionCreators.messages__connectMessageReceived(event))
            }
        })

    }
}

export function messagesSuccessResponseMessageReceived(event) {
    return (dispatch, getState) => {
        const state = getState()
        console.log('received response to ', event.isResponseTo, ' of ', event);

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
            console.log("got response for OPEN: " + event.hasMessageType)
            let eventUri = null;
            let isRemoteResponse = false;
            //TODO maybe refactor these response message handling
            if (state.getIn(['messages', 'waitingForAnswer', event.isRemoteResponseTo])) {
                eventUri = event.isRemoteResponseTo
                dispatch(actionCreators.messages__remoteResponseReceived(event.isRemoteResponseTo))

                //TODO: handle these cases
                //this.gotResponseFromRemoteNode = true;
            } else if (state.getIn(['messages', 'waitingForAnswer', event.isResponseTo])) {
                dispatch(actionCreators.messages__ownResponseReceived(event.isResponseTo))
                eventUri = event.isResponseTo
                //TODO: handle these cases
                //this.gotResponseFromOwnNode = true;
            }
            if (!isSuccessMessage(event)) {
                console.log(event)
            }

            if (state.getIn(['messages', 'waitingForAnswer', eventUri]).ownResponse === true && state.getIn(['messages', 'waitingForAnswer', eventUri]).remoteResponse === true) {
                won.invalidateCacheForNewMessage(event.hasReceiver).then(()=> {
                    getConnectionRelatedDataAndDispatch(event.hasReceiverNeed, event.hasSenderNeed, event.hasReceiver, dispatch).then(connectionData=> {
                        won.executeCrawlableQuery(
                            won.queries["getLastEventUriOfConnection"],
                            event.hasReceiver
                        ).then(lastEvent=> {
                                connectionData.lastEvent = lastEvent;
                                dispatch(actionCreators.messages__openResponseReceived({eventUri, connectionData}))
                            })

                    })
                })


            }
            /*                won.ensureLoaded(eventData.hasSender)
             .then(function(value){
             won.ensureLoaded(eventUri)
             })*/
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
