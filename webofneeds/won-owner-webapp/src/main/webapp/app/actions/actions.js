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

import  won from '../won-es6';

// <utils>

import {
    tree2constants,
    deepFreeze,
    reduceAndMapTreeKeys,
    flattenTree,
    delay,
    checkHttpStatus,
    watchImmutableRdxState,
    entries,
} from '../utils';
import { hierarchy2Creators } from './action-utils';
import { getEventsFromMessage,setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils';
import {
    buildCreateMessage,
    buildCloseNeedMessage,
    buildOpenNeedMessage
} from '../won-message-utils';

import {
    selectConnectionUris,
} from '../won-utils';

// </utils>

// <action-creators>

import {
    accountLogin,
    accountLogout,
    accountRegister,
} from './account-actions';

import * as cnct from './connections-actions';
import * as messages from './messages-actions';

import {
    configInit,
    pageLoadAction
} from './load-action';
import { matchesLoad } from './matches-actions';
import { stateGo, stateReload, stateTransitionTo } from 'redux-ui-router';

// </action-creators>


/**
 * all values equal to this string will be replaced by action-creators that simply
 * passes it's argument on as payload on to the reducers
 */
const INJ_DEFAULT = 'INJECT_DEFAULT_ACTION_CREATOR';
const actionHierarchy = {
    initialPageLoad: pageLoadAction,
    events:{
        addUnreadEventUri:INJ_DEFAULT,
        read:INJ_DEFAULT
    },
    matches: {
        load: matchesLoad,
        add:INJ_DEFAULT,
    },
    connections:{
        fetch: cnct.connectionsFetch,
        open: cnct.connectionsOpen,
        connect: cnct.connectionsConnect,
        close: cnct.connectionsClose,
        rate: cnct.connectionsRate,
        typedAtChatMessage: INJ_DEFAULT,
        sendChatMessage: cnct.connectionsChatMessage,
        showLatestMessages: cnct.showLatestMessages,
        showMoreMessages: cnct.showMoreMessages,
        reset:INJ_DEFAULT,
    },
    needs: {
        received: INJ_DEFAULT,
        connectionsReceived:INJ_DEFAULT,
        clean:INJ_DEFAULT,
        reopen: needsOpen,
        close: needsClose,
        failed: INJ_DEFAULT
    },
    drafts: {
        new: INJ_DEFAULT, // A new draft was created (either through the view in this client or on another browser)
        change: { // A draft has changed. Pass along the draftURI and the respective data.
            type: INJ_DEFAULT,
            title: INJ_DEFAULT,
            description: INJ_DEFAULT,
            tags: INJ_DEFAULT,
            thumbnail: INJ_DEFAULT,
            location: INJ_DEFAULT,
        },

        delete: INJ_DEFAULT,
        publish: draftsPublish,
        publishSuccessful: INJ_DEFAULT
    },
    router: {
        stateGo,
        stateReload,
        stateTransitionTo,
        back: stateBack,
        accessedNonLoadedPost: INJ_DEFAULT, //dispatched in configRouting.js
    },
    posts:{
        load:INJ_DEFAULT,
        clean:INJ_DEFAULT
    },
    posts_overview:{
        openPostsView:INJ_DEFAULT
    },

    /**
     * Server triggered interactions (aka received messages)
     */
    messages: { /* websocket messages, e.g. post-creation, chatting */
        //TODO get rid of send and rename to receivedMessage

        send: INJ_DEFAULT, //TODO this should be part of proper, user-story-level actions (e.g. need.publish or sendCnctMsg)

        /*
         * posting things to the server should be optimistic and assume
         * success that is rolled back in case of a failure or timeout.
         */

        create: {
            success: messages.successfulCreate,
            //TODO failure: messages.failedCreate
        },
        open: {
            successRemote: INJ_DEFAULT, //2nd successResponse
            successOwn: INJ_DEFAULT, //1st successResponse
            failure: INJ_DEFAULT,
            //TODO failure: messages.failedOpen
        },
        close: { //TODO: NAME SEEMS GENERIC EVEN THOUGH IT IS ONLY USED FOR CLOSING CONNECITONS; REFACTOR THIS SOMEDAY
            success: messages.successfulCloseConnection,
            //TODO failure: messages.failedClose
        },
        connect: {
            //success: messages.successfulConnect,
            successRemote: INJ_DEFAULT, //2nd successResponse
            successOwn: INJ_DEFAULT, //1st successResponse
            failure: INJ_DEFAULT,
        },
        chatMessage: {
            //success: messages.successfulChatMessage,
            successRemote: INJ_DEFAULT, //2nd successResponse
            successOwn: INJ_DEFAULT, //1st successResponse
            failure: INJ_DEFAULT,
        },
        closeNeed: {
            success: messages.successfulCloseNeed,
            failure: messages.failedCloseNeed
        },
        connectionMessageReceived: INJ_DEFAULT,
        connectMessageReceived: messages.connectMessageReceived,
        hintMessageReceived: messages.hintMessageReceived,
        openMessageReceived: messages.openMessageReceived,

        waitingForAnswer: INJ_DEFAULT,
    },
    hideLogin: INJ_DEFAULT,
    showLogin: INJ_DEFAULT,
    login: accountLogin,
    logout: accountLogout,
    register: accountRegister,
    loginFailed: INJ_DEFAULT,
    loginReset: INJ_DEFAULT,
    registerReset: INJ_DEFAULT,
    registerFailed: INJ_DEFAULT,

    lostConnection: INJ_DEFAULT,
    reconnect: INJ_DEFAULT,
    reconnectSuccess: INJ_DEFAULT,

    toasts: {
        delete: INJ_DEFAULT,
        test: INJ_DEFAULT
    },
    config: {
        init: configInit,
        update: INJ_DEFAULT,
    },
    tick: startTicking,

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

}

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



//////////// STUFF THAT SHOULD BE IN OTHER FILES BELOW //////////////////

export function startTicking() {
    return (dispatch) =>
        setInterval(() =>
            dispatch({ type: actionTypes.tick, payload: Date.now() }),
            60000
        );
}


export function draftsPublish(draft, nodeUri) {
    const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);
    return {
        type: actionTypes.drafts.publish,
        payload: { eventUri, message, needUri, draftId: draft.draftId }
    };
}

