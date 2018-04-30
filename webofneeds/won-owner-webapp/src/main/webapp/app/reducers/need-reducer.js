/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6.js';
import {
    msStringToDate,
    getIn,
} from '../utils.js';
import {
    isUriRead,
    markUriAsRead,
    resetUrisRead,
} from '../won-localstorage.js';

const initialState = Immutable.fromJS({
});


export default function(allNeedsInState = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
        case actionTypes.needs.clean:
            return initialState;

        case actionTypes.loginStarted:
            // starting a new login process. this could mean switching
            // to a different session. we need to mark any needs
            // that are already loaded as non-owned.
            return allNeedsInState.map(need => need
                    .set('ownNeed', false)
                    .set('connections', Immutable.Map())
            );

        case actionTypes.initialPageLoad:
        case actionTypes.login:
            let ownNeeds = action.payload.get('ownNeeds');
            ownNeeds = ownNeeds ? ownNeeds : Immutable.Set();
            let theirNeeds = action.payload.get('theirNeeds');
            theirNeeds = theirNeeds ? theirNeeds : Immutable.Set();
            const stateWithOwnNeeds = ownNeeds.reduce(
                (updatedState, ownNeed) => addNeed(updatedState, ownNeed, true),
                allNeedsInState
            );
            const stateWithOwnAndTheirNeeds = theirNeeds.reduce(
                (updatedState, theirNeed) => addNeed(updatedState, theirNeed, false),
                stateWithOwnNeeds

            );

            return storeConnectionsData(stateWithOwnAndTheirNeeds, action.payload.get('connections'));

        case actionTypes.messages.closeNeed.failed:
            return storeConnectionsData(allNeedsInState, action.payload.get('connections'));

        case actionTypes.router.accessedNonLoadedPost:
            return addNeed(allNeedsInState, action.payload.get('theirNeed'), false);

        case actionTypes.needs.fetch:
            return action.payload.reduce(
                (updatedState, ownNeed) => addNeed(updatedState, ownNeed, true),
                allNeedsInState
            );

        case actionTypes.messages.reopenNeed.failed:
            return storeConnectionsData(allNeedsInState, action.payload.get('connections'));

        case actionTypes.needs.reopen:
            return changeNeedState(allNeedsInState, action.payload.ownNeedUri, won.WON.ActiveCompacted);

        case actionTypes.needs.close:
            return changeNeedState(allNeedsInState, action.payload.ownNeedUri, won.WON.InactiveCompacted);

        case actionTypes.needs.create: // optimistic need adding
            return addNeedInCreation(allNeedsInState, action.payload.need, action.payload.needUri)
        //return addNeedInCreation(allNeedsInState, action.payload.needList, action.payload.needUri);
        case actionTypes.needs.createSuccessful:
            return addNeed(allNeedsInState, action.payload.need, true);


        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData, false),
                allNeedsInState);

        case actionTypes.messages.openMessageReceived:
        case actionTypes.messages.connectMessageReceived:
            var ownNeedFromState = allNeedsInState.get(action.payload.ownNeedUri);
            var remoteNeedFromState = allNeedsInState.get(action.payload.remoteNeed['@id']);
            var remoteNeed = action.payload.remoteNeed;

            let changedState = ownNeedFromState ? allNeedsInState : addNeed(allNeedsInState, ownNeed, true);
            changedState = addNeed(changedState, remoteNeed, false); // guarantee
            // that
            // remoteNeed
            // is
            // in
            // state
            if (action.type == actionTypes.messages.connectMessageReceived){
                changedState = addConnectionFull(changedState, action.payload.connection);
            }

            if(action.payload.message){
                changedState  = addMessage(changedState , action.payload.message);
            }
            changedState  = changeConnectionStateByFun(
                changedState  ,
                action.payload.updatedConnection,
                    state => {
                    if (!state) return won.WON.RequestReceived; //fallback if no state present
                    if (state == won.WON.RequestSent) return won.WON.Connected;
                    if (state == won.WON.Suggested) return won.WON.RequestReceived;
                    if (state == won.WON.Closed) return won.WON.RequestReceived;
                    return won.WON.RequestReceived;
                });
            return changedState ;
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(allNeedsInState, action.payload, true);

        // NEW CONNECTIONS STATE UPDATES
        case actionTypes.connections.close:
            return changeConnectionState(allNeedsInState, action.payload.connectionUri, won.WON.Closed);

        case actionTypes.needs.connect: // user has sent a connect request

            const optimisticEvent = action.payload.optimisticEvent;
            const ownNeedUri = optimisticEvent.getSenderNeed();
            const theirNeedUri = optimisticEvent.getReceiverNeed();
            const eventUri = optimisticEvent.getMessageUri();
            let stateUpdated;

            if (action.payload.ownConnectionUri) {
                stateUpdated = changeConnectionState(allNeedsInState, action.payload.ownConnectionUri, won.WON.RequestSent);
                //because we have a connection uri, we can add the message
                return addMessage(stateUpdated, action.payload.optimisticEvent);
            } else {
                var tmpConnectionUri = 'connectionFrom:' + eventUri; // need to
                // wait for
                // success-response
                // to set
                // that
                var optimisticConnection = Immutable.fromJS({
                    uri: tmpConnectionUri,
                    usingTemporaryUri: true,
                    state: won.WON.RequestSent,
                    remoteNeedUri: theirNeedUri,
                    unread: true,
                    messages: {
                        [eventUri]: {
                            uri: eventUri,
                            text: optimisticEvent.getTextMessage(),
                            date: msStringToDate(optimisticEvent.getSentTimestamp()),
                            outgoingMessage: true,
                            unread: true,
                            connectMessage: true,
                            isRelevant: true,
                        }
                    }
                });
                return allNeedsInState.setIn([ownNeedUri, 'connections', tmpConnectionUri], optimisticConnection)
            }


        case actionTypes.connections.open: // user has sent an open request        	
            var cnctStateUpdated = changeConnectionStateByFun(allNeedsInState, action.payload.connectionUri,
                    state => {
                    if (!state) return won.WON.RequestSent; //fallback if no state present
                    if (state == won.WON.RequestReceived) return won.WON.Connected;
                    if (state == won.WON.Suggested) return won.WON.RequestSent;
                    if (state == won.WON.Closed) return won.WON.RequestSent;
                });
            return addMessage(cnctStateUpdated, action.payload.optimisticEvent);

        case actionTypes.messages.open.failure:
            return changeConnectionState(allNeedsInState,  action.payload.events['msg:FromSystem'].hasReceiver, won.WON.RequestReceived);

        case actionTypes.messages.open.successRemote:
        case actionTypes.messages.connect.successRemote:
            // use the remote success message to obtain the remote connection
            // uri (which we may not have known)
            var wonMessage = action.payload;
            var connectionUri =  wonMessage.getReceiver();
            var needUri =  wonMessage.getReceiverNeed();
            var remoteConnectionUri = wonMessage.getSender();

            if(allNeedsInState.getIn([needUri, 'connections', connectionUri])){
                return allNeedsInState.setIn([needUri, 'connections', connectionUri, 'remoteConnectionUri'], remoteConnectionUri);
            }else{
                console.warn("Open/Connect success for a connection that is not stored in the state yet, connUri: ",connectionUri);
                return allNeedsInState;
            }

        case actionTypes.messages.connect.successOwn:
            // TODO SRP; split in isSuccessOfAdHocConnect, addAddHoc(?) and
            // changeConnectionState
            const wonMessage = action.payload;
            const tmpConnectionUri = "connectionFrom:" + wonMessage.getIsResponseTo();
            const connectionUri = wonMessage.getReceiver();
            const needForTmpCnct = selectNeedByConnectionUri(allNeedsInState, tmpConnectionUri);
            var unsortedAdHocConnection = needForTmpCnct && needForTmpCnct.getIn(['connections', tmpConnectionUri]);
            if(unsortedAdHocConnection) {
                // connection was established from scratch without having a
                // connection uri. now that we have the uri, we can store it
                // (see connectAdHoc)
                var needUri = needForTmpCnct.get('uri');
                if(!needForTmpCnct.get('ownNeed')) {
                    throw new Exception('Trying to add/change connection for need that\'s not an "ownNeed".');
                }

                const properConnection = unsortedAdHocConnection
                    .delete('usingTemporaryUri')
                    .set('uri', connectionUri);

                return allNeedsInState
                    .deleteIn([needUri, 'connections', tmpConnectionUri])
                    .mergeDeepIn([needUri, 'connections', connectionUri], properConnection);
            } else {
                // connection has been stored as match first
                return changeConnectionState(allNeedsInState, connectionUri, won.WON.RequestSent);
            }

        case actionTypes.messages.close.success:
            return changeConnectionState(allNeedsInState,  action.payload.getReceiver(), won.WON.Closed);
        case actionTypes.messages.markAsRead:
            return markMessageAsRead(allNeedsInState, action.payload.messageUri, action.payload.connectionUri, action.payload.needUri);
        case actionTypes.messages.markAsRelevant:
        	return markMessageAsRelevant(allNeedsInState, action.payload.messageUri, action.payload.connectionUri, action.payload.needUri, action.payload.relevant);
        case actionTypes.connections.markAsRead:
            return markConnectionAsRead(allNeedsInState, action.payload.connectionUri, action.payload.needUri);
        case actionTypes.connections.markAsRated:
            return markConnectionAsRated(allNeedsInState, action.payload.connectionUri);
        case actionTypes.connections.setLoading:
        	return setConnectionLoading(allNeedsInState, action.payload.connectionUri, action.payload.isLoading);
        // NEW MESSAGE STATE UPDATES
        case actionTypes.messages.connectionMessageReceived:
            // ADD RECEIVED CHAT MESSAGES
            // payload; { events }
            return addMessage(allNeedsInState, action.payload);

        case actionTypes.connections.sendChatMessage:
            // ADD SENT TEXT MESSAGE
            /*
             * payload: { eventUri: optimisticEvent.uri, message,
             * optimisticEvent, }
             */
            return addMessage(allNeedsInState, action.payload.optimisticEvent);

        // update timestamp on success response
        case actionTypes.messages.connect.successOwn:
        case actionTypes.messages.open.successOwn:
        case actionTypes.messages.chatMessage.successOwn:
            var wonMessage = getIn(action, ['payload']);
            var eventUri = wonMessage.getIsResponseTo();
            var needUri = wonMessage.getReceiverNeed();
            var connectionUri = wonMessage.getReceiver();
            // we want to use the response date to update the original message
            // date
            // in order to use server timestamps everywhere
            var responseDateOnServer =  msStringToDate(wonMessage.getTimestamp());
            // make sure we have an event with that uri:
            var eventToUpdate = allNeedsInState.getIn([needUri, 'connections', connectionUri, 'messages', eventUri]);
            if (eventToUpdate) {
                allNeedsInState =  allNeedsInState.setIn([needUri, 'connections', connectionUri, 'messages', eventUri, 'date'], responseDateOnServer);
            }
            return allNeedsInState;


        case actionTypes.connections.showLatestMessages:
        case actionTypes.connections.showMoreMessages:
            var isLoading = action.payload.get('isLoading');
            var connectionUri = action.payload.get('connectionUri');

            if(isLoading && connectionUri){
                allNeedsInState = setConnectionLoading(allNeedsInState, connectionUri, true);
            }

            var loadedMessages = action.payload.get('events');
            if(loadedMessages){
                allNeedsInState = addMessages(allNeedsInState, loadedMessages);
                allNeedsInState = setConnectionLoading(allNeedsInState, connectionUri, false);
            }
            const error = action.payload.get('error');

            if(error && connectionUri){
                allNeedsInState = setConnectionLoading(allNeedsInState, connectionUri, false);
            }

            return allNeedsInState;

        default:
            return allNeedsInState;
    }
}

