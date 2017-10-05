/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6.js';
import Immutable from 'immutable';
import jsonld from 'jsonld'; //import *after* the rdfstore to shadow its custom jsonld

import {
    selectOpenConnectionUri,
    selectOpenPostUri,
    selectRemoteEvents,
    selectConnection,
} from '../selectors.js';

import {
    is,
    urisToLookupMap,
    msStringToDate,
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

export function connectionsChatMessage(chatMessage, connectionUri) {
   return (dispatch, getState) => {
       console.log('connectionsChatMessage: ', chatMessage, connectionUri);

       const ownNeed = getState().get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
       const theirNeedUri = getState().getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
       const theirNeed = getState().getIn(["needs", theirNeedUri]);
       const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

       buildChatMessage(chatMessage, connectionUri, ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), theirConnectionUri)
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
       });
   }
}

export function connectionsFetch(data) {
    return dispatch=> {
        const allConnectionsPromise = won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], data.needUri);
        allConnectionsPromise.then(function (connections) {
            console.log("fetching connections");
            dispatch(actionCreators.needs__connectionsReceived({needUri: data.needUri, connections: connections}));
        })
    }
}

export function connectionsOpen(connectionUri, message) {
    return (dispatch, getState) => {
        const ownNeed = getState().get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
        const theirNeedUri = getState().getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
        const theirNeed = getState().getIn(["needs", theirNeedUri]);
        const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

        buildOpenMessage(connectionUri, ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), theirConnectionUri, message)
        .then(msgData =>
            Promise.all([won.wonMessageFromJsonLd(msgData.message), msgData.message]))
        .then(([optimisticEvent, jsonldMessage]) => {
            // dispatch(actionCreators.messages__send(messageData));
            dispatch({
                type: actionTypes.connections.open,
                payload: {
                    eventUri: optimisticEvent.getMessageUri(),
                    message: jsonldMessage,
                    optimisticEvent,
                }
            });

            dispatch(actionCreators.router__stateGoAbs("post", {
                postUri: optimisticEvent.getSenderNeed(),
                connectionType: won.WON.Connected,
                connectionUri: optimisticEvent.getSender(),
            }));

        });
    }
}



export function connectionsConnect(connectionUri, textMessage) {
    return async (dispatch, getState) => {
        const state = getState();

        const ownNeed = getState().get("needs").filter(need => need.getIn(["connections", connectionUri])).first();
        const theirNeedUri = getState().getIn(["needs", ownNeed.get("uri"), "connections", connectionUri, "remoteNeedUri"]);
        const theirNeed = getState().getIn(["needs", theirNeedUri]);
        const theirConnectionUri = ownNeed.getIn(["connections", connectionUri, "remoteConnectionUri"]);

        const cnctMsg = await buildConnectMessage(connectionUri, ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), theirConnectionUri, textMessage);

        dispatch(actionCreators.messages__send({eventUri: cnctMsg.eventUri, message: cnctMsg.message}));

        const event = await messageGraphToEvent(cnctMsg.eventUri, cnctMsg.message);

        dispatch({
            type: actionTypes.connections.connect,
            payload: {
                connectionUri,
                textMessage,
                eventUri: cnctMsg.eventUri,
                optimisticEvent: event,
            }
        });
    }
}