/**
 * @deprecated used for keeping old code.
 * @param needUri
 * @param remoteNeedUri
 * @param connectionUri
 * @return {*}
 */
export function getConnectionRelatedData(needUri, remoteNeedUri, connectionUri) {
    const remoteNeed = won.getTheirNeed(remoteNeedUri);
    const ownNeed = won.getNeedWithConnectionUris(needUri);
    const connection = won.getConnectionWithEventUris(connectionUri, { requesterWebId: needUri });
    const events = won.getEventsOfConnection(connectionUri, { requesterWebId: needUri })
        .then(eventsLookup => {
            const eventList = [];
            for (let [uri, event] of entries(eventsLookup)) {
                eventList.push(event);
            }
            return eventList;
        });

    return Promise.all([remoteNeed, ownNeed, connection, events])
        .then(results => ({
            remoteNeed: results[0],
            ownNeed: results[1],
            connection: results[2],
            events: results[3],
        }));
}

export function needsOpen(needUri) {
    return (dispatch, getState) => {
        const state = getState();
        buildOpenNeedMessage(
            needUri,
            getState().getIn(['config', 'defaultNodeUri'])
        )
            .then((data)=> {
                console.log(data);
                dispatch(actionCreators.messages__send({
                    eventUri: data.eventUri,
                    message: data.message
                }));
            })
            .then(() =>
                // assume close went through successfully, update GUI
                dispatch({
                    type: actionTypes.needs.reopen,
                    payload: {
                        ownNeedUri: needUri,
                        affectedConnections: selectConnectionUris(
                                getState().getIn(['needs', 'ownNeeds', needUri])
                            )
                    }
                })
        )
    }
}

export function needsClose(needUri) {
    return (dispatch, getState) => {
        buildCloseNeedMessage(
            needUri,
            getState().getIn(['config', 'defaultNodeUri'])
        )
        .then((data)=> {
            console.log(data);
            dispatch(actionCreators.messages__send({
                eventUri: data.eventUri,
                message: data.message
            }));
        })
        .then(() =>
            // assume close went through successfully, update GUI
            dispatch({
                type: actionTypes.needs.close,
                payload: {
                    ownNeedUri: needUri,
                    affectedConnections: selectConnectionUris(
                        getState().getIn(['needs', 'ownNeeds', needUri])
                    )
                }
            })
        )
        .then(() =>
            // go back to overview
            dispatch(actionCreators.router__stateGo('overviewPosts'))
        )
    }
}

/**
 * Action-Creator that goes back in the browser history
 * without leaving the app.
 * @param dispatch
 * @param getState
 */
function stateBack() {
    return (dispatch, getState) => {
        const hasPreviousState = !!getState().getIn(['router', 'prevState', 'name']);
        if (hasPreviousState) {
            history.back();
        } else {
            dispatch(actionCreators.router__stateGo('landingpage'));
        }
    }
}
