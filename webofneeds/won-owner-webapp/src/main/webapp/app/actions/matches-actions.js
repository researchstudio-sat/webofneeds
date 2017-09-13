/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6.js';
import { actionCreators, actionTypes } from './actions.js';

export function matchesLoad(data) {
    return (dispatch, getState) => {
        const state = getState();
        for (let needUri in data) {
            won.getConnectionInStateForNeedWithRemoteNeed(needUri, "won:Suggested").then(function (results) {
                let needData = state.getIn(['needs', needUri]).toJS();
                let data = {ownNeed: needData, connections: results};
                //TODO only one action should be dispatched for every interaction! (reducers should be able to handle arrays)
                results.forEach(function (entry) {
                    console.log("add match: ",entry);
                    dispatch(actionCreators.matches__add(entry))
                })
            })
        }
    }
}