function storeConnectionAndRelatedData(state, connectionWithRelatedData, unread) {
    const {ownNeed, remoteNeed, connection} = connectionWithRelatedData;
    const stateWithOwnNeed = addNeed(state, ownNeed, true); // guarantee that
                                                            // ownNeed is in
                                                            // state
    const stateWithBothNeeds = addNeed(stateWithOwnNeed, remoteNeed, false); // guarantee
    // that
    // remoteNeed
    // is
    // in
    // state

    return addConnectionFull(stateWithBothNeeds, connection);
}

function addNeed(needs, jsonldNeed, ownNeed) {
    const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

    let newState;
    let parsedNeed = parseNeed(jsonldNeed, ownNeed);

    if(parsedNeed && parsedNeed.get("uri")) {
        let existingNeed = needs.get(parsedNeed.get("uri"));
        if(ownNeed && existingNeed){ // If need is already present and the
            // need is claimed as an own need we set
            // have to set it
            if (existingNeed.get("beingCreated")){
                // replace it
                parsedNeed = parsedNeed.set("connections", existingNeed.get("connections"));
                return needs.setIn([parsedNeed.get("uri")], parsedNeed);
            } else {
                // just be sure we mark it as own need
                return needs.setIn([parsedNeed.get("uri"), "ownNeed"], ownNeed);
            }
        } else {
            return setIfNew(needs, parsedNeed.get("uri"), parsedNeed);
        }
    } else {
        console.error('Tried to add invalid need-object: ', jsonldNeedImm.toJS());
        newState = needs;
    }
    return newState;
}

