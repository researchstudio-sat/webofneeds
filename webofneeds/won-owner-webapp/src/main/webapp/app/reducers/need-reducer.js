/**
 * Created by syim on 11.12.2015.
 */
import { actionTypes } from '../actions/actions';
import Immutable from 'immutable';
import { createReducer } from 'redux-immutablejs'
import won from '../won-es6';
import { msStringToDate } from '../utils';

const initialState = Immutable.fromJS({
    ownNeeds: {},
    theirNeeds: {},
    allNeeds: {},
});

export default function(state = initialState, action = {}) {
    switch(action.type) {
        case actionTypes.logout:
        case actionTypes.needs.clean:
            return initialState;

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
            //TODO needs supplied by this action don't have a list of already associated connections
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
            var updatedNeeds =  action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                state);
            return updatedNeeds;

        case actionTypes.messages.connectMessageReceived:
            const {ownNeedUri, remoteNeed, updatedConnection, connection } = action.payload;
            const stateWithBothNeeds = addNeed(state, remoteNeed, false); // guarantee that remoteNeed is in state
            const stateWithBothNeedsAndConnection = addConnection(stateWithBothNeeds, ownNeedUri, updatedConnection);
            return addConnectionFull(stateWithBothNeedsAndConnection, connection, true);

        case actionTypes.messages.hintMessageReceived:
            return storeConnectionAndRelatedData(state, action.payload);

        //NEW CONNECTIONS STATE UPDATES
        case actionTypes.connections.close:
            return changeConnectionState(state, action.payload.connectionUri, won.WON.Closed);

        case actionTypes.messages.connectMessageReceived:
        case actionTypes.messages.openMessageReceived:
        case actionTypes.messages.hintMessageReceived:
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
            console.log("connectionMessageReceived: ", action.payload.events);
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
            /*var updatedNeeds =  action.payload.reduce(
                (updatedState, connectionWithRelatedData) =>
                    addMessage(updatedState, )storeConnectionAndRelatedData(updatedState, connectionWithRelatedData),
                state);*/
            var loadedEvents = action.payload.get('events');
            //TODO: IMPLEMENT MESSAGE STORING OF MESSAGES
            return state;

        default:
            return state;
    }
}

function storeConnectionAndRelatedData(state, connectionWithRelatedData) {
    const {ownNeed, remoteNeed, connection} = connectionWithRelatedData;
    const stateWithOwnNeed = addNeed(state, ownNeed, true); // guarantee that ownNeed is in state
    const stateWithBothNeeds = addNeed(stateWithOwnNeed, remoteNeed, false); // guarantee that remoteNeed is in state
    const stateWithConnection = addConnection(stateWithBothNeeds, ownNeed["@id"], connection.uri);

    return addConnectionFull(stateWithConnection, connection, false);
}

function addNeed(needs, jsonldNeed, ownNeed) {
    const jsonldNeedImm = Immutable.fromJS(jsonldNeed);
    const mapName = ownNeed? "ownNeeds" : "theirNeeds";

    let newState;
    let parsedNeed = parseNeed(jsonldNeed, ownNeed);

    if(parsedNeed && parsedNeed.get("id")) {
        newState = setIfNew(needs, [mapName, parsedNeed.get("id")], jsonldNeedImm);
        newState = setIfNew(newState, ["allNeeds", parsedNeed.get("id")], parsedNeed);
    } else {
        console.error('Tried to add invalid need-object: ', jsonldNeedImm);
        newState = needs;
    }

    return newState;
}

