/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";

import { actionTypes } from "./actions.js";

import { buildConnectMessage } from "../won-message-utils.js";

//ownConnectionUri is optional - set if known
export function needsConnect(
  ownNeedUri,
  ownConnectionUri,
  theirNeedUri,
  textMessage
) {
  return async (dispatch, getState) => {
    const state = getState();
    const ownNeed = state.getIn(["needs", ownNeedUri]);
    const theirNeed = state.getIn(["needs", theirNeedUri]);
    const cnctMsg = await buildConnectMessage({
      ownNeedUri: ownNeedUri,
      theirNeedUri: theirNeedUri,
      ownNodeUri: ownNeed.get("nodeUri"),
      theirNodeUri: theirNeed.get("nodeUri"),
      textMessage: textMessage,
      optionalOwnConnectionUri: ownConnectionUri,
    });
    const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
    dispatch({
      type: actionTypes.needs.connect,
      payload: {
        eventUri: cnctMsg.eventUri,
        message: cnctMsg.message,
        ownConnectionUri: ownConnectionUri,
        optimisticEvent: optimisticEvent,
      },
    });
  };
}
