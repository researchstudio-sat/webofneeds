/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions.js';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6.js';
import { msStringToDate } from '../utils.js';

const initialState = Immutable.fromJS({
});

export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
        case actionTypes.needs.clean:
            return initialState;

        case actionTypes.loginStarted:
            // starting a new login process. this could mean switching
            // to a different session. we need to mark any needs
            // that are already loaded as non-owned.
            return state.map(need => need
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
                state
            );
            const stateWithOwnAndTheirNeeds = theirNeeds.reduce(
                (updatedState, theirNeed) => addNeed(updatedState, theirNeed, false),
                stateWithOwnNeeds

            );

            return storeConnectionsData(stateWithOwnAndTheirNeeds, action.payload.get('connections'), false);

        case actionTypes.messages.closeNeed.failed:
            return storeConnectionsData(state, action.payload.get('connections'), false);

        case actionTypes.router.accessedNonLoadedPost:
            return addNeed(state, action.payload.get('theirNeed'), false);

        case actionTypes.needs.fetch:
            return action.payload.reduce(
                (updatedState, ownNeed) => addNeed(updatedState, ownNeed, true),
                state
            );

        case actionTypes.needs.reopen:
            return changeNeedState(state, action.payload.ownNeedUri, won.WON.ActiveCompacted);

        case actionTypes.needs.close:
            return changeNeedState(state, action.payload.ownNeedUri, won.WON.InactiveCompacted);

        case actionTypes.needs.createSuccessful:
            return addNeed(state, action.payload.need, true);

        case actionTypes.connections.load:
            return action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData, false),
                state);

        case actionTypes.messages.connectMessageReceived:
            const {ownNeedUri, ownNeed, remoteNeed, updatedConnection, connection, events } = action.payload;
            const ownNeedFromState = state.get(ownNeedUri);

            const stateWithOwnNeed = ownNeedFromState ? state : addNeed(state, ownNeed, true);
            const stateWithBothNeeds = addNeed(stateWithOwnNeed, remoteNeed, false); // guarantee that remoteNeed is in state
            const stateWithBothNeedsAndConnection = addConnectionFull(stateWithBothNeeds, connection, true);

            let stateWithEverything = stateWithBothNeedsAndConnection;
            if(events){
                events.map(function(event, key){
                    const evnt = Immutable.fromJS(event);
                    if(evnt.get("hasCorrespondingRemoteMessage")) {
                        stateWithEverything = addMessage(stateWithEverything, event, false, true);
                    }
                });
            }

            return stateWithEverything;
        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(state, action.payload, true);

        //NEW CONNECTIONS STATE UPDATES
        case actionTypes.connections.close:
            return changeConnectionState(state, action.payload.connectionUri, won.WON.Closed);

        case actionTypes.messages.openMessageReceived:
            return addConnectionFull(state, action.payload.connection, true);

        case actionTypes.connections.connect: // user has sent a request
            return changeConnectionState(state, action.payload.connectionUri, won.WON.RequestSent);

        case actionTypes.connections.open:
            return changeConnectionState(state,  action.payload.optimisticEvent.hasSender, won.WON.Connected);

        case actionTypes.messages.open.failure:
            return changeConnectionState(state,  action.payload.events['msg:FromSystem'].hasReceiver, won.WON.RequestReceived);

        case actionTypes.messages.connect.success:
            return changeConnectionState(state,  action.payload.hasReceiver, won.WON.RequestSent);

        case actionTypes.messages.close.success:
            return changeConnectionState(state,  action.payload.hasReceiver, won.WON.Closed);

        //NEW MESSAGE STATE UPDATES
        case actionTypes.messages.connectionMessageReceived:
            //ADD RECEIVED CHAT MESSAGES
            //payload; { events }
            return addMessage(state, action.payload.events, false, true);

        case actionTypes.connections.sendChatMessage:
            //ADD SENT TEXT MESSAGE
            /*payload: {
                eventUri: optimisticEvent.uri,
                message,
                optimisticEvent,
             }*/
            console.log("sendChatMessage: ", action.payload.optimisticEvent);
            return addMessage(state, action.payload.optimisticEvent, true, true);

        case actionTypes.connections.showLatestMessages:
        case actionTypes.connections.showMoreMessages:
            var loadedEvents = action.payload.get('events');
            if(loadedEvents){
                state = addMessages(state, loadedEvents);
            }

            return state;

        default:
            return state;
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
        console.error('Tried to add invalid need-object: ', jsonldNeedImm);
        newState = needs;
    }


    return newState;
}


