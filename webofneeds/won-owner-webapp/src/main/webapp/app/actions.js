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
        select: injDefault,
        publish: injDefault,
        notifyOfSuccessfulPublish: injDefault //triggered by server
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
        console.log('creating instance of actionType ', type, ' with payload: ', payload);
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

