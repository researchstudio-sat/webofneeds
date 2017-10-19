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

            return storeConnectionsData(stateWithOwnAndTheirNeeds, action.payload.get('connections'), false);

        case actionTypes.messages.closeNeed.failed:
            return storeConnectionsData(allNeedsInState, action.payload.get('connections'), false);

        case actionTypes.router.accessedNonLoadedPost:
            return addNeed(allNeedsInState, action.payload.get('theirNeed'), false);

        case actionTypes.needs.fetch:
            return action.payload.reduce(
                (updatedState, ownNeed) => addNeed(updatedState, ownNeed, true),
                allNeedsInState
            );

        case actionTypes.needs.reopen:
            return changeNeedState(allNeedsInState, action.payload.ownNeedUri, won.WON.ActiveCompacted);

        case actionTypes.needs.close:
            return changeNeedState(allNeedsInState, action.payload.ownNeedUri, won.WON.InactiveCompacted);

        //case actionTypes.needs.create: //TODO optimistic need adding
        case actionTypes.needs.createSuccessful:
            return addNeed(allNeedsInState, action.payload.need, true);


        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData, false),
                allNeedsInState);

        case actionTypes.messages.connectMessageReceived:
            var ownNeedFromState = allNeedsInState.get(action.payload.ownNeedUri);
            var remoteNeedFromState = allNeedsInState.get(action.payload.remoteNeed['@id']);
            var remoteNeed = action.payload.remoteNeed;

            var stateWithOwnNeed = ownNeedFromState ? allNeedsInState : addNeed(allNeedsInState, ownNeed, true);
            var stateWithBothNeeds = addNeed(stateWithOwnNeed, remoteNeed, false); // guarantee that remoteNeed is in state
            var stateWithBothNeedsAndConnection = addConnectionFull(stateWithBothNeeds, action.payload.connection, true);

            let stateWithEverything = stateWithBothNeedsAndConnection;
            if(action.payload.message){
                stateWithEverything = addMessage(stateWithEverything, action.payload.message, false, true);
            }

            return stateWithEverything;
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(allNeedsInState, action.payload, true);

        //NEW CONNECTIONS STATE UPDATES
        case actionTypes.connections.close:
            return changeConnectionState(allNeedsInState, action.payload.connectionUri, won.WON.Closed);

        case actionTypes.messages.openMessageReceived:
            var withConnection = addConnectionFull(allNeedsInState, action.payload.connection);
            return addMessage(withConnection, action.payload.message, false, true);

        case actionTypes.connections.connectAdHoc:
            var optimisticEvent = getIn(action, ['payload', 'optimisticEvent']);
            var ownNeedUri = optimisticEvent.hasSenderNeed;
            var theirNeedUri = optimisticEvent.hasReceiverNeed;
            var eventUri = optimisticEvent.uri;
            var tmpConnectionUri = 'connectionFrom:' + eventUri; // need to wait for success-response to set that
            var optimisticConnection = Immutable.fromJS({
                uri: tmpConnectionUri,
                usingTemporaryUri: true,
                state: won.WON.RequestSent,
                remoteNeedUri: theirNeedUri,
                newConnection: true,
                messages: {
                    [eventUri]: {
                        uri: eventUri,
                        text: optimisticEvent.hasTextMessage,
                        date: msStringToDate(optimisticEvent.hasSentTimestamp),
                        outgoingMessage: true,
                        newMessage: true,
                        connectMessage: true,
                    }
                }
            });
            // as eventUris shouldn't clash with connectionUris, we can store it like this and already display it
            return allNeedsInState.setIn([ownNeedUri, 'connections', tmpConnectionUri], optimisticConnection);


        case actionTypes.connections.connect: // user has sent a request
            var cnctStateUpdated = changeConnectionState(allNeedsInState, action.payload.connectionUri, won.WON.RequestSent);
            return addMessage(cnctStateUpdated, action.payload.optimisticEvent, true, false);

        case actionTypes.connections.open:
            var cnctStateUpdated = changeConnectionState(allNeedsInState,  action.payload.optimisticEvent.getSender(), won.WON.Connected);
            return addMessage(cnctStateUpdated, action.payload.optimisticEvent, true, false);

        case actionTypes.messages.open.failure:
            return changeConnectionState(allNeedsInState,  action.payload.events['msg:FromSystem'].hasReceiver, won.WON.RequestReceived);

        case actionTypes.messages.open.successRemote:
        case actionTypes.messages.connect.successRemote:
            // use the remote success message to obtain the remote connection uri (which we may not have known)
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
            //TODO SRP; split in isSuccessOfAdHocConnect, addAddHoc(?) and changeConnectionState
            var wonMessage = action.payload;
            var connectionUri = wonMessage.getReceiver();
            var needForTmpCnct = selectNeedByConnectionUri(allNeedsInState, connectionUri);
            var unsortedAdHocConnection = needForTmpCnct && needForTmpCnct.getIn(['connections', connectionUri]);
            if(unsortedAdHocConnection) {
                // connection was established from scratch without having a connection uri. now that we have the uri, we can store it (see connectAdHoc)
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

        //case actionTypes.messages.close.failure:
            //do the same like in success -> debugging -> need ConnectionUR
            // return changeConnectionState(allNeedsInState,  action.payload.getReceiver(), won.WON.Closed);

        //NEW MESSAGE STATE UPDATES
        case actionTypes.messages.connectionMessageReceived:
            //ADD RECEIVED CHAT MESSAGES
            //payload; { events }
            return addMessage(allNeedsInState, action.payload, false, true);

        case actionTypes.connections.sendChatMessage:
            //ADD SENT TEXT MESSAGE
            /*payload: {
                eventUri: optimisticEvent.uri,
                message,
                optimisticEvent,
             }*/
            console.log("sendChatMessage: ", action.payload.optimisticEvent);
            return addMessage(allNeedsInState, action.payload.optimisticEvent, true, true);

        // update timestamp on success response
        case actionTypes.messages.connect.successOwn:
        case actionTypes.messages.open.successOwn:
        case actionTypes.messages.chatMessage.successOwn:
            var wonMessage = getIn(action, ['payload']);
            var eventUri = wonMessage.getIsResponseTo();
            var needUri = wonMessage.getReceiverNeed();
            var connectionUri = wonMessage.getReceiver();
            // we want to use the response date to update the original message date
            // in order to use server timestamps everywhere
            var responseDateOnServer =  msStringToDate(wonMessage.getTimestamp());
            //make sure we have an event with that uri:
            var eventToUpdate = allNeedsInState.getIn([needUri, 'connections', connectionUri, 'messages', eventUri]);
            if (eventToUpdate) {
                allNeedsInState =  allNeedsInState.setIn([needUri, 'connections', connectionUri, 'messages', eventUri, 'date'], responseDateOnServer);
            }
            return allNeedsInState;


        case actionTypes.connections.showLatestMessages:
        case actionTypes.connections.showMoreMessages:
            var loadedEvents = action.payload.get('events');
            if(loadedEvents){
                allNeedsInState = addMessages(allNeedsInState, loadedEvents);
            }

            return allNeedsInState;

        default:
            return allNeedsInState;
    }
}