function addNeedInCreation(needs, needInCreation, needUri) {
    let newState;
    let need = Immutable.fromJS(needInCreation);
    if(need) {
        need = need.set("beingCreated",true);
        need = need.set("ownNeed", true);
        need = need.set("needUri", needUri);
        need = need.set("connections", Immutable.Map());
        if (need.get("whatsAround")){
            need = need.set("isWhatsAround", true);
        }
        newState = needs.setIn([needUri], need);
    } else {
        console.error('Tried to add invalid need-object: ', needInCreation);
        newState = needs;
    }
    return newState;
}


function storeConnectionsData(state, connectionsToStore, newConnections) {
    newConnections = newConnections ? newConnections : Immutable.Set();

    if(connectionsToStore && connectionsToStore.size > 0) {
        connectionsToStore.forEach(connection => {
            state = addConnectionFull(state, connection);
        });
    }
    return state;
}

/**
 * Add's the connection to the needs connections.
 *
 * @param state
 * @param connection
 * @param newConnection
 * @return {*}
 */
function addConnectionFull(state, connection) {
    let parsedConnection = parseConnection(connection);

    if(parsedConnection){
        // console.log("parsedConnection: ", parsedConnection.toJS(), "immutable
        // ", parsedConnection);

        const needUri = parsedConnection.get("belongsToUri");
        let connections = state.getIn([needUri, 'connections']);

        if(connections){
            const connectionUri = parsedConnection.getIn(["data", "uri"]);

            if(parsedConnection.getIn(["data", "unread"])) {
                //If there is a new message for the connection we will set the connection to newConnection
                state = state.setIn([needUri, "lastUpdateDate"], parsedConnection.getIn(["data", "lastUpdateDate"]));
                state = state.setIn([needUri, "unread"], true);
            }


            return state.mergeDeepIn([needUri, "connections", connectionUri], parsedConnection.get("data"));
        }else{
            console.error("Couldnt add valid connection - missing need data in state", needUri, "parsedConnection: ", parsedConnection.toJS());
        }
    }else{
        // console.log("No connection parsed, add no connection to this state:
        // ", state);
    }
    return state;
}

