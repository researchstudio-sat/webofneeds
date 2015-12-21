/**
 * Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation for their expected payloads.
 */
import {
    tree2constants,
    deepFreeze,
    reduceAndMapTreeKeys,
    flattenTree,
    delay,
    checkHttpStatus
} from '../utils';

import { hierarchy2Creators } from './action-utils';

import { stateGo, stateReload, stateTransitionTo } from 'redux-ui-router';
import { buildCreateMessage } from '../won-message-utils';

//all values equal to this string will be replaced by action-creatos that simply
// passes it's argument on as payload on to the reducers
const INJ_DEFAULT = 'INJECT_DEFAULT_ACTION_CREATOR';
const actionHierarchy = {
    /* actions received as responses or push notifications */
    user: {
        /* contains all user-bound data, e.g. ownedPosts,
         * drafts, messages,...
         * This action will likely be caused as a consequence of signing in.
         */
        receive: INJ_DEFAULT,
        loginFailed: INJ_DEFAULT,
        registerFailed: INJ_DEFAULT
    },
    needs: {
        receive: INJ_DEFAULT,
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

        publish: (draft, nodeUri) => (dispatch) => {
            const { message, eventUri, needUri } = buildCreateMessage(draft, nodeUri);
            dispatch({
                type: actionTypes.drafts.publish,
                payload: { eventUri, message, needUri, draftId: draft.draftId }
            });
        },
    },
    router: {
        stateGo,
        stateReload,
        stateTransitionTo
    },


    messages: { /* websocket messages, e.g. post-creation, chatting */
        markAsSent: INJ_DEFAULT,
        markAsSuccess: INJ_DEFAULT,
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

    moreWub: INJ_DEFAULT,
    /*
     * This action creator uses thunk (https://github.com/gaearon/redux-thunk) which
     * allows using it with a normal dispatch(actionCreator(payload)) even though
     *  it does asynchronous calls. This is a requirement for using it with
     *  $ngRedux.connect(..., actionCreators, ...)
     */
    delayedWub : (nrOfWubs, milliseconds = 1000) => (dispatch) =>
        delay(milliseconds).then(
                args => dispatch(actionCreators.moreWub(nrOfWubs)),
                error => console.err('actions.js: Error while delaying for delayed Wub.')
        ),

    verifyLogin: () => dispatch => {
        fetch('rest/users/isSignedIn', {credentials: 'include'}) //TODO send credentials along
            .then(checkHttpStatus)
            .then(resp => resp.json())
            /* handle data, dispatch actions */
            .then(data => {
                dispatch(actionCreators.user__receive({loggedIn: true, email: data.username }));
                dispatch(actionCreators.retrieveNeedUris());
            })
            /* handle: not-logged-in */
            .catch(error =>
                dispatch(actionCreators.user__receive({loggedIn: false}))
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
                dispatch(actionCreators.user__receive({loggedIn: true, email: username}));
                dispatch(actionCreators.retrieveNeedUris());
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
                dispatch(actionCreators.user__receive({loggedIn: false}));
                dispatch(actionCreators.needs__receive({needs: {}}));
            }
        ).catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
            error => {
                console.log(error);
                dispatch(actionCreators.user__receive({loggedIn : true}))
            }
        ),
    register: (username, password, passwordAgain) => (dispatch) =>
        fetch('/owner/rest/users/', {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({username: username, password: password, passwordAgain: passwordAgain})
        }).then(checkHttpStatus)
            .then( response => {
                return response.json()
            }).then(
                data => dispatch(actionCreators.login(username, password))
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
                needs => dispatch(actionCreators.needs__receive({needs: needs}))
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

