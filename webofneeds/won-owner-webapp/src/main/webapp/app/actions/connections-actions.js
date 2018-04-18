/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6.js';
import Immutable from 'immutable';
import jsonld from 'jsonld'; //import *after* the rdfstore to shadow its custom jsonld

import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
    selectOpenPostUri,
    selectRemoteEvents,
    selectConnection,
} from '../selectors.js';

import {
    is,
    urisToLookupMap,
    getIn,
    get,
    jsonld2simpleFormat,
    cloneAsMutable,
    delay,
} from '../utils.js';

import {
   makeParams,
} from '../configRouting.js';

import {
    ensureLoggedIn,
} from './account-actions';

import {
    actionTypes,
    actionCreators,
} from './actions.js';

import {
    buildCreateMessage,
    buildOpenMessage,
    buildCloseMessage,
    buildChatMessage,
    buildRateMessage,
    buildConnectMessage,
    buildAdHocConnectMessage,
} from '../won-message-utils.js';

export function connectionsChatMessage(chatMessage, connectionUri, isTTL=false) {
   return (dispatch, getState) => {

       const ownNeed = getState().get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
       const theirNeedUri = getState().getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
       const theirNeed = getState().getIn(["needs", theirNeedUri]);
       const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

       buildChatMessage({
            chatMessage: chatMessage, 
            connectionUri,
            ownNeedUri: ownNeed.get("uri"), 
            theirNeedUri: theirNeedUri,
            ownNodeUri: ownNeed.get("nodeUri"), 
            theirNodeUri: theirNeed.get("nodeUri"), 
            theirConnectionUri,
            isTTL,
        })
       .then(msgData =>
            Promise.all([won.wonMessageFromJsonLd(msgData.message), msgData.message]))
       .then(([optimisticEvent, jsonldMessage]) => {
           // dispatch(actionCreators.messages__send(messageData));
           dispatch({
               type: actionTypes.connections.sendChatMessage,
               payload: {
                   eventUri: optimisticEvent.getMessageUri(),
                   message: jsonldMessage,
                   optimisticEvent,
                }
            });
       })
       .catch(e => {
           console.error('Error while processing chat message: ', e);
           dispatch({
               type: actionTypes.connections.sendChatMessageFailed,
               payload: {
                   error: e,
                   message: e.message,
                }
            });
       });
   }
}

export function connectionsFetch(data) {
    return dispatch=> {
        const allConnectionsPromise = won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], data.needUri);
        allConnectionsPromise.then(function (connections) {
            dispatch(actionCreators.needs__connectionsReceived({needUri: data.needUri, connections: connections}));
        })
    }
}


export function connectionsOpen(connectionUri, textMessage) {	
    return async (dispatch, getState) => {
    	 const state = getState();
         const ownNeed = getState().get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
         const theirNeedUri = getState().getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
         const theirNeed = getState().getIn(["needs", theirNeedUri]);
         const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

         const openMsg = await buildOpenMessage(connectionUri, ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), theirConnectionUri, textMessage);
         
         const optimisticEvent = await won.wonMessageFromJsonLd(openMsg.message);

         dispatch({
             type: actionTypes.connections.open,
             payload: {
                 connectionUri,
                 textMessage,
                 eventUri: openMsg.eventUri,
                 message: openMsg.message,
                 optimisticEvent,
             }
         });
         
        dispatch(actionCreators.router__stateGoCurrent({
            connectionUri: optimisticEvent.getSender(),
        }));
    }
}


export function connectionsConnectAdHoc(theirNeedUri, textMessage) {
    return (dispatch, getState) => connectAdHoc(theirNeedUri, textMessage, dispatch, getState) // moved to separate function to make transpilation work properly
}
async function connectAdHoc(theirNeedUri, textMessage, dispatch, getState) {
    await ensureLoggedIn(dispatch, getState);
	const state = getState();
    const theirNeed = getIn(state, ['needs', theirNeedUri]);
    const adHocDraft = generateResponseNeedTo(theirNeed);
    const nodeUri = getIn(state, ['config', 'defaultNodeUri']);
    const { message, eventUri, needUri } = buildCreateMessage(adHocDraft, nodeUri);
    const cnctMsg = buildConnectMessage({
        ownNeedUri: needUri,
        theirNeedUri: theirNeedUri,
        ownNodeUri: nodeUri,
        theirNodeUri: theirNeed.get("nodeUri"),
        textMessage: textMessage,
    });
    
    const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
    
    // connect action to be dispatched when the 
    // ad hoc need has been created: 
    const connectAction = {
		type: actionTypes.needs.connect, 
		payload: {
            eventUri: cnctMsg.eventUri,
            message: cnctMsg.message,
            optimisticEvent: optimisticEvent,
        }
    }
    
    // register the connect action to be dispatched when 
    // need creation is successful
    dispatch({
    	type: actionTypes.messages.dispatchActionOn.registerSuccessOwn,
    	payload: {
    		eventUri: eventUri,
    		actionToDispatch: connectAction,
    	}
    })
    
    // create the new need
    dispatch({
        type: actionTypes.needs.create, // TODO custom action
        payload: {eventUri, message, needUri, need: adHocDraft}
    });

    dispatch(actionCreators.router__stateGoAbs('feed'));
}

