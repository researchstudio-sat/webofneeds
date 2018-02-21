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

//ownConnectionUri is optional - set if known
export function needsConnect(ownNeedUri, ownConnectionUri, theirNeedUri, textMessage) {
    return async (dispatch, getState) => {
    	
        const state = getState();
        const ownNeed = state.getIn(["needs", ownNeedUri]);
        const theirNeed = state.getIn(["needs", theirNeedUri]);
        let theirNodeUri = null;
        if (theirNeed){
        	theirNodeUri = theirNeed.get("nodeUri");
        } else {
        	theirNodeUri = await won.getNode(theirNeedUri).hasWonNode;
        } 
        const cnctMsg = await buildConnectMessage({
            ownNeedUri: ownNeedUri,
            theirNeedUri: theirNeedUri,
            ownNodeUri: ownNeed.get("nodeUri"),
            theirNodeUri: theirNeed.get("nodeUri"),
            textMessage: textMessage,
            optionalOwnConnectionUri: ownConnectionUri
        });
        const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
        dispatch({
            type: actionTypes.needs.connect,
            payload: {
                eventUri: cnctMsg.eventUri,
                message: cnctMsg.message,
                ownConnectionUri: ownConnectionUri,
                optimisticEvent: optimisticEvent,
            }
        });
    }
}