function addMessage(state, wonMessage) {
    if (wonMessage.getContentGraphs().length > 0) {
        // we only want to add messages to the state that actually contain text
        // content. (no empty connect messages, for example)
        let parsedMessage = parseMessage(wonMessage);

        if (parsedMessage) {
            const isNewMessage = parsedMessage.getIn(["data", "uri"]);

            const connectionUri = parsedMessage.get("belongsToUri");
            let needUri = null;
            if (parsedMessage.getIn(["data", "outgoingMessage"])) {
                // needUri is the message's hasSenderNeed
                needUri = wonMessage.getSenderNeed();
            } else {
                // needUri is the remote message's hasReceiverNeed
                needUri = wonMessage.getReceiverNeed();
                if(!!parsedMessage.getIn(["data", "unread"])) {
                    //If there is a new message for the connection we will set the connection to newConnection
                    state = state.setIn([needUri, "lastUpdateDate"], parsedMessage.getIn(["data", "date"]));
                    state = state.setIn([needUri, "unread"], true);
                    state = state.setIn([needUri, "connections", connectionUri, "lastUpdateDate"], parsedMessage.getIn(["data", "date"]));
                    state = state.setIn([needUri, "connections", connectionUri, "unread"], true);
                }
            }
            if (needUri) {
                let messages = state.getIn([needUri, "connections", connectionUri, "messages"]);
                messages = messages.set(parsedMessage.getIn(["data", "uri"]), parsedMessage.get("data"));
                return state.setIn([needUri, "connections", connectionUri, "messages"], messages);
            }
        }
    }
    return state;
}