function storeConnectionAndRelatedData(state, connectionWithRelatedData, newConnection) {
    const {ownNeed, remoteNeed, connection} = connectionWithRelatedData;
    const stateWithOwnNeed = addNeed(state, ownNeed, true); // guarantee that ownNeed is in state
    const stateWithBothNeeds = addNeed(stateWithOwnNeed, remoteNeed, false); // guarantee that remoteNeed is in state

    return addConnectionFull(stateWithBothNeeds, connection, newConnection);
}

function addNeed(needs, jsonldNeed, ownNeed) {
    const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

    let newState;
    let parsedNeed = parseNeed(jsonldNeed, ownNeed);

    if(parsedNeed && parsedNeed.get("uri")) {
        if(ownNeed && needs.get(parsedNeed.get("uri"))){ //If need is already present and the need is claimed as an own need we set have to set it
            newState = needs.setIn([parsedNeed.get("uri"), "ownNeed"], ownNeed);
        }else{
            newState = setIfNew(needs, parsedNeed.get("uri"), parsedNeed);
        }
    } else {
        console.error('Tried to add invalid need-object: ', jsonldNeedImm.toJS());
        newState = needs;
    }


    return newState;
}


function storeConnectionsData(state, connectionsToStore, newConnections) {
    newConnections = newConnections ? newConnections : Immutable.Set();

    if(connectionsToStore && connectionsToStore.size > 0) {
        connectionsToStore.forEach(connection => {
            state = addConnectionFull(state, connection, newConnections);
        });
    }
    return state;
}