export function connectionsConnectAdHoc(theirNeedUri, textMessage) {
    return (dispatch, getState) => connectAdHoc(theirNeedUri, textMessage, dispatch, getState) // moved to separate function to make transpilation work properly
}
async function connectAdHoc(theirNeedUri, textMessage, dispatch, getState) {
    const state = getState();

    const theirNeed = getIn(state, ['needs', theirNeedUri]);
    const adHocDraft = generateResponseNeedTo(theirNeed);
    const nodeUri = getIn(state, ['config', 'defaultNodeUri']);

    await ensureLoggedIn(dispatch, getState);

    const { message, eventUri, needUri } = buildCreateMessage(adHocDraft, nodeUri);
    dispatch({
        type: actionTypes.needs.create, // TODO custom action
        payload: {eventUri, message, needUri}
    });

    console.log('STARTED PUBLISHING AD HOC DRAFT: ', adHocDraft);

    //TODO wait for success-response instead
    await delay(1000); // to give the server enough time to handle need creation.

    // TODO handle failure to post need (the needUri won't be valid)

    const cnctMsg = await buildAdHocConnectMessage(needUri, theirNeedUri, nodeUri, theirNeed.get("nodeUri"), textMessage);

    const event = await messageGraphToEvent(cnctMsg.eventUri, cnctMsg.message);

    dispatch({
        type: actionTypes.connections.connectAdHoc,
        payload: {
            //connectionUri,
            textMessage,
            eventUri: cnctMsg.eventUri,
            optimisticEvent: event,
        }
    });

    dispatch(actionCreators.messages__send({eventUri: cnctMsg.eventUri, message: cnctMsg.message}));

    await won.invalidateCacheForNewConnection(undefined /* we don't have a cnct uri yet */, needUri); // mark connections dirty

    console.log('STARTED AD-HOC CONNECTING: ', cnctMsg);

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
    let reNeedType, descriptionPhrase;
    const theirNeedType = get(theirNeed, 'type');
    if(theirNeedType === won.WON.BasicNeedTypeDemandCompacted) {
        reNeedType = won.WON.BasicNeedTypeSupply;
        descriptionPhrase = 'I have something similar to: ';
    } else if(theirNeedType === won.WON.BasicNeedTypeSupplyCompacted) {
        reNeedType = won.WON.BasicNeedTypeDemand;
        descriptionPhrase = 'I want something like: ';
    } else if(theirNeedType === won.WON.BasicNeedTypeDotogetherCompacted) {
        reNeedType = won.WON.BasicNeedTypeDotogether;
        descriptionPhrase = 'I\'d like to find people for something like the following: ';
    } else {
        console.error(
            'The need responded to (' + get(theirNeed, 'uri') + ') doesn\'t ' +
            'have a need type recognized by ad-hoc-connect method. Type: ',
            theirNeedType
        );
        reNeedType = undefined;
        descriptionPhrase = 'It\'s a response to: ';
    }

    let theirDescription = get(theirNeed, 'description');
    let theirTitle = get(theirNeed, 'title');

    return {
        title: 'Re: ' + theirTitle,
        description:
        'This is an automatically generated post. ' +
        descriptionPhrase +
        '"' + (theirDescription? theirDescription : theirTitle) +'"',
        type: reNeedType,
        tags: cloneAsMutable(get(theirNeed, 'tags')),
        location: cloneAsMutable(get(theirNeed, 'location')),
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

export function connectionsRate(connectionUri,rating) {
    return (dispatch, getState) => {
        console.log(connectionUri);
        console.log(rating);

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
export function showLatestMessages(connectionUri, numberOfEvents){
    return (dispatch, getState) => {
        const state = getState();
        const connectionUri = selectOpenConnectionUri(state);
        const needUri = selectOpenPostUri(state);
        const connection = selectConnection(state, connectionUri);
        if (!connectionUri || !connection) return;

        const connectionMessages = connection.get('messages');
        if (connection.get('loadingEvents') || !connectionMessages || connectionMessages.size > 0) return; // only start loading once. //TODO: PENDING IS CURRENTLY NOT IMPLEMENTED IN THE NEW STATE

        dispatch({
            type: actionTypes.connections.showLatestMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });

        getEvents(
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
        const needUri = selectOpenPostUri(state);
        const events = state.getIn(["needs", needUri, "connections", connectionUri, "messages"]);
        // determine the oldest loaded event
        const sortedOwnEvents = events.valueSeq().sort( (event1, event2) => event1.get('date') - event2.date.get('date'));
        const oldestEvent = sortedOwnEvents.first();
        const eventHashValue = oldestEvent
                .get('uri')
                .replace(/.*\/event\/(.*)/, '$1'); // everything following the `/event/`
        dispatch({
            type: actionTypes.connections.showMoreMessages,
            payload: Immutable.fromJS({connectionUri, pending: true}),
        });

        getEvents(
            connectionUri,
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