function addMessages(state, wonMessages) {
    if(wonMessages && wonMessages.size > 0){
        wonMessages.map(wonMessage => {
            state = addMessage(state, wonMessage);
        });
    }else{
        console.log("no messages to add");
    }
    return state;
}


function setIfNew(state, path, obj){
    return state.update(path, val => val ?
        // we've seen this need before, no need to overwrite it
        val :
        // it's the first time we see this need -> add it
        Immutable.fromJS(obj))
}

function changeConnectionState(state, connectionUri, newState) {
    const need = selectNeedByConnectionUri(state, connectionUri);

    if(!need) {
        console.error("no need found for connectionUri", connectionUri);
        return state;
    }

    const needUri = need.get("uri");

    return state
        .setIn([needUri, "connections", connectionUri, "state"], newState)
        .setIn([needUri, "connections", connectionUri, "unread"], true);
}

function markMessageAsRelevant(state, messageUri, connectionUri, needUri, relevant) {
	
	let need = state.get(needUri);
    let connection = need && need.getIn(["connections", connectionUri]);
    let message = connection && connection.getIn(["messages", messageUri]);

    if(!message) {
        console.error("no message with messageUri: <", messageUri,"> found within needUri: <", needUri, "> connectionUri: <", connectionUri, ">");
        return state;
    }
    return state.setIn([needUri, "connections", connectionUri, "messages", messageUri, "isRelevant"], relevant);
}

function markMessageAsRead(state, messageUri, connectionUri, needUri) {

    const need = state.get(needUri);
    const connection = need && need.getIn(["connections", connectionUri]);
    const message = connection && connection.getIn(["messages", messageUri]);

    markUriAsRead(messageUri);

    if(!message) {
        console.error("no message with messageUri: <", messageUri,"> found within needUri: <", needUri, "> connectionUri: <", connectionUri, ">");
        return state;
    }

    state = state.setIn([needUri, "connections", connectionUri, "messages", messageUri, "unread"], false);

    if( state.getIn([needUri, "connections", connectionUri, "messages"]).filter(msg => msg.get("unread")).size == 0){
        state = markConnectionAsRead(state, connectionUri, needUri);
    }

    return state.setIn([needUri, "connections", connectionUri, "messages", messageUri, "unread"], false);
}

function markConnectionAsRead(state, connectionUri, needUri) {
    const need = state.get(needUri);
    const connection = need && need.getIn(["connections", connectionUri]);

    markUriAsRead(connectionUri);

    if(!connection) {
        console.error("no connection with connectionUri: <", connectionUri,"> found within needUri: <", needUri, ">");
        return state;
    }

    state = state.setIn([needUri, "connections", connectionUri, "unread"], false);

    if( state.getIn([needUri, "connections"]).filter(conn => conn.get("unread")).size == 0 ) {
        state = markNeedAsRead(state, needUri);
    }

    return state;
}

function setConnectionLoading(state, connectionUri, isLoading) {
    const need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
    const needUri = need && need.get("uri");
    const connection = need && need.getIn(["connections", connectionUri]);

    if(!connection) {
        console.error("no connection with connectionUri: <", connectionUri,"> found within needUri: <", needUri, ">");
        return state;
    }

    return state.setIn([needUri, "connections", connectionUri, "isLoading"], isLoading);
}

function markNeedAsRead(state, needUri) {
    const need = state.get(needUri);

    if(!need) {
        console.error("no need with needUri: <", needUri, ">");
        return state;
    }
    return state.setIn([needUri, "unread"], false);
}

function markConnectionAsRated(state, connectionUri) {
    let need = connectionUri && selectNeedByConnectionUri(state, connectionUri);
    let connection = need && need.getIn(["connections", connectionUri]);

    if(!connection) {
        console.error("no connection with connectionUri: <", connectionUri,"> found within needUri: <", need.get("uri"), ">");
        return state;
    }

    return state.setIn([need.get("uri"), "connections", connectionUri, "isRated"], true);
}

