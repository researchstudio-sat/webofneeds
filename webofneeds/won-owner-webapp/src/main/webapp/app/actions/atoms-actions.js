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
  fetchUnloadedData,
  fetchDataForNonOwnedAtomOnly,
} from "../won-message-utils.js";
import * as connectionUtils from "../connection-utils.js";
import * as atomUtils from "../atom-utils.js";
import { get } from "../utils.js";

export function fetchUnloadedAtoms() {
  return async dispatch => {
    fetchUnloadedData(dispatch);
  };
}

export function fetchUnloadedAtom(atomUri) {
  return async dispatch => {
    fetchDataForNonOwnedAtomOnly(atomUri, dispatch);
  };
}

//ownConnectionUri is optional - set if known
export function atomsConnect(
  ownedAtomUri,
  ownConnectionUri,
  theirAtomUri,
  connectMessage,
  ownSocketType,
  theirSocketType
) {
  return async (dispatch, getState) => {
    const state = getState();
    const ownedAtom = state.getIn(["atoms", ownedAtomUri]);
    const theirAtom = state.getIn(["atoms", theirAtomUri]);

    const ownSocketUri = atomUtils.getSocketUri(ownedAtom, ownSocketType);
    const theirSocketUri = atomUtils.getSocketUri(theirAtom, theirSocketType);

    if (ownSocketType && !ownSocketUri) {
      throw new Error(
        `Atom ${ownedAtom.get("uri")} does not have a ${ownSocketType}`
      );
    }

    if (theirSocketType && !theirSocketUri) {
      throw new Error(
        `Atom ${theirAtom.get("uri")} does not have a ${theirSocketType}`
      );
    }

    const cnctMsg = await buildConnectMessage({
      ownedAtomUri: ownedAtomUri,
      theirAtomUri: theirAtomUri,
      ownNodeUri: ownedAtom.get("nodeUri"),
      theirNodeUri: theirAtom.get("nodeUri"),
      connectMessage: connectMessage,
      optionalOwnConnectionUri: ownConnectionUri,
      ownSocket: ownSocketUri,
      theirSocket: theirSocketUri,
    });
    const optimisticEvent = await won.wonMessageFromJsonLd(cnctMsg.message);
    dispatch({
      type: actionTypes.atoms.connect,
      payload: {
        eventUri: cnctMsg.eventUri,
        message: cnctMsg.message,
        ownConnectionUri: ownConnectionUri,
        optimisticEvent: optimisticEvent,
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
          type: actionTypes.atoms.delete,
          payload: Immutable.fromJS({
            uri: atomUri,
          }),
        })
      );
  };
}
