/**
 * Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation for their expected payloads.
 */
import { tree2constants, deepFreeze, reduceAndMapTreeKeys, flattenTree } from './utils';
import './service/won';

export const actionTypes = tree2constants({

    /* actions received as responses or push notifications */
    received: {
        /* contains all user-bound data, e.g. ownedPosts,
         * drafts, messages,...
         * This action will likely be caused as a consequence of signing in.
         */
        userData : null
    },
    drafts: {
        /*
         * A new draft was created (either through the view in this client or on another browser)
         */
        new: null,
        /*
         * A draft has changed. Pass along the draftURI and the respective data.
         */
        change: {
            type: null,
            title: null,
            thumbnail: null,
        },
        delete: null,
        select: null,
        publish: null,
        notifyOfSuccessfulPublish: null //triggered by server
    },
    moreWub: null
});

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
export const actionCreators = flattenTree(
        reduceAndMapTreeKeys(
            (acc, k) => acc.concat(k), //construct paths, e.g. ['draft', 'new']
            (acc) => createActionCreator(won.lookup(actionTypes, acc)), //lookup constant at path
            [], actionTypes
        ),
    '__');

/*
 * This action creator uses thunk (https://github.com/gaearon/redux-thunk) which
 * allows using it with a normal dispatch(actionCreator(payload)) even though
 *  it does asynchronous calls. This is a requirement for using it with
 *  $ngRedux.connect(..., actionCreators, ...)
 */
actionCreators.delayedWub = (nrOfWubs, milliseconds = 1000) => (dispatch) =>
    delay(milliseconds).then(
        args => dispatch(actionCreators.moreWub(nrOfWubs)),
        error => console.err('actions.js: Error while delaying for delayed Wub.')
    );

import { stateGo, stateReload, stateTransitionTo } from 'redux-ui-router';
actionCreators.router__stateGo = stateGo;
actionCreators.router__stateReload = stateReload;
actionCreators.router__stateTransitionTo = stateTransitionTo;

window.delay = delay;
function delay(milliseconds) {
    return new Promise((resolve, reject) =>
        window.setTimeout(() => resolve(), milliseconds)
    );
}

function createActionCreator(type) {
    return (payload) => {
        console.log('creating instance of actionType ', type, ' with payload: ', payload);
        return {type, payload};
    };
}

/*
 * NOTE: Add any custom action-creators here!
 */

Object.freeze(actionCreators); //to make sure it's not modified elsewhere


/*
 * TODO deletme; for debugging
 */
window.actionCreators = actionCreators;
window.actionTypes = actionTypes;