function changeConnectionStateByFun(state, connectionUri, fun) {
    const need = selectNeedByConnectionUri(state, connectionUri);

    if(!need) {
        console.error("no need found for connectionUri", connectionUri);
        return state;
    }

    const needUri = need.get("uri");
    const connectionState = state.getIn([needUri, "connections", connectionUri, "state"]);
    return state
        .setIn([needUri, "connections", connectionUri, "state"], fun(connectionState))
        .setIn([needUri, "connections", connectionUri, "unread"], true);
}


function changeNeedState(state, needUri, newState) {
    return state
        .setIn([needUri, "state"], newState);
}



function parseConnection(jsonldConnection) {
    const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);
    // console.log("Connection to parse: ", jsonldConnectionImm.toJS());

    let parsedConnection = {
        belongsToUri: undefined,
        data: {
            uri: undefined,
            state: undefined,
            messages: Immutable.Map(),
            remoteNeedUri: undefined,
            remoteConnectionUri: undefined,
            creationDate: undefined,
            lastUpdateDate: undefined,
            unread: undefined,
            isRated: false,
            isLoading: false,
        }
    };

    const belongsToUri = jsonldConnectionImm.get("belongsToNeed");
    const remoteNeedUri = jsonldConnectionImm.get("hasRemoteNeed");
    const remoteConnectionUri = jsonldConnectionImm.get("hasRemoteConnection");
    const uri = jsonldConnectionImm.get("uri");

    if(!!uri && !!belongsToUri && !!remoteNeedUri){
        parsedConnection.belongsToUri = belongsToUri;
        parsedConnection.data.uri = uri;
        parsedConnection.data.unread = !isUriRead(uri);
        parsedConnection.data.remoteNeedUri = remoteNeedUri;
        parsedConnection.data.remoteConnectionUri = remoteConnectionUri;

        const creationDate = jsonldConnectionImm.get("modified");
        if(!!creationDate){
            parsedConnection.data.creationDate = new Date(creationDate);
            parsedConnection.data.lastUpdateDate = parsedConnection.data.creationDate;
        }

        const state = jsonldConnectionImm.get("hasConnectionState");
        if(
            (state === won.WON.RequestReceived) ||
            (state === won.WON.RequestSent) ||
            (state === won.WON.Suggested) ||
            (state === won.WON.Connected) ||
            (state === won.WON.Closed)
        ) {
            parsedConnection.data.state = state;
        }else{
            console.error('Cant parse connection, data is an invalid connection-object (faulty state): ', jsonldConnectionImm.toJS());
            return undefined; // FOR UNKNOWN STATES
        }

        return Immutable.fromJS(parsedConnection);
    }else{
        console.error('Cant parse connection, data is an invalid connection-object (mandatory uris could not be retrieved): ', jsonldConnectionImm.toJS());
        return undefined;
    }
}

