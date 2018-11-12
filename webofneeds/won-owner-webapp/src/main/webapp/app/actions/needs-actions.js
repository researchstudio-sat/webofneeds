/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import Immutable from "immutable";

import { actionTypes } from "./actions.js";

import {
  buildConnectMessage,
  fetchUnloadedData,
  fetchDataForNonOwnedNeedOnly,
} from "../won-message-utils.js";

export function fetchUnloadedNeeds() {
  return async dispatch => {
    const curriedDispatch = payload => {
      dispatch({
        type: actionTypes.needs.fetchUnloadedNeeds,
        payload: payload,
      });
    };
    fetchUnloadedData(curriedDispatch);
  };
}

export function fetchSuggested(needUri) {
  return async dispatch => {
    fetchDataForNonOwnedNeedOnly(needUri).then(response => {
      const suggestedPosts = response && response.get("theirNeeds");

      if (suggestedPosts && suggestedPosts.size > 0) {
        const payload = Immutable.fromJS({
          suggestedPosts: suggestedPosts,
        });

        dispatch({
          type: actionTypes.needs.fetchSuggested,
          payload: payload,
        });
      }
    });
  };
}

//ownConnectionUri is optional - set if known
export function needsConnect(
  ownNeedUri,
  ownConnectionUri,
  theirNeedUri,
  connectMessage
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
      connectMessage: connectMessage,
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
