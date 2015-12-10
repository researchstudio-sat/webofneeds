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
        failed: INJ_DEFAULT
    },
    needs: {
        receive: INJ_DEFAULT,
        failed: INJ_DEFAULT,
        clear: INJ_DEFAULT
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

        // use this action creator (drafts__publish__call) to initiate the process
        //publish: publishDraft, //async dispatch
        publish: INJ_DEFAULT,
        setPublish: {
            // the following three are triggered (a)synchronously to cause state-updates
            pending: INJ_DEFAULT, //triggered by `publish`-action creator
            successful: INJ_DEFAULT, //triggered by server-callback
            failed: INJ_DEFAULT, //triggered by server-callback
        }
    },
    router: {
        stateGo,
        stateReload,
        stateTransitionTo
    },


    messages: {
        /* TODO this fragment is part of an attempt to sketch a different
         * approach to asynchronity (Remove it or the thunk-based
         * solution afterwards)
         */
        enqueue: INJ_DEFAULT,
        markAsSent: INJ_DEFAULT,
        receive: INJ_DEFAULT,
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
            .then(data =>
                dispatch(actionCreators.user__receive({
                    loggedIn: true,
                    email: data.username
                }))
            )
            /* handle: not-logged-in */
            .catch(error =>
                dispatch(actionCreators.user__receive({
                    loggedIn: false
                }))
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
                dispatch(actionCreators.retrieveNeedUris({username: username, password: password}));
                //retrieveNeedUris(username, password, dispatch);
            }
        ).catch(
            error => dispatch(actionCreators.user__failed({error: "No such username/password combination registered."}))
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
                dispatch(actionCreators.needs__clear({}));
            }
        ).catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
            error => dispatch(actionCreators.user__receive({loggedIn : true}))
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
                data => dispatch(actionCreators.user__receive({loggedIn: true, email: username}))
        ).catch(
            //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
                error => dispatch(actionCreators.user__failed({error: "Passwords do not match"}))
        ),
    retrieveNeedUris: (username, password) => (dispatch) =>
        retrieveNeedUris(username, password, dispatch),
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

function retrieveNeedUris(username, password, dispatch) {
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
            error => {
                console.log(error);
                dispatch(actionCreators.needs__failed({error: "user needlist retrieval failed"}))
            }
    )
}

/* WORK IN PROGRESS */
//import wonServiceFoo from './service/won-service';//testfile';//won-service';
//window.wonServiceFoo = wonServiceFoo;
function publishDraft(draftId)  {
    return (dispatch) => { //using the thunk-middleware for asynchronity

        /*
         * TODO: get access to state? (or pass whole draft info along with draftId?)
         */

        /*
         *  TODO get access to services required for asyncPublish
         *
         *  - remove angular dependencies from message & linked data service (or move most
         *  of their stuff to a seperate file and only retain a thin shell for compatibility
         *  with the old app)
         *   -> merge first with yanas work!!!
         *
         *  - publish the basic angular services globally (thus introducing a tight
         *  coupling of angular into the new code)
         *  - republish the service methods / make service publish itself (thus only
         *  introducing a single point of coupling)
         *
         *  - wrap actions.js and reducers.js in angular-services
         *
         *  - use commonjs/amd export -> doesn't work due to jspm loading everything
         *    when 'won.owner' isn't yet defined.
         *
         *  - have reexport.js that loads angular and all services and exports them
         *    -> probably will have same problem
         */

        const PLACEHOLDER = {};


        //dispatch({type: actionTypes.drafts.publish, payload: undefined})
        dispatch(actionCreators.drafts__setPublish__pending(PLACEHOLDER));

        const draft = PLACEHOLDER.get(draftId);
        asyncPublish(draft).then(
                args => dispatch(actionCreators.drafts__setPublish__successful(args[PLACEHOLDER])),
                args => dispatch(actionCreators.drafts__setPublish__failed(args[PLACEHOLDER]))
        )
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

