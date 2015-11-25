/**
 * Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation for their expected payloads.
 */
import { tree2constants, deepFreeze, reduceAndMapTreeKeys, flattenTree } from './utils';
import './service/won';

import { stateGo, stateReload, stateTransitionTo } from 'redux-ui-router';

const injDefault = 'INJECT_DEFAULT_ACTION_CREATOR';
const actionHierarchy = {
    /* actions received as responses or push notifications */
    received: {
        /* contains all user-bound data, e.g. ownedPosts,
         * drafts, messages,...
         * This action will likely be caused as a consequence of signing in.
         */
        userData : injDefault
    },
    drafts: {
        /*
         * A new draft was created (either through the view in this client or on another browser)
         */
        new: injDefault,
        /*
         * A draft has changed. Pass along the draftURI and the respective data.
         */
        change: {
            type: injDefault,
            title: injDefault,
            thumbnail: injDefault,
        },
        delete: injDefault,

        // use this action creator (drafts__publish__call) to initiate the process
        publish: publishDraft,
        setPublish: {
            // the following three are triggered (a)synchronously to cause state-updates
            pending: injDefault, //triggered by `publish`-action creator
            successful: injDefault, //triggered by server-callback
            failed: injDefault, //triggered by server-callback
        }
    },
    router: {
        stateGo,
        stateReload,
        stateTransitionTo
    },
    moreWub: injDefault,
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
export const actionCreators = Object.freeze(flattenTree(
        reduceAndMapTreeKeys(
            (path, k) => path.concat(k), //construct paths, e.g. ['draft', 'new']
            (path) => {
                /* leaf can either be a defined creator or a
                 * placeholder asking to generate one.
                 */
                const potentialCreator = won.lookup(actionHierarchy, path);
                if(typeof potentialCreator === 'function') {
                    return potentialCreator; //already a defined creator. hopefully.
                } else {
                    const type = won.lookup(actionTypes, path);
                    return createActionCreator(type);
                }
            }, [], actionHierarchy
        ),
    '__'));

function createActionCreator(type) {
    return (payload) => {
        console.debug('creating instance of actionType ', type, ' with payload: ', payload);
        return {type, payload};
    };
}

function delay(milliseconds) {
    return new Promise((resolve, reject) =>
        window.setTimeout(() => resolve(), milliseconds)
    );
}

/*
 * TODO deletme; for debugging
 */
window.actionCreators4Dbg = actionCreators;
window.actionTypes4Dbg = actionTypes;