function storeConnectionsData(state, connectionsToStore, newConnections) {
    newConnections = newConnections ? newConnections : Immutable.Set();

    if(connectionsToStore && connectionsToStore.size > 0) {
        connectionsToStore.map(function(connection){
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
    console.log("Adding Full Connection");
    let parsedConnection = parseConnection(connection, newConnection);

    if(parsedConnection){
        console.log("parsedConnection: ", parsedConnection.toJS(), "immutable ", parsedConnection);

        const needUri = parsedConnection.get("belongsToUri");
        let connections = state.getIn([needUri, 'connections']);

        if(connections){
            connections = connections.set(parsedConnection.getIn(["data", "uri"]), parsedConnection.get("data"));

            return state.setIn([needUri, "connections"], connections);
        }else{
            console.error("Couldnt add valid connection - missing need data in state", needUri, "parsedConnection: ", parsedConnection.toJS());
        }
    }else{
        console.log("No connection parsed, add no connection to this state: ", state);
    }
    return state;
}

function addMessage(state, message, outgoingMessage, newMessage) {
    let parsedMessage = parseMessage(message, outgoingMessage, newMessage);

    if(parsedMessage){
        const connectionUri = parsedMessage.get("belongsToUri");
        let need = getNeedForConnectionUri(state, connectionUri); //TODO: can surely be retrieved from the message as well
        if(need){
            let messages = state.getIn([need.get("uri"), "connections", connectionUri, "messages"]);
            messages = messages.set(parsedMessage.getIn(["data", "uri"]), parsedMessage.get("data"));

            return state.setIn([need.get("uri"), "connections", connectionUri, "messages"], messages);
        }
    }
    return state;
}

function addMessages(state, messages) {
    if(messages && messages.size > 0){

        messages.map(function(message, key){
            const outgoingMessage = !!message.get("hasTextMessage");
            const incomingMessage = !!message.getIn(["hasCorrespondingRemoteMessage", "hasTextMessage"]);

            if(outgoingMessage || incomingMessage){
                //ONLY HANDLE TEXTMESSAGES
                state = addMessage(state, message, outgoingMessage, true);
            }
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
    const need = getNeedForConnectionUri(state, connectionUri);

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

function getNeedForConnectionUri(state, connectionUri){
    return state.filter(need => need.get("connections").has(connectionUri)).first();
}

function parseConnection(jsonldConnection, newConnection) {
    const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);
    console.log("Connection to parse: ", jsonldConnectionImm.toJS());

    let parsedConnection = {
        belongsToUri: undefined,
        data: {
            uri: undefined,
            state: undefined,
            messages: Immutable.Map(),
            remoteNeedUri: undefined,
            creationDate: undefined,
            newConnection: !!newConnection,
        }
    };

    const belongsToUri = jsonldConnectionImm.get("belongsToNeed");
    const remoteNeedUri = jsonldConnectionImm.get("hasRemoteNeed");
    const uri = jsonldConnectionImm.get("uri");

    if(!!uri && !!belongsToUri && !!remoteNeedUri){
        parsedConnection.belongsToUri = belongsToUri;
        parsedConnection.data.uri = uri;
        parsedConnection.data.remoteNeedUri = remoteNeedUri;

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

function parseMessage(jsonldMessage, outgoingMessage, newMessage) {
    const jsonldMessageImm = Immutable.fromJS(jsonldMessage);

    let parsedMessage = {
        belongsToUri: undefined,
        data: {
            uri: undefined,
            text: undefined,
            date: undefined,
            outgoingMessage: outgoingMessage,
            newMessage: !!newMessage,
            connectMessage: false,
        }
    };

    if(outgoingMessage){
        parsedMessage.belongsToUri = jsonldMessageImm.get("hasSender");
        parsedMessage.data.uri = jsonldMessageImm.get("uri");
        parsedMessage.data.text = jsonldMessageImm.get("hasTextMessage");
        parsedMessage.data.date = msStringToDate(jsonldMessageImm.get("hasSentTimestamp"));
    }else{
        const fromOwner = jsonldMessageImm.get("msg:FromOwner");

        if(fromOwner){
            //If message is received directly
            parsedMessage.belongsToUri = fromOwner.get("hasReceiver");
            parsedMessage.data.uri = fromOwner.get("uri");
            parsedMessage.data.text = fromOwner.get("hasTextMessage");
            parsedMessage.data.date = msStringToDate(jsonldMessageImm.getIn(["msg:FromExternal", "hasReceivedTimestamp"]));
        }else{
            const fromCorrespondingMessage = jsonldMessageImm.get("hasCorrespondingRemoteMessage");

            if(fromCorrespondingMessage){
                //if message comes within the events of showLatestMessages/showMoreMessages action
                parsedMessage.belongsToUri = jsonldMessageImm.get("hasReceiver");
                parsedMessage.data.uri = fromCorrespondingMessage.get("uri");
                parsedMessage.data.text = fromCorrespondingMessage.get("hasTextMessage");
                parsedMessage.data.date = msStringToDate(jsonldMessageImm.getIn(["hasReceivedTimestamp"]));

                if(fromCorrespondingMessage.get("hasMessageType") === won.WONMSG.connectMessage){
                    parsedMessage.data.connectMessage = true;
                }
            }
        }
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
    }else{
        console.error('Cant parse need, data is an invalid need-object: ', jsonldNeedImm.toJS());
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