function parseMessage(wonMessage) {
    const rawContentGraphTrig = (wonMessage.contentGraphTrig || "");
    const contentGraphTrigLines = (wonMessage.contentGraphTrig || "").split('\n');

    //seperating off header/@prefix-statements, so they can be folded in
    const contentGraphTrigPrefixes = contentGraphTrigLines
        .filter(line => line.startsWith('@prefix'))
        .join('\n');

    const contentGraphTrigBody = contentGraphTrigLines
        .filter(line => !line.startsWith('@prefix'))
        .map(line => line
            // add some extra white-space between statements, so they stay readable even when they wrap.
            .replace(/\.$/, '.\n')
            .replace(/\;$/, ';\n')
            .replace(/\{$/, '{\n')
            .replace(/^\}$/, '\n}')
    )
        .join('\n')
        .trim();

    let parsedMessage = {
        belongsToUri: undefined,
        data: {
            uri: wonMessage.getMessageUri(),
            remoteUri: !wonMessage.isFromOwner()? wonMessage.getRemoteMessageUri() : undefined,
            text: wonMessage.getTextMessage(),
            contentGraphs: wonMessage.getContentGraphs(),
            date: msStringToDate(wonMessage.getTimestamp()),
            outgoingMessage: wonMessage.isFromOwner(),
            unread: !wonMessage.isFromOwner() && !isUriRead(wonMessage.getMessageUri()),
            connectMessage: wonMessage.isConnectMessage(),
            //TODO: add all different types
            clauses: wonMessage.isProposeMessage()? wonMessage.getProposedMessages() : wonMessage.getProposedToCancelMessages(),
            isProposeMessage: wonMessage.isProposeMessage(),
            isAcceptMessage: wonMessage.isAcceptMessage(),
            isProposeToCancel: wonMessage.isProposeToCancel(),
            isRejectMessage: wonMessage.isRejectMessage(),
            isRetractMessage: wonMessage.isRetractMessage(),
            isRelevant: true,
            contentGraphTrig: {
                prefixes: contentGraphTrigPrefixes,
                body: contentGraphTrigBody,
            },
            contentGraphTrigRaw: wonMessage.contentGraphTrig,
            contentGraphTrigError: wonMessage.contentGraphTrigError,

        }
    };

    if(wonMessage.isFromOwner()){
        parsedMessage.belongsToUri = wonMessage.getSender();
    } else {
        parsedMessage.belongsToUri = wonMessage.getReceiver();
    }

    if(
        !parsedMessage.data.uri ||
        !parsedMessage.belongsToUri ||
        !parsedMessage.data.date
    ) {
        console.error('Cant parse chat-message, data is an invalid message-object: ', wonMessage);
        return undefined;
    } else {
        return Immutable.fromJS(parsedMessage);
    }
}

function parseNeed(jsonldNeed, ownNeed) {
    const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

    let parsedNeed = {
        uri: undefined,
        nodeUri: undefined,
        title: undefined,
        description: undefined,
        type: undefined,
        state: undefined,
        tags: undefined,
        location: undefined,
        connections: Immutable.Map(),
        creationDate: undefined,
        lastUpdateDate: undefined,
        unread: false,
        ownNeed: !!ownNeed,
        isWhatsAround: false,
        matchingContexts: undefined,
    };

    if(jsonldNeedImm){
        const uri = jsonldNeedImm.get("@id");
        const nodeUri = jsonldNeedImm.getIn(["won:hasWonNode", "@id"]);
        const isPresent = !!jsonldNeedImm.getIn(["won:is", "dc:title"]);
        const seeksPresent = !!jsonldNeedImm.getIn(["won:seeks", "dc:title"]);
        const is = jsonldNeedImm.get("won:is");
        const seeks = jsonldNeedImm.get("won:seeks");

        //TODO We need to decide which is the main title? Or combine?
        const title = isPresent ? is.get("dc:title") : (seeksPresent ? seeks.get("dc:title") : undefined);

        if(!!uri && !!title){
            parsedNeed.uri = uri;
            parsedNeed.title = title;
        }else{
            return undefined;
        }

        /*
         * The following code-snippet is solely to determine if the parsed need
         * is a special "whats around"-need, in order to do this we have to make
         * sure that the won:hasFlag is checked in two forms, both as a string
         * and an immutable object
         */
        const wonHasFlags = jsonldNeedImm.get("won:hasFlag");
        const isWhatsAround = wonHasFlags && wonHasFlags
                .filter(function(flag) {
                    if(flag instanceof Immutable.Map){
                        return flag.get("@id") === "won:WhatsAround";
                    } else {
                        return flag === "won:WhatsAround";
                    }
                })
                .size > 0;

        const wonHasMatchingContexts = jsonldNeedImm.get("won:hasMatchingContext");

        const creationDate = jsonldNeedImm.get("dct:created");
        if(creationDate){
            parsedNeed.creationDate = new Date(creationDate);
            parsedNeed.lastUpdateDate = parsedNeed.creationDate;
        }

        const state = jsonldNeedImm.getIn([won.WON.isInStateCompacted, "@id"]);
        if(state === won.WON.ActiveCompacted){ // we use to check for active
            // state and everything else
            // will be inactive
            parsedNeed.state = state;
        } else {
            parsedNeed.state = won.WON.InactiveCompacted;
        }

        let isPart = undefined;
        let seeksPart = undefined;
        let type = undefined;

        /*
         let description = undefined;
         let tags = undefined;
         let location = undefined;
         */
        //TODO: Type concept?
        if(isPresent){
            type = seeksPresent ? won.WON.BasicNeedTypeCombinedCompacted : won.WON.BasicNeedTypeSupplyCompacted;
            let tags = is.get("won:hasTag")? is.get("won:hasTag") : undefined;
            isPart = {
                title: is.get("dc:title"),
                type: type,
                description: (is.get("dc:description")? is.get("dc:description") : undefined),
                tags: (tags? (Immutable.List.isList(tags)? tags : Immutable.List.of(tags)) : undefined),
                location: (is.get("won:hasLocation")? parseLocation(is.get("won:hasLocation")) : undefined),
            };
        }
        if(seeksPresent){
            type = isPresent? type : won.WON.BasicNeedTypeDemandCompacted;
            let tags = seeks.get("won:hasTag")? seeks.get("won:hasTag") : undefined;
            seeksPart = {
                title: seeks.get("dc:title"),
                type: type,
                description: (seeks.get("dc:description")? seeks.get("dc:description") : undefined),
                tags: (tags? (Immutable.List.isList(tags)? tags : Immutable.List.of(tags)) : undefined),
                location: (seeks.get("won:hasLocation")? parseLocation(seeks.get("won:hasLocation")) : undefined),
            };
        }

        parsedNeed.is = isPart;
        parsedNeed.seeks = seeksPart;
        parsedNeed.type = isWhatsAround? won.WON.BasicNeedTypeWhatsAroundCompacted : type;
        parsedNeed.isWhatsAround = !!isWhatsAround;
        parsedNeed.matchingContexts =  wonHasMatchingContexts ? ( Immutable.List.isList(wonHasMatchingContexts) ? wonHasMatchingContexts : Immutable.List.of(wonHasMatchingContexts) ) : undefined;
        parsedNeed.nodeUri = nodeUri;
    }else{
        console.error('Cant parse need, data is an invalid need-object: ', jsonldNeedImm && jsonldNeedImm.toJS());
        return undefined;
    }

    return Immutable.fromJS(parsedNeed);
}