/**
 * Add's the connection to the needs connections.
 * @param state
 * @param connection
 * @param newConnection
 * @return {*}
 */
function addConnectionFull(state, connection, newConnection) {

    //console.log("Adding Full Connection");
    if(newConnection === undefined) {
      newConnection = !!selectNeedByConnectionUri(state, connection.uri || connection.get('uri')); // do we already have a connection like that?
    }
    let parsedConnection = parseConnection(connection, newConnection);

    if(parsedConnection){
        //console.log("parsedConnection: ", parsedConnection.toJS(), "immutable ", parsedConnection);

        const needUri = parsedConnection.get("belongsToUri");
        let connections = state.getIn([needUri, 'connections']);

        if(connections){
            const connectionUri = parsedConnection.getIn(["data", "uri"]);
            return state.mergeDeepIn([needUri, "connections", connectionUri], parsedConnection.get("data"));
        }else{
            console.error("Couldnt add valid connection - missing need data in state", needUri, "parsedConnection: ", parsedConnection.toJS());
        }
    }else{
        //console.log("No connection parsed, add no connection to this state: ", state);
    }
    return state;
}

function addMessage(state, wonMessage, outgoingMessage, newMessage) {
    if (wonMessage.getTextMessage()) {
        // we only want to add messages to the state that actually contain text content.
        // (no empty connect messages, for example)
        let parsedMessage = parseMessage(wonMessage, outgoingMessage, newMessage);

        if (parsedMessage) {
            const connectionUri = parsedMessage.get("belongsToUri");
            let needUri = null;
            if (outgoingMessage) {
                //needUri is the message's hasSenderNeed
                needUri = wonMessage.getSenderNeed();
            } else {
                //needUri is the remote message's hasReceiverNeed
                needUri = wonMessage.getReceiverNeed();
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
            const outgoingMessage = wonMessage.isFromOwner();
            state = addMessage(state, wonMessage, outgoingMessage, true);
        });
    }else{
        console.log("no messages to add");
    }
    return state;
}


function setIfNew(state, path, obj){
    return state.update(path, val => val ?
        //we've seen this need before, no need to overwrite it
        val :
        //it's the first time we see this need -> add it
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
            .setIn([needUri, "connections", connectionUri, "newConnection"], true);
}

function changeNeedState(state, needUri, newState) {
    return state
        .setIn([needUri, "state"], newState);
}



function parseConnection(jsonldConnection, newConnection) {
    const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);
    //console.log("Connection to parse: ", jsonldConnectionImm.toJS());

    let parsedConnection = {
        belongsToUri: undefined,
        data: {
            uri: undefined,
            state: undefined,
            messages: Immutable.Map(),
            remoteNeedUri: undefined,
            remoteConnectionUri: undefined,
            creationDate: undefined,
            newConnection: !!newConnection,
        }
    };

    const belongsToUri = jsonldConnectionImm.get("belongsToNeed");
    const remoteNeedUri = jsonldConnectionImm.get("hasRemoteNeed");
    const remoteConnectionUri = jsonldConnectionImm.get("hasRemoteConnection");
    const uri = jsonldConnectionImm.get("uri");

    if(!!uri && !!belongsToUri && !!remoteNeedUri){
        parsedConnection.belongsToUri = belongsToUri;
        parsedConnection.data.uri = uri;
        parsedConnection.data.remoteNeedUri = remoteNeedUri;
        parsedConnection.data.remoteConnectionUri = remoteConnectionUri;

        const creationDate = jsonldConnectionImm.get("dct:created"); //THIS IS NOT IN THE DATA
        if(creationDate){
            parsedConnection.data.creationDate = creationDate;
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
            console.error('Cant parse connection, data is an invalid connection-object: ', jsonldConnectionImm.toJS());
            return undefined; //FOR UNKNOWN STATES
        }

        return Immutable.fromJS(parsedConnection);
    }else{
        console.error('Cant parse connection, data is an invalid connection-object: ', jsonldConnectionImm.toJS());
        return undefined;
    }
}

