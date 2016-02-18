/**
 * Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation
 * for their expected payloads.
 *
 * # Redux Primer - Actions
 *
 * Actions are small objects like:
 *
 * `{type: 'someaction', payload: {...}}`
 *
 * that are usually created via action-creators (ACs), e.g.:
 *
 * `function someaction(args) { return { type: 'someaction', payload: args }}`
 *
 * and then passed on to the reducer via `redux.dispatch(action)`.
 *
 * *Note:* The calls to `$ngRedux.connect` wrap the ACs in this call to `dispatch`
 *
 * # Best Practices
 *
 * Even though it's possible to have ACs trigger multiple ACs (which is
 * necessary asynchronous actions), try avoiding that. All actions are
 * broadcasted to all reducers anyway.  Mostly it's a symptom of actions
 * that aren't high-level enough. (high-level: `publish`,
 * low-level: `inDraftSetPublishPending`).
 *
 * ACs function is to do simple data-processing that is needed by multiple
 * reducers (e.g. creating the post-publish messages that are needed by
 * the drafts-reducer as well) and dealing with side-effects (e.g. routing,
 * http-calls)
 *
 * As a rule of thumb the lion's share of all processing should happen
 * in the reducers.
 */
import {
    tree2constants,
    deepFreeze,
    reduceAndMapTreeKeys,
    flattenTree,
    delay,
    checkHttpStatus,
    watchImmutableRdxState,
} from '../utils';

