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
    fetchDataForOwnedNeeds,
	callAgreementsFetch,
} from '../won-message-utils.js';

import {
    makeParams,
    resetParams,
} from '../configRouting.js';

export function successfulCloseNeed(event) {
    return (dispatch, getState) => {
        //TODO maybe refactor these response message handling
        if (getState().getIn(['messages', 'waitingForAnswer', event.getIsRemoteResponseTo()])) {
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
        let eventUri = null;
        let receiverUri = null;
        let isRemoteResponse = false;
        //TODO maybe refactor these response message handling
        if (state.getIn(['messages', 'waitingForAnswer', event.getIsResponseTo()])) {
            eventUri = event.getIsResponseTo();
            dispatch({
                type: actionTypes.messages.close.success,
                payload: event
            });
        } else if (state.getIn(['messages', 'waitingForAnswer', event.getIsRemoteResponseTo()])) {
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

export function successfulCreate(event) {
    return (dispatch) => {
        //const state = getState();
        //TODO: if negative, use alternative need URI and send again
        //fetch need data and store in local RDF store
        //get URI of newly created need from message

        //load the data into the local rdf store and publish NeedCreatedEvent when done
        var needURI = event.getReceiverNeed();

        won.getNeed(needURI)
            .then((need) => {
                dispatch(actionCreators.needs__createSuccessful({
                    publishEventUri: event.getIsResponseTo(),
                    needUri: event.getSenderNeed(),
                    need: need,
                }));
            });
    }
}

export function openMessageReceived(event) {
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
	                type: actionTypes.messages.openMessageReceived,
	                payload: {
	                    updatedConnection: ownConnectionUri,
	                    connection:  connection,
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


export function connectionMessageReceived(event) {
	return (dispatch, getState) => {
		const connectionUri = event.getReceiver();
		const needUri = event.getReceiverNeed();
		const messages = getState().getIn(['needs', needUri, 'connections', connectionUri, 'messages']);
		const baseString = "/owner/";
		
     	var url = baseString + 'rest/agreement/getMessageEffects?connectionUri='+connectionUri+'&messageUri='+event.getMessageUri();
     	callAgreementsFetch(url)
 		.then(response => {
 			console.log("response : ", response);
 			
 			for(effect of response) {
 				console.log("effect : ", effect);
 				 switch (effect.type) {
                  	case "ACCEPTS":
                  		console.log("ACCEPTS");
                  		if(effect.accepts) {;
                  			let messageUri = getEventUri(messages, effect.acceptedMessageUri);
                  			dispatch({
                		        type: actionTypes.messages.markAsRelevant,
                		        payload: {
                		    			 messageUri: messageUri,
                		                 connectionUri: connectionUri,
                		                 needUri: needUri,
                		                 relevant: false,
                	 			}
                			});
                  		}
                  		break;
                  		
                  	case "PROPOSES":
                  		console.log("PROPOSES");
                  		break;
                  		
                  	case "REJECTS":
                  		console.log("REJECTS");
                     	if(effect.rejects) {
	              			let messageUri = getEventUri(messages, effect.rejectedMessageUri);
	              			dispatch({
	            		        type: actionTypes.messages.markAsRelevant,
	            		        payload: {
	            		    			 messageUri: messageUri,
	            		                 connectionUri: connectionUri,
	            		                 needUri: needUri,
	            		                 relevant: false,
	            	 			}
	            			});
	              		}
                  		break;

                     case "RETRACTS":
                     	console.log("RETRACTS");
                     	if(effect.retracts) {
	              			let messageUri = getEventUri(messages, effect.retractedMessageUri);
	              			dispatch({
	            		        type: actionTypes.messages.markAsRelevant,
	            		        payload: {
	            		    			 messageUri: messageUri,
	            		                 connectionUri: connectionUri,
	            		                 needUri: needUri,
	            		                 relevant: false,
	            	 			}
	            			});
	              		}
	              		break;

                     default:
                     	//return state;
                     	break;
 				 }
 			}
 			
 			dispatch({
                type: actionTypes.messages.connectionMessageReceived,
                payload: event,
            });
 		});
	 }
}

function getEventUri(messages, messageUri) {
	if(messageUri) {
		let uriSet = new Set();
		for(message of Array.from(messages)) {
			uriSet.add(message[0]);
		}
		if(!uriSet.has(messageUri)){
			for(msg of Array.from(messages)) {
				if(msg[1].get("remoteUri") === messageUri) {
					messageUri = msg[1].get("uri");
				}
			}
		}   		
	}
	return messageUri
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


export function markAsRelevant(event) {
	 return (dispatch, getState) => {

		 const messages = getState().getIn(["needs", event.needUri, "connections", event.connectionUri, "messages"]);
		 const messageUri = getEventUri(messages, event.messageUri);
		 
		 const payload = {
    			 messageUri: messageUri,
                 connectionUri: event.connectionUri,
                 needUri: event.needUri,
                 relevant: event.relevant,
        }

		 dispatch({
			 type: actionTypes.messages.markAsRelevant,
			 payload: payload,
		 });
	 }
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
        } else if(getState().getIn(['needs', event.getMatchCounterpart(), 'state']) === won.WON.InactiveCompacted) {
            console.log("ignoring hint for an inactive  need:", event.getMatchCounterpart());
        } else {
            //event.eventType = won.messageType2EventType[event.hasMessageType]; TODO needed?
            won.invalidateCacheForNewConnection(event.getReceiver(), event.getReceiverNeed())
                .then(() => {
                    let needUri = event.getReceiverNeed();
                    let match = {}
                    //TODO: why do add the matchscore and counterpart when we don't use the event?

                    event.matchScore = event.getMatchScore();
                    event.matchCounterpartURI = event.getMatchCounterpart();

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

                });
        }
    }
}

/**
 * Dispatches actions registered for the "successOwn" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "failureOwn" event
 */
export function dispatchActionOnSuccessOwn(event) {
    return (dispatch, getState) => {
		const toDispatchList = getState().getIn(['messages','dispatchOnSuccessOwn', event.getIsResponseTo()]);
		if (toDispatchList){
			toDispatchList.forEach( d => {
				dispatch(d)
			})
		}
		//the reducer will delete the toDispatchList for successOwn and failureOwn
		dispatch({
			type: actionTypes.messages.dispatchActionOn.successOwn,
			payload: {
				eventUri:  event.getIsResponseTo(),
			}
		})
    }
}

/**
 * Dispatches actions registered for the "failureOwn" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "successOwn" event
 */
export function dispatchActionOnFailureOwn(event) {
    return (dispatch, getState) => {
		const toDispatchList = getState().getIn(['messages','dispatchOnFailureOwn', event.getIsResponseTo()]);
		if (toDispatchList){
			toDispatchList.forEach( d => {
				dispatch(d)
			})
		}
		//the reducer will delete the toDispatchList for successOwn and failureOwn
		dispatch({
			type: actionTypes.messages.dispatchActionOn.failureOwn,
			payload: {
				eventUri:  event.getIsResponseTo(),
			}
		})
    }
}

/**
 * Dispatches actions registered for the "successRemote" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "failureRemote" event
 */
export function dispatchActionOnSuccessRemote(event) {
    return (dispatch, getState) => {
		const toDispatchList = getState().getIn(['messages','dispatchOnSuccessRemote', event.getIsRemoteResponseTo()]);
		if (toDispatchList){
			toDispatchList.forEach( d => {
                if (d.type){
                    dispatch(d);
                } else {
                    // if an adHocConnection was successfully created, go to the correct connectionUri
                    if (d.connectionUri === "responseEvent::receiverUri"){
                        dispatch(actionCreators.router__stateGoCurrent({
                            connectionUri: event.getReceiver(),
                        }));
                    }
                }
			})
		}
		//the reducer will delete the toDispatchList for successOwn and failureOwn
		dispatch({
			type: actionTypes.messages.dispatchActionOn.successRemote,
			payload: {
				eventUri:  event.getIsRemoteResponseTo(),
			}
		})
    }
}

/**
 * Dispatches actions registered for the "failureRemote" event for the specified message uri.
 * The corresponding reducer clears any registered actions for the "successRemote" event
 */
export function dispatchActionOnFailureRemote(event) {
    return (dispatch, getState) => {
		const toDispatchList = getState().getIn(['messages','dispatchOnFailureRemote', event.getIsRemoteResponseTo()]);
		if (toDispatchList){
			toDispatchList.forEach( d => {
				dispatch(d)
			})
		}
		//the reducer will delete the toDispatchList for successOwn and failureOwn
		dispatch({
			type: actionTypes.messages.dispatchActionOn.failureRemote,
			payload: {
				eventUri:  event.getIsRemoteResponseTo(),
			}
		})
    }
}


