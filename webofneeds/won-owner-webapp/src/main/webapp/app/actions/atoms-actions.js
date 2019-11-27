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
        `Atom ${ownedAtom.get("uri")} does not have a ${socketType}`
      );
    }

    if (targetSocketType && !targetSocketUri) {
      throw new Error(
        `Atom ${theirAtom.get("uri")} does not have a ${targetSocketType}`
      );
    }

    const cnctMsg = await buildConnectMessage({
      ownedAtomUri: ownedAtomUri,
      theirAtomUri: theirAtomUri,
      ownNodeUri: ownedAtom.get("nodeUri"),
      theirNodeUri: theirAtom.get("nodeUri"),
      connectMessage: connectMessage,
      optionalOwnConnectionUri: ownConnectionUri,
      socketUri: socketUri,
      targetSocketUri: targetSocketUri,
    });
    const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
    //TODO: WRAP /rest/messages/send POST AROUND
    dispatch({
      type: actionTypes.atoms.connect,
      payload: {
        eventUri: cnctMsg.eventUri,
        message: cnctMsg.message,
        ownConnectionUri: ownConnectionUri,
        optimisticEvent: optimisticEvent,
        socketUri: socketUri,
        targetSocketUri: targetSocketUri,
      },
    });
  };
}

export function atomsClose(atomUri) {
  return (dispatch, getState) => {
    buildCloseAtomMessage(
      atomUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        //TODO: WRAP /rest/messages/send POST AROUND
        dispatch(
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
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
  return (dispatch, getState) => {
    buildOpenAtomMessage(
      atomUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
      .then(data => {
        dispatch(
          //TODO: WRAP /rest/messages/send POST AROUND
          actionCreators.messages__send({
            eventUri: data.eventUri,
            message: data.message,
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
    const atom = getState().getIn(["atoms", event.getRecipientAtom()]);
    if (!atom) {
      console.debug(
        "ignoring deactivateMessage for an atom that is not ours:",
        event.getRecipientAtom()
      );
    }
    dispatch({
      type: actionTypes.atoms.closedBySystem,
      payload: {
        atomUri: event.getRecipientAtom(),
        humanReadable: atom.get("humanReadable"),
        message: event.getTextMessage(),
      },
    });
  };
}

export function atomsDelete(atomUri) {
  return (dispatch, getState) => {
    buildDeleteAtomMessage(
      atomUri,
      getState().getIn(["config", "defaultNodeUri"])
    )
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