async function messageGraphToEvent(eventUri, messageGraph) {

    const framed = await jsonld.promises.frame(
        messageGraph,
        {
            '@id': eventUri,
            '@context': messageGraph['@context']
        }
    )

    let event = getIn(framed, ['@graph', 0]);
    if(event) {
        event['@context'] = framed['@context']; // context is needed by jsonld2simpleFormat for expanding prefixes in values
        event = jsonld2simpleFormat(event);
    }

    return event;
}


function generateResponseNeedTo(theirNeed) {
    const theirSeeks = get(theirNeed, 'seeks');
    const theirIs = get(theirNeed, 'is');
    return {
        is: theirSeeks? generateResponseContentNodeTo(theirSeeks) : undefined,
        seeks: theirIs? generateResponseContentNodeTo(theirIs) : undefined,
    };
}

function generateResponseContentNodeTo(contentNode) {
    const theirTitle = get(contentNode, 'title');
    return {
        title: 'Re: ' + theirTitle,
        description: 'Direct response to : ' + theirTitle,
        //type: reNeedType,
        tags: cloneAsMutable(get(contentNode, 'tags')),
        location: cloneAsMutable(get(contentNode, 'location')),
        noHints: true,
    };
}

export function connectionsClose(connectionUri) {
    return (dispatch, getState) => {

        const ownNeed = getState().get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
        const theirNeedUri = getState().getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
        const theirNeed = getState().getIn(["needs", theirNeedUri]);
        const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

        buildCloseMessage(connectionUri, ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), theirConnectionUri)
        .then(closeMessage => {
            dispatch(actionCreators.messages__send({
                eventUri: closeMessage.eventUri,
                message: closeMessage.message
            }));
            dispatch({
                type: actionTypes.connections.close,
                payload: {connectionUri}
            })
        });
    }
}

export function connectionsCloseRemote(message){
    //Closes the 'remoteConnection' again, if closeConnections(...) only closes the 'own' connection
    return (dispatch, getState) => {
        const connectionUri = message.getSender();
        const remoteNeedUri = message.getSenderNeed();
        const remoteNode = message.getSenderNode();
        const ownNeedUri = message.getReceiverNeed();
        const ownNode = message.getReceiverNode();

        buildCloseMessage(connectionUri, remoteNeedUri, ownNeedUri, ownNode, remoteNode, null)
            .then(closeMessage => {
                dispatch(actionCreators.messages__send({
                    eventUri: closeMessage.eventUri,
                    message: closeMessage.message
                }));
            });
    }
}

export function connectionsRate(connectionUri,rating) {
    return (dispatch, getState) => {

        const state = getState();
        let messageData = null;

        won.getConnectionWithEventUris(connectionUri)
            .then(connection=> {
                let msgToRateFor = {connection: connection};

                const ownNeed = state.get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
                const theirNeedUri = state.getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
                const theirNeed = state.getIn(["needs", theirNeedUri]);
                const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

                return buildRateMessage(msgToRateFor, ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), theirConnectionUri, rating);
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
export function showLatestMessages(connectionUriParam, numberOfEvents){
    return (dispatch, getState) => {
        const state = getState();
        const connectionUri = connectionUriParam || selectOpenConnectionUri(state);
        const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
        const needUri = need && need.get("uri");
        const connection = connectionUri && selectConnection(state, connectionUri);
        if (!connectionUri || !connection) return;

        const connectionMessages = connection.get('messages');
        if (connection.get('loadingEvents') || !connectionMessages || connectionMessages.size > 0) return; // only start loading once. //TODO: PENDING IS CURRENTLY NOT IMPLEMENTED IN THE NEW STATE

        dispatch({
            type: actionTypes.connections.showLatestMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });


        won.getWonMessagesOfConnection(
            connectionUri,
            {
                requesterWebId: needUri,
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
/*
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
*/


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
export function showMoreMessages(connectionUriParam, numberOfEvents) {
    return (dispatch, getState) => {
        const state = getState();
        const connectionUri = connectionUriParam || selectOpenConnectionUri(state);
        const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
        const needUri = need && need.get("uri");
        const events = state.getIn(["needs", needUri, "connections", connectionUri, "messages"]) || Immutable.List();

        // determine the oldest loaded event
        const sortedOwnEvents = events.valueSeq().sort( (event1, event2) => event1.get('date') - event2.get('date'));
        const oldestEvent = sortedOwnEvents.first();

        const eventHashValue = oldestEvent && oldestEvent
                .get('uri')
                .replace(/.*\/event\/(.*)/, '$1'); // everything following the `/event/`
        dispatch({
            type: actionTypes.connections.showMoreMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });

        won.getWonMessagesOfConnection(
            connectionUri,
            {
                requesterWebId: needUri,
                pagingSize: numOfEvts2pageSize(numberOfEvents),
                deep: true,
                resumebefore: eventHashValue,
            }
        ).then(events =>
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