function parseMessage(wonMessage, outgoingMessage, newMessage) {

    let parsedMessage = {
        belongsToUri: undefined,
        data: {
            uri: wonMessage.getMessageUri(),
            text: wonMessage.getTextMessage(),
            date: msStringToDate(wonMessage.getTimestamp()),
            outgoingMessage: outgoingMessage,
            newMessage: !!newMessage,
            connectMessage: wonMessage.isConnectMessage(),
        }
    };

    if(outgoingMessage){
        parsedMessage.belongsToUri = wonMessage.getSender();  
    } else {
        parsedMessage.belongsToUri = wonMessage.getReceiver();
    }
    
    if(
        !parsedMessage.data.uri ||
        !parsedMessage.belongsToUri ||
        !parsedMessage.data.text ||
        !parsedMessage.data.date
    ) {
        console.error('Cant parse chat-message, data is an invalid message-object: ', jsonldMessageImm.toJS());
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
        ownNeed: !!ownNeed,
        isWhatsAround: false,
    };

    if(jsonldNeedImm){
        const uri = jsonldNeedImm.get("@id");
        const nodeUri = jsonldNeedImm.getIn(["won:hasWonNode", "@id"]);
        const isPresent = !!jsonldNeedImm.getIn(["won:is", "dc:title"]);
        const seeksPresent = !!jsonldNeedImm.getIn(["won:seeks", "dc:title"]);
        const is = jsonldNeedImm.get("won:is");
        const seeks = jsonldNeedImm.get("won:seeks");

        const title = isPresent ? is.get("dc:title") : (seeksPresent ? seeks.get("dc:title") : undefined);

        if(!!uri && !!title){
            parsedNeed.uri = uri;
            parsedNeed.title = title;
        }else{
            return undefined;
        }

        /*
        The following code-snippet is solely to determine if the parsed need is a special "whats around"-need,
        in order to do this we have to make sure that the won:hasFlag is checked in two forms, both as a string
        and an immutable object
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

        const creationDate = jsonldNeedImm.get("dct:created");
        if(creationDate){
            parsedNeed.creationDate = creationDate;
        }

        const state = jsonldNeedImm.getIn([won.WON.isInStateCompacted, "@id"]);
        if(state === won.WON.ActiveCompacted){ //we use to check for active state and everything else will be inactive
            parsedNeed.state = state;
        } else {
            parsedNeed.state = won.WON.InactiveCompacted;
        }

        let type = undefined;
        let description = undefined;
        let tags = undefined;
        let location = undefined;

        if(isPresent){
            type = seeksPresent ? won.WON.BasicNeedTypeDotogetherCompacted : won.WON.BasicNeedTypeSupplyCompacted;
            description = is.get("dc:description");
            tags = is.get("won:hasTag");
            location = parseLocation(is.get("won:hasLocation"));
        }else if(seeksPresent){
            type = won.WON.BasicNeedTypeDemandCompacted;
            description = seeks.get("dc:description");
            tags = seeks.get("won:hasTag");
            location = parseLocation(seeks.get("won:hasLocation"));
        }

        parsedNeed.tags = tags ? tags : undefined;
        parsedNeed.description = description ? description : undefined;
        parsedNeed.isWhatsAround = !!isWhatsAround;
        parsedNeed.type = isWhatsAround? won.WON.BasicNeedTypeWhatsAroundCompacted : type;
        parsedNeed.location = location;
        parsedNeed.nodeUri = nodeUri;
    }else{
        console.error('Cant parse need, data is an invalid need-object: ', jsonldNeedImm && jsonldNeedImm.toJS());
        return undefined;
    }

    return Immutable.fromJS(parsedNeed);
}

function parseLocation(jsonldLocation) {
    if(!jsonldLocation) return undefined; //NO LOCATION PRESENT

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
 * @param state to retrieve data from
 * @param connectionUri to find corresponding need for
 */
function selectNeedByConnectionUri(allNeedsInState, connectionUri){
    return allNeedsInState.filter(need =>
        need.getIn(["connections", connectionUri])).first();
}