import { hierarchy2Creators } from './action-utils';
import { getEventData,setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils';
import { stateGo, stateReload, stateTransitionTo } from 'redux-ui-router';
import { buildCreateMessage } from '../won-message-utils';

import { loadAction } from './load-action';

/**
 * all values equal to this string will be replaced by action-creators that simply
 * passes it's argument on as payload on to the reducers
 */
const INJ_DEFAULT = 'INJECT_DEFAULT_ACTION_CREATOR';
const actionHierarchy = {
    /* actions received as user responses or push notifications */
    load: loadAction, /* triggered on pageload to cause initial crawling of linked-data and other startup tasks*/
    user: {
        /* contains all user-bound data, e.g. ownedPosts,
         * drafts, messages,...
         * This action will likely be caused as a consequence of signing in.
         */
        loggedIn: INJ_DEFAULT,
        loginFailed: INJ_DEFAULT,
        registerFailed: INJ_DEFAULT
    },
    events:{
      fetch:(data)=>dispatch=>{
          data.connectionUris.forEach(function(connection){
              console.log("fetch events of connection: "+connectdionUri)
              won.getEventsOfConnection(connection.connection).then(function(events){
                  console.log(events)
              })
          })
      },

        addUnreadEventUri:INJ_DEFAULT,
        read:INJ_DEFAULT
    },
    matches: {
        load:(data) => (dispatch, getState) => {
            const state = getState();
            for(let needUri in data){
                won.getConnectionInStateForNeedWithRemoteNeed(needUri, "won:Suggested").then(function(results){
                    let needData = state.getIn(['needs', 'ownNeeds', needUri]).toJS();
                    let data = { ownNeed: needData, connections: results };
                    //TODO only one action should be dispatched for every interaction! (reducers should be able to handle arrays)
                    results.forEach(function(entry){
                        dispatch(actionCreators.matches__add(entry))
                    })
                })
            }
        },
        add:INJ_DEFAULT,
    },
    connections:{
        load : (needUris) => dispatch =>{
            needUris.forEach(needUri =>
                won.executeCrawlableQuery(won.queries["getAllConnectionUrisOfNeed"], needUri)
                    .then(function(connectionsOfNeed){
                        console.log("fetching connections");
                        Promise.all(connectionsOfNeed.map(connection => getConnectionRelatedData(
                            connection.need.value,
                            connection.remoteNeed.value,
                            connection.connection.value
                        )))
                        .then(connectionsWithRelatedData =>
                            dispatch({
                                type: actionTypes.connections.load,
                                payload: connectionsWithRelatedData
                            })
                        );
                    })
            );
        },
        open: (connection,message)=>dispatch =>{

        },
        reset:INJ_DEFAULT,
    },
    needs: {
        fetch: (data) => dispatch => {
            const needUris = data.needs;
            const needLookups = needUris.map(needUri => won.getNeed(needUri));
            Promise.all(needLookups).then(needs => {
                console.log("linked data fetched for needs: ", needs );
                dispatch({ type: actionTypes.needs.fetch, payload: needs });
            });

            //TODO get rid of this multiple dispatching here (always push looping back into the reducer)
            dispatch(actionCreators.connections__load(needUris));
            /*
            needUris.forEach(needUri => {
                dispatch(actionCreators.connections__load(needUri));
            });
            */
        },
        received: INJ_DEFAULT,
        connectionsReceived:INJ_DEFAULT,
        clean:INJ_DEFAULT,
        failed: INJ_DEFAULT
    },
    drafts: {
        /*
         * A new draft was created (either through the view in this client or on another browser)
         */
        new: INJ_DEFAULT,
        /*
         * A draft has changed. Pass along the draftURI and the respective data.
         */
        change: {
            type: INJ_DEFAULT,
            title: INJ_DEFAULT,
            thumbnail: INJ_DEFAULT,
        },

        delete: INJ_DEFAULT,

        publish: (draft, nodeUri) => {
            const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);
            return {
                type: actionTypes.drafts.publish,
                payload: { eventUri, message, needUri, draftId: draft.draftId }
            };
        },
        publishSuccessful: INJ_DEFAULT
    },
    router: {
        stateGo,
        stateReload,
        stateTransitionTo
    },
    posts:{
        load:INJ_DEFAULT,
        clean:INJ_DEFAULT
    },
    posts_overview:{
        openPostsView:INJ_DEFAULT
    },

    messages: { /* websocket messages, e.g. post-creation, chatting */
        markAsSent: INJ_DEFAULT,
        /**
         * TODO this action is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
        requestWsReset_Hack: INJ_DEFAULT,
        messageReceived:(data)=>dispatch=> {
            //TODO move this switch-case to the messaging agent
            console.log('messages__messageReceived: ', data)
            getEventData(data).then(event=>{
                console.log('messages__messageReceived: event.hasMessageType === ', event.hasMessageType)
                window.event4dbg = event;
                if(event.hasMessageType === won.WONMSG.successResponseCompacted) {
                    dispatch(actionCreators.messages__successResponseMessageReceived(event))
                }
                else if(event.hasMessageType === won.WONMSG.hintMessageCompacted){
                    dispatch(actionCreators.messages__hintMessageReceived(event))
                }
                else if(event.hasMessageType === won.WONMSG.connectMessageCompacted){
                    dispatch(actionCreators.messages__connectMessageReceived(event))
                }
            })

        },
        successResponseMessageReceived :(event)=>dispatch=>{
            console.log('received response to ', event.isResponseTo, ' of ', event);

            //TODO do all of this in actions.js?
            if (event.isResponseToMessageType === won.WONMSG.createMessageCompacted) {
                console.log("got response for CREATE: " + event.hasMessageType);
                //TODO: if negative, use alternative need URI and send again
                //fetch need data and store in local RDF store
                //get URI of newly created need from message

                //load the data into the local rdf store and publish NeedCreatedEvent when done
                var needURI = event.hasReceiverNeed;
                won.ensureLoaded(needURI).then(
                    function (value) {
                        var eventData = won.clone(event);
                        eventData.eventType = won.EVENT.NEED_CREATED;
                        setCommStateFromResponseForLocalNeedMessage(eventData);
                        eventData.needURI = needURI;
                        won.getNeed(needURI)
                            .then(function(need){

                                console.log("Dispatching action " + won.EVENT.NEED_CREATED);
                                dispatch(actionCreators.drafts__publishSuccessful({
                                    publishEventUri: event.isResponseTo,
                                    needUri: event.hasSenderNeed,
                                    eventData:eventData
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

            }
        },
        connectMessageReceived:(data)=>dispatch=>{
            data.eventType = messageTypeToEventType[data.hasMessageType].eventType;
            //TODO data.hasReceiver, the connectionUri is undefined in the response message
            won.invalidateCacheForNewConnection(data.hasReceiver,data.hasReceiverNeed)
                .then(() => {
                    won.getConnectionWithOwnAndRemoteNeed(data.hasReceiverNeed,data.hasSenderNeed).then(connectionData=>{
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
        },
        hintMessageReceived:(data)=>dispatch=>{
            data.eventType = messageTypeToEventType[data.hasMessageType].eventType;
            won.invalidateCacheForNewConnection(data.hasReceiver,data.hasReceiverNeed)
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
    },

    /*
    runMessagingAgent: () => (dispatch) => {
        //TODO  move here?
        // would require to make sendmsg an actionCreator as well
        // con: aren't stateless functions (then again: the other async-creators aren't either)
        //        - need to share reference to websocket for the send-method
        //        - need to keep internal mq
        // pro: everything that can create actions is listed here
        createWs
        ws.onmessage = parse && dispatch(...)^n
    },
    send = dispatch("pending")
    */



    verifyLogin: () => dispatch => {
        fetch('rest/users/isSignedIn', {credentials: 'include'}) //TODO send credentials along
            .then(checkHttpStatus)
            .then(resp => resp.json())
            /* handle data, dispatch actions */
            .then(data => {
                dispatch(actionCreators.user__loggedIn({loggedIn: true, email: data.username }));
                dispatch(actionCreators.retrieveNeedUris());
            })
            /* handle: not-logged-in */
            .catch(error =>
                dispatch(actionCreators.user__loggedIn({loggedIn: false}))
            );
        ;
    },

    login: (username, password) => (dispatch) =>
        fetch('/owner/rest/users/signin', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({username: username, password: password})
        }).then(checkHttpStatus)
        .then( response => {
            return response.json()
        }).then(
            data => {
                dispatch(actionCreators.user__loggedIn({loggedIn: true, email: username}));
                dispatch(actionCreators.messages__requestWsReset_Hack());
                dispatch(actionCreators.retrieveNeedUris());
                //dispatch(actionCreators.posts__load());
                dispatch(actionCreators.router__stateGo("feed"));
            }
        ).catch(
            error => dispatch(actionCreators.user__loginFailed({loginError: "No such username/password combination registered."}))
        ),
    logout: () => (dispatch) =>
        fetch('/owner/rest/users/signout', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({})
        }).then(checkHttpStatus)
        .then( response => {
            return response.json()
        }).then(
            data => {
                dispatch(actionCreators.messages__requestWsReset_Hack());
                dispatch(actionCreators.user__loggedIn({loggedIn: false}));
                dispatch(actionCreators.needs__clean({needs: {}}));
                dispatch(actionCreators.posts__clean({}));
                dispatch(actionCreators.connections__reset({}))
                dispatch(actionCreators.router__stateGo("landingpage"));
            }
        ).catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
            error => {
                console.log(error);
                dispatch(actionCreators.user__loggedIn({loggedIn : true}))
            }
        ),
    register: (username, password) => (dispatch) =>
        fetch('/owner/rest/users/', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({username: username, password: password})
        }).then(checkHttpStatus)
            .then( response => {
                return response.json()
            }).then(
                data => {
                    dispatch(actionCreators.login(username,password))
/*                    dispatch(actionCreators.user__loggedIn({loggedIn: true, email: username}));
                    dispatch(actionCreators.router__stateGo("createNeed"));*/
                }
        ).catch(
            //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
            error => dispatch(actionCreators.user__registerFailed({registerError: "Registration failed"}))
        ),
    retrieveNeedUris: () => (dispatch) => {
        fetch('/owner/rest/needs/', {
            method: 'get',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include'
        }).then(checkHttpStatus)
            .then(response => {
                return response.json()
            }).then(
                needs => dispatch(actionCreators.needs__fetch({needs: needs}))
        ).catch(
                error => dispatch(actionCreators.needs__failed({error: "user needlist retrieval failed"}))
        )},
    config: {
        /**
         * Anything that is load-once, read-only, global app-config
         * should be initialized in this action. Ideally all of this
         * should be baked-in/prerendered when shipping the code, in
         * future versions => TODO
         */
        init: () => (dispatch) =>
            /* this allows the owner-app-server to dynamically switch default nodes. */
            fetch(/*relativePathToConfig=*/'appConfig/getDefaultWonNodeUri')
                .then(checkHttpStatus)
                .then(resp => resp.json())
                .catch(err => {
                        const defaultNodeUri = `${location.protocol}://${location.host}/won/resource`;
                        console.info(
                            'Failed to fetch default node uri at the relative path `',
                            relativePathToConfig,
                            '` (is the API endpoint there up and reachable?) -> falling back to the default ',
                            defaultNodeUri
                        );
                        return defaultNodeUri;
                })
                .then(defaultNodeUri =>
                    dispatch(actionCreators.config__update({ defaultNodeUri }))
                ),

        update: INJ_DEFAULT,
    }
}

