/**
 * Created by ksinger on 19.02.2016.
 */

import  won from '../won-es6.js';
import Immutable from 'immutable';

import {
    is,
    urisToLookupMap,
    msStringToDate,
    getIn,
    get,
    jsonld2simpleFormat,
    cloneAsMutable,
    delay,
} from '../utils.js';


import {
    actionTypes,
    actionCreators,
} from './actions.js';

import {
    buildConnectMessage,
} from '../won-message-utils.js';


export function needsConnect(event) {
    return async (dispatch, getState) => {
    	const ownNeedUri = event.needUri;
    	const theirNeedUri = event.remoteNeedUri;
    	const textMessage = event.textMessage;
    	
    	
        const state = getState();
        console.log("executing CONNECT NEEDS action");
        const ownNeed = getState().getIn(["needs", ownNeedUri]);
        const theirNeed = getState().getIn(["needs", theirNeedUri]);
        let theirNodeUri = null;
        if (theirNeed){
        	theirNodeUri = theirNeed.get("nodeUri");
        } else {
        	theirNodeUri = await won.getNode(theirNeedUri).hasWonNode;
        } 
        const cnctMsg = await buildConnectMessage(ownNeed.get("uri"), theirNeedUri, ownNeed.get("nodeUri"), theirNeed.get("nodeUri"), textMessage);

        dispatch(actionCreators.messages__send({eventUri: cnctMsg.eventUri, message: cnctMsg.message}));

        const optimisticEvent = await won.toWonMessage(cnctMsg.message);

        dispatch({
            type: actionTypes.needs.connect,
            payload: {
                eventUri: cnctMsg.eventUri,
                optimisticEvent,
            }
        });
    }
}



