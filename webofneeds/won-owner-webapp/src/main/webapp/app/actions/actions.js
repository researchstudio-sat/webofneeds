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
import { getEventData,setCommStateFromResponseForLocalNeedMessage } from '../won-message-utils';
import {
    buildCreateMessage,
} from '../won-message-utils';

// </utils>

// <action-creators>

import {
    accountLogin,
    accountLogout,
    accountRegister,
    accountVerifyLogin
} from './account-actions';
import {
    messagesConnectMessageReceived,
    messagesHintMessageReceived,
    messagesSuccessResponseMessageReceived
} from './messages-actions';
import {
    connectionsClose,
    connectionsConnect,
    connectionsFetch,
    connectionsLoad,
    connectionsOpen,
    connectionsRate
} from './connections-actions';

import { loadAction, retrieveNeedUris, configInit, needsFetch } from './load-action';
import { matchesLoad } from './matches-actions';
import { stateGo, stateReload, stateTransitionTo } from 'redux-ui-router';

// </action-creators>


/**
 * all values equal to this string will be replaced by action-creators that simply
 * passes it's argument on as payload on to the reducers
 */
const INJ_DEFAULT = 'INJECT_DEFAULT_ACTION_CREATOR';
const actionHierarchy = {
    load: loadAction, /* triggered on pageload to cause initial crawling of linked-data and other startup tasks*/
    user: {
        loggedIn: INJ_DEFAULT,
        loginFailed: INJ_DEFAULT,
        registerFailed: INJ_DEFAULT
    },
    events:{
        addUnreadEventUri:INJ_DEFAULT,
        read:INJ_DEFAULT
    },
    matches: {
        load: matchesLoad,
        add:INJ_DEFAULT,
    },
    connections:{
        fetch: connectionsFetch,
        load: connectionsLoad,
        open: connectionsOpen,
        connect: connectionsConnect,
        accepted: INJ_DEFAULT,
        denied: INJ_DEFAULT,
        close: connectionsClose,
        rate: connectionsRate,
        sendOpen:INJ_DEFAULT,
        add:INJ_DEFAULT,
        reset:INJ_DEFAULT,
    },
    needs: {
        fetch: needsFetch,
        received: INJ_DEFAULT,
        connectionsReceived:INJ_DEFAULT,
        clean:INJ_DEFAULT,
        failed: INJ_DEFAULT
    },
    drafts: {
        new: INJ_DEFAULT, // A new draft was created (either through the view in this client or on another browser)
        change: { // A draft has changed. Pass along the draftURI and the respective data.
            type: INJ_DEFAULT,
            title: INJ_DEFAULT,
            thumbnail: INJ_DEFAULT,
        },

        delete: INJ_DEFAULT,
        publish: draftsPublish,
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
        send: INJ_DEFAULT,
        waitingForAnswer: INJ_DEFAULT,
        /**
         * TODO this action is part of the session-upgrade hack documented in:
         * https://github.com/researchstudio-sat/webofneeds/issues/381#issuecomment-172569377
         */
        requestWsReset_Hack: INJ_DEFAULT,
        successResponseMessageReceived: messagesSuccessResponseMessageReceived,
        connectMessageReceived: messagesConnectMessageReceived,
        hintMessageReceived: messagesHintMessageReceived,
    },
    verifyLogin: accountVerifyLogin,
    login: accountLogin,
    logout: accountLogout,
    register: accountRegister,
    retrieveNeedUris: retrieveNeedUris,
    config: {
        init: configInit,

        update: INJ_DEFAULT,
    }

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


export function draftsPublish(draft, nodeUri) {
    const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);
    return {
        type: actionTypes.drafts.publish,
        payload: { eventUri, message, needUri, draftId: draft.draftId }
    };
}

/**
 * @deprecated i need to delete this again *
 * @param needUri
 * @param remoteNeedUri
 * @param connectionUri
 * @return {*}
 */
export function getConnectionRelatedData(needUri,remoteNeedUri,connectionUri) {
    const remoteNeed = won.getNeed(remoteNeedUri);
    const ownNeed = won.getNeed(needUri);
    const connection = won.getConnection(connectionUri);
    const events = won.getEventsOfConnection(connectionUri, needUri)
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

export const messageTypeToEventType = deepFreeze({
    [won.WONMSG.hintMessageCompacted] : {eventType: won.EVENT.HINT_RECEIVED},
    [won.WONMSG.connectMessageCompacted] : {eventType: won.EVENT.CONNECT_RECEIVED},
    [won.WONMSG.connectSentMessageCompacted] : {eventType: won.EVENT.CONNECT_SENT},
    [won.WONMSG.openMessageCompacted] : {eventType: won.EVENT.OPEN_RECEIVED},
    [won.WONMSG.closeMessageCompacted] : {eventType: won.EVENT.CLOSE_RECEIVED},
    [won.WONMSG.closeNeedMessageCompacted] : {eventType: won.EVENT.CLOSE_NEED_RECEIVED},
    [won.WONMSG.connectionMessageCompacted] : {eventType: won.EVENT.CONNECTION_MESSAGE_RECEIVED},
    [won.WONMSG.needStateMessageCompacted] : {eventType: won.EVENT.NEED_STATE_MESSAGE_RECEIVED},
    [won.WONMSG.errorMessageCompacted] : {eventType: won.EVENT.NOT_TRANSMITTED }
});
