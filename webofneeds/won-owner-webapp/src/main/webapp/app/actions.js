/**
 * Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation for their expected payloads.
 */
import { tree2constants, deepFreeze, reduceAndMapTreeKeys } from './utils';
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
    draft: {
        /*
         * A new draft was created (either through the view in this client or on another browser)
         */
        new: null,
        /*
         * A draft has changed. Pass along the draftURI and the respective data.
         */
        changed: {
            type: null,
            title: null,
            thumbnail: null,
        }
    },
    ownpost: {
        new: null,
    },
    moreWub: null
});

/**
 * actionCreators are functions that take the payload and output
 * an action object, thus prebinding the action-type.
 *
 * e.g.:
 *
 * ```javascript
 * function newDraft(draft) {
 *   return { type: 'draft.new', payload: draft }
 * }
 * ```
 */
export const actionCreators = tree2actionCreators(actionTypes);
window.actionCreators = actionCreators;
window.actionTypes = actionTypes;

function tree2actionCreators(obj) {
    return deepFreeze(reduceAndMapTreeKeys(
        (acc, k) => acc.concat(k), //construct paths, e.g. ['draft', 'new']
        (acc) => createActionCreator(won.lookup(actionTypes, acc)), //lookup constant at path
        [], obj
    ))
}
function createActionCreator(type) {
    return (payload) => {
        console.log('creating instance of actionType ', type, ' with payload: ', payload);
        return {type, payload};
    };
}