function getConnectionRelatedData(needUri,remoteNeedUri,connectionUri) {
    const remoteNeed = won.getNeed(remoteNeedUri);
    const ownNeed = won.getNeed(needUri);
    const connection = won.getConnection(connectionUri);
    const events = won.getEventsOfConnection(connectionUri, needUri)

    return Promise.all([remoteNeed, ownNeed, connection, events])
        .then(results => ({
            remoteNeed: results[0],
            ownNeed: results[1],
            connection: results[2],
            events: results[3],
        }))

}

var messageTypeToEventType = {};
messageTypeToEventType[won.WONMSG.hintMessageCompacted] = {eventType: won.EVENT.HINT_RECEIVED};
messageTypeToEventType[won.WONMSG.connectMessageCompacted] = {eventType: won.EVENT.CONNECT_RECEIVED};
messageTypeToEventType[won.WONMSG.connectSentMessageCompacted] = {eventType: won.EVENT.CONNECT_SENT}
messageTypeToEventType[won.WONMSG.openMessageCompacted] = {eventType: won.EVENT.OPEN_RECEIVED};
messageTypeToEventType[won.WONMSG.closeMessageCompacted] = {eventType: won.EVENT.CLOSE_RECEIVED};
messageTypeToEventType[won.WONMSG.closeNeedMessageCompacted] = {eventType: won.EVENT.CLOSE_NEED_RECEIVED};
messageTypeToEventType[won.WONMSG.connectionMessageCompacted] = {eventType: won.EVENT.CONNECTION_MESSAGE_RECEIVED};
messageTypeToEventType[won.WONMSG.needStateMessageCompacted] = {eventType: won.EVENT.NEED_STATE_MESSAGE_RECEIVED};
messageTypeToEventType[won.WONMSG.errorMessageCompacted] = {eventType: won.EVENT.NOT_TRANSMITTED }
//as string constans, e.g. actionTypes.drafts.change.type === "drafts.change.type"
export const actionTypes = tree2constants(actionHierarchy);

/**
 * actionCreators are functions that take the payload and output
 * an action object, thus prebinding the action-type.
 * This object follows the structure of the actionTypes-object,
 * but is flattened for use with ng-redux. Thus calling
 * `$ngRedux.dispatch(actionCreators.drafts__new(myDraft))` will trigger an action
 * `{type: actionTypes.drafts.new, payload: myDraft}`
 *
 * e.g.:
 *
 * ```javascript
 * function newDraft(draft) {
 *   return { type: 'draft.new', payload: draft }
 * }
 * ```
 */
export const actionCreators = hierarchy2Creators(actionHierarchy);


/*
 * TODO deletme; for debugging
 */
window.actionCreators4Dbg = actionCreators;
window.actionTypes4Dbg = actionTypes;

