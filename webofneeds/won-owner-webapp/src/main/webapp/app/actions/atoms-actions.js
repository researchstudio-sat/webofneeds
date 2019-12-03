/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";

import { actionTypes, actionCreators } from "./actions.js";
import Immutable from "immutable";

import {
  buildConnectMessage,
  buildCloseAtomMessage,
  buildOpenAtomMessage,
  buildDeleteAtomMessage,
} from "../won-message-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as stateStore from "../redux/state-store.js";
import * as ownerApi from "../api/owner-api.js";
import { get } from "../utils.js";

export function fetchUnloadedAtom(atomUri) {
  return (dispatch, getState) =>
    stateStore.fetchAtomAndDispatch(atomUri, dispatch, getState);
}

export function atomsConnectSockets(
  senderSocketUri,
  targetSocketUri,
  connectMessage
) {
  return async dispatch => {
    if (!senderSocketUri) {
      throw new Error("SenderSocketUri not present");
    }

    if (!targetSocketUri) {
      throw new Error("TargetSocketUri not present");
    }

    const cnctMsg = buildConnectMessage({
      connectMessage: connectMessage,
      socketUri: senderSocketUri,
      targetSocketUri: targetSocketUri,
    });

    const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);

    return ownerApi.sendMessage(cnctMsg.message).then(jsonResp => {
      dispatch({
        type: actionTypes.atoms.connectSockets,
        payload: {
          eventUri: jsonResp.messageUri,
          message: jsonResp.message,
          optimisticEvent: optimisticEvent,
          senderSocketUri: senderSocketUri,
          targetSocketUri: targetSocketUri,
        },
      });
    });
  };
}

//ownConnectionUri is optional - set if known
export function atomsConnect(
  ownedAtomUri,
  ownConnectionUri,
  theirAtomUri,
  connectMessage,
  socketType,
  targetSocketType
) {
  return async (dispatch, getState) => {
    const state = getState();
    const ownedAtom = state.getIn(["atoms", ownedAtomUri]);
    const theirAtom = state.getIn(["atoms", theirAtomUri]);

    const socketUri = atomUtils.getSocketUri(ownedAtom, socketType);
    const targetSocketUri = atomUtils.getSocketUri(theirAtom, targetSocketType);

    if (socketType && !socketUri) {
      throw new Error(
        `Atom ${get(ownedAtom, "uri")} does not have a ${socketType}`
      );
    }

    if (targetSocketType && !targetSocketUri) {
      throw new Error(
        `Atom ${get(theirAtom, "uri")} does not have a ${targetSocketType}`
      );
    }

    const cnctMsg = buildConnectMessage({
      connectMessage: connectMessage,
      socketUri: socketUri,
      targetSocketUri: targetSocketUri,
    });
    const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);

    return ownerApi.sendMessage(cnctMsg.message).then(jsonResp => {
      dispatch({
        type: actionTypes.atoms.connect,
        payload: {
          eventUri: jsonResp.messageUri,
          message: jsonResp.message,
          ownConnectionUri: ownConnectionUri,
          optimisticEvent: optimisticEvent,
          socketUri: socketUri,
          targetSocketUri: targetSocketUri,
          atomUri: get(ownedAtom, "uri"),
          targetAtomUri: get(theirAtom, "uri"),
        },
      });
    });
  };
}

export function atomsClose(atomUri) {
  return (dispatch, getState) => {
    buildCloseAtomMessage(atomUri)
      .then(data => ownerApi.sendMessage(data.message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );

        //Close all the open connections of the atom
        getState()
          .getIn(["atoms", atomUri, "connections"])
          .map(conn => {
            if (connectionUtils.isConnected(conn)) {
              dispatch(actionCreators.connections__close(get(conn, "uri")));
            }
          });
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.atoms.close,
          payload: {
            ownedAtomUri: atomUri,
          },
        })
      );
  };
}

export function atomsOpen(atomUri) {
  return dispatch => {
    buildOpenAtomMessage(atomUri)
      .then(data => ownerApi.sendMessage(data.message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.atoms.reopen,
          payload: {
            ownedAtomUri: atomUri,
          },
        })
      );
  };
}

export function atomsClosedBySystem(event) {
  return (dispatch, getState) => {
    //first check if we really have the 'own' atom in the state - otherwise we'll ignore the message
    const atom = getState().getIn(["atoms", event.getAtom()]);
    if (!atom) {
      console.debug(
        "ignoring deactivateMessage for an atom that is not ours:",
        event.getAtom()
      );
    }
    dispatch({
      type: actionTypes.atoms.closedBySystem,
      payload: {
        atomUri: event.getAtom(),
        humanReadable: get(atom, "humanReadable"),
        message: event.getTextMessage(),
      },
    });
  };
}

export function atomsDelete(atomUri) {
  return dispatch => {
    buildDeleteAtomMessage(atomUri)
      .then(data => ownerApi.sendMessage(data.message))
      .then(jsonResp => {
        dispatch(
          actionCreators.messages__send({
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
          })
        );
      })
      .then(() =>
        // assume close went through successfully, update GUI
        dispatch({
          type: actionTypes.atoms.delete,
          payload: Immutable.fromJS({
            uri: atomUri,
          }),
        })
      );
  };
}