function parseNeed(jsonldNeed, ownNeed) {
    const jsonldNeedImm = Immutable.fromJS(jsonldNeed);

    let parsedNeed = {id: undefined,
                title: undefined,
                description: undefined,
                type: undefined,
                state: undefined,
                tags: undefined,
                location: undefined,
                connections: Immutable.Map(),
                creationDate: undefined,
                ownNeed};

    if(jsonldNeedImm){
        const id = jsonldNeedImm.get("@id");

        const is = jsonldNeedImm.get("won:is");
        const seeks = jsonldNeedImm.get("won:seeks");

        const title = (is && is.get("dc:title")) ? is.get("dc:title") : ((seeks && seeks.get("dc:title")) ? seeks.get("dc:title") : undefined);

        if(!!id && !!title){
            parsedNeed.id = id;
            parsedNeed.title = title;
        }else{
            return undefined;
        }

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

        if(is){
            type = seeks ? won.WON.BasicNeedTypeDotogetherCompacted : won.WON.BasicNeedTypeSupplyCompacted;
            description = is.get("dc:description");
            tags = is.get("won:hasTag");
        }else if(seeks){
            type = won.WON.BasicNeedTypeDemandCompacted;
            description = seeks.get("dc:description");
            tags = seeks.get("won:hasTag");
        }

        parsedNeed.tags = tags ? tags : undefined;
        parsedNeed.description = description ? description : undefined;
        parsedNeed.type = type;

        //TODO: LOCATION IS STILL MISSING
    }else{
        console.error('Cant parse need, data is an invalid need-object: ', jsonldNeedImm.toJS());
        return undefined;
    }

    return Immutable.fromJS(parsedNeed);
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

function parseConnection(jsonldConnection, newConnection) {
    const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);

    let parsedConnection = {
                                belongsToId: undefined,
                                data: {
                                    id: undefined,
                                    state: undefined,
                                    messages: Immutable.Map(),
                                    remoteNeedId: undefined,
                                    creationDate: undefined,
                                    newConnection
                                }
                            };

    const belongsToId = jsonldConnectionImm.get("belongsToNeed");
    const remoteNeedId = jsonldConnectionImm.get("hasRemoteNeed");
    const id = jsonldConnectionImm.get("uri");

    if(!!id && !!belongsToId && !!remoteNeedId){
        parsedConnection.belongsToId = belongsToId;
        parsedConnection.data.id = id;
        parsedConnection.data.remoteNeedId = remoteNeedId;

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
        belongsToId: undefined,
        data: {
            id: undefined,
            text: undefined,
            date: undefined,
            outgoingMessage,
            newMessage
        }
    };



    if(outgoingMessage){
        parsedMessage.belongsToId = jsonldMessageImm.get("hasSender");
        parsedMessage.data.id = jsonldMessageImm.get("uri");
        parsedMessage.data.text = jsonldMessageImm.get("hasTextMessage");
        parsedMessage.data.date = jsonldMessageImm.get("hasSentTimestamp");
    }else{
        parsedMessage.belongsToId = jsonldMessageImm.getIn(["msg:FromOwner", "hasReceiver"]);
        parsedMessage.data.id = jsonldMessageImm.getIn(["msg:FromOwner", "uri"]);
        parsedMessage.data.text = jsonldMessageImm.getIn(["msg:FromOwner", "hasTextMessage"]);
        parsedMessage.data.date = msStringToDate(jsonldMessageImm.getIn(["msg:FromExternal", "hasReceivedTimestamp"]));
    }

    if(
        !parsedMessage.data.id ||
        !parsedMessage.belongsToId ||
        !parsedMessage.data.text ||
        !parsedMessage.data.date
    ) {
        console.error('Cant parse chat-message, data is an invalid message-object: ', jsonldMessageImm.toJS());
        return undefined;
    } else {
        return Immutable.fromJS(parsedMessage);
    }
}

/**
 * Add's the connectionUri to the needs connections. Makes
 * sure the same uri doesn't get added twice.
 * NOTE: As this function goes through all previous connections
 * to make sure that there are no duplicates, avoid using it
 * when adding a bunch of connections at once.
 * @param state
 * @param needUri
 * @param connectionUri
 * @return {*}
 */
function addConnection(state, needUri, connectionUri) {
    const pathToConnections = ['ownNeeds', needUri, 'won:hasConnections', 'rdfs:member'];

    if(!state.getIn(pathToConnections)) {
        state = state.setIn(pathToConnections, Immutable.List());
    }

    const connections = state.getIn(pathToConnections);
    if( connections.filter(c => c && c.get('@id') === connectionUri).size == 0) {
        // new connection, add it to the need
        state = state.updateIn(
            pathToConnections,
            connections => connections.push(
                Immutable.fromJS({ '@id': connectionUri })
            )
        );
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
    let parsedConnection = parseConnection(connection, newConnection);

    if(parsedConnection){
        const needId = parsedConnection.get("belongsToId");
        let connections = state.getIn(['allNeeds', needId, 'connections']);

        if(connections){
            connections = connections.set(parsedConnection.getIn(["data", "id"]), parsedConnection.get("data"));

            return state.setIn(["allNeeds", needId, "connections"], connections);
        }else{
            console.error("Couldnt add valid connection - missing need data in state", needId);
        }
    }
    return state;
}

function addMessage(state, message, ownMessage, newMessage) {
    let parsedMessage = parseMessage(message, ownMessage, newMessage);

    if(parsedMessage){
        const connectionId = parsedMessage.get("belongsToId");
        let need = getNeedForConnectionUri(state, connectionId);
        if(need){
            let messages = state.getIn(['allNeeds', need.get("id"), "connections", connectionId, "messages"]);
            messages = messages.set(parsedMessage.getIn(["data", "id"]), parsedMessage.get("data"));

            return state.setIn(["allNeeds", need.get("id"), "connections", connectionId, "messages"], messages);
        }
    }
    return state;
}


function setIfNew(state, path, obj){
    return state.updateIn(path, val => val ?
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

    const needUri = need.get("id");

    return state
            .setIn(["allNeeds", needUri, "connections", connectionUri, "state"], newState)
            .setIn(["allNeeds", needUri, "connections", connectionUri, "newConnection"], true);
}

function changeNeedState(state, needUri, newState) {
    return state
        .setIn(["ownNeeds", action.payload.ownNeedUri, 'won:isInState'], newState)
        .setIn(["allNeeds", action.payload.ownNeedUri, "state", newState]);
}

function getNeedForConnectionUri(state, connectionUri){
    let needs = state.get("allNeeds");
    return needs.filter(need => need.get("connections").has(connectionUri)).first();
}