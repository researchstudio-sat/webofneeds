/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";

import { actionTypes, actionCreators } from "./actions.js";

import {
  buildConnectMessage,
  buildCloseNeedMessage,
  buildOpenNeedMessage,
  buildDeleteNeedMessage,
  fetchUnloadedData,
  fetchDataForNonOwnedNeedOnly,
} from "../won-message-utils.js";

export function fetchUnloadedNeeds() {
  return async dispatch => {
    fetchUnloadedData(dispatch);
  };
}

export function fetchSuggested(needUri) {
  return async dispatch => {
    fetchDataForNonOwnedNeedOnly(needUri, dispatch);
  };
}

//ownConnectionUri is optional - set if known
export function needsConnect(
  ownedNeedUri,
  ownConnectionUri,
  theirNeedUri,
  connectMessage
) {
  return async (dispatch, getState) => {
    const state = getState();
    const ownedNeed = state.getIn(["needs", ownedNeedUri]);
    const theirNeed = state.getIn(["needs", theirNeedUri]);
    const cnctMsg = await buildConnectMessage({
      ownedNeedUri: ownedNeedUri,
      theirNeedUri: theirNeedUri,
      ownNodeUri: ownedNeed.get("nodeUri"),
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

export function needsClose(needUri) {
  return (dispatch, getState) => {
    buildCloseNeedMessage(
      needUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        dispatch(
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
          })
        );

        //Close all the open connections of the need
        getState()
          .getIn(["needs", needUri, "connections"])
          .map(function(con) {
            if (
              getState().getIn([
                "needs",
                needUri,
                "connections",
                con.get("uri"),
                "state",
              ]) === won.WON.Connected
            ) {
              dispatch(actionCreators.connections__close(con.get("uri")));
            }
          });
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.needs.close,
          payload: {
            ownedNeedUri: needUri,
          },
        })
      );
  };
}

export function needsOpen(needUri) {
  return (dispatch, getState) => {
    buildOpenNeedMessage(
      needUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        dispatch(
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.needs.reopen,
          payload: {
            ownedNeedUri: needUri,
          },
        })
      );
  };
}

export function needsClosedBySystem(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' need in the state - otherwise we'll ignore the message
    const need = getState().getIn(["needs", event.getReceiverNeed()]);
    if (!need) {
      console.debug(
        "ignoring deactivateMessage for a need that is not ours:",
        event.getReceiverNeed()
      );
    }
    dispatch({
      type: actionTypes.needs.closedBySystem,
      payload: {
        needUri: event.getReceiverNeed(),
        humanReadable: need.get("humanReadable"),
        message: event.getTextMessage(),
      },
    });
  };
}

export function needsDelete(needUri) {
  return (dispatch, getState) => {
    buildDeleteNeedMessage(
      needUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        dispatch(
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.needs.delete,
          payload: {
            ownNeedUri: needUri,
          },
        })
      );
  };
}