function parseLocation(jsonldLocation) {
    if(!jsonldLocation) return undefined; // NO LOCATION PRESENT

    const jsonldLocationImm = Immutable.fromJS(jsonldLocation);

    let location = {
        address: undefined,
        lat: undefined,
        lng: undefined,
        nwCorner: {
            lat: undefined,
            lng: undefined,
        },
        seCorner: {
            lat: undefined,
            lng: undefined
        }
    };

    location.address = jsonldLocationImm.get("s:name");

    location.lat = Number.parseFloat(jsonldLocationImm.getIn(["s:geo", "s:latitude"]));
    location.lng = Number.parseFloat(jsonldLocationImm.getIn(["s:geo", "s:longitude"]));

    location.nwCorner.lat = Number.parseFloat(jsonldLocationImm.getIn(["won:hasBoundingBox", "won:hasNorthWestCorner", "s:latitude"]));
    location.nwCorner.lng = Number.parseFloat(jsonldLocationImm.getIn(["won:hasBoundingBox", "won:hasNorthWestCorner", "s:longitude"]));
    location.seCorner.lat = Number.parseFloat(jsonldLocationImm.getIn(["won:hasBoundingBox", "won:hasSouthEastCorner", "s:latitude"]));
    location.seCorner.lng = Number.parseFloat(jsonldLocationImm.getIn(["won:hasBoundingBox", "won:hasSouthEastCorner", "s:longitude"]));

    if(
        location.address &&
        location.lat &&
        location.lng &&
        location.nwCorner.lat &&
        location.nwCorner.lng &&
        location.seCorner.lat &&
        location.seCorner.lng
    ){
        return Immutable.fromJS(location);
    }

    console.error('Cant parse location, data is an invalid location-object: ', jsonldLocationImm.toJS());
    return undefined;
}


/**
 * Get the need for a given connectionUri
 *
 * @param state
 *            to retrieve data from
 * @param connectionUri
 *            to find corresponding need for
 */
function selectNeedByConnectionUri(allNeedsInState, connectionUri){
    return allNeedsInState.filter(need =>
        need.getIn(["connections", connectionUri])).first();
}