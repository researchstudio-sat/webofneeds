/**
 * Created by ksinger on 23.09.2015.
 *
 * Contains a list of actions to be used with the dispatcher and documentation for their expected payloads.
 */
import { tree2constants } from './utils';

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

export const actionCreators = {
    moreWub : (howMuch) => ({type: actionTypes.moreWub, howMuch}),
}

