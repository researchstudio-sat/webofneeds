/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import vocab from "../service/vocab.js";

import { actionTypes, actionCreators } from "./actions.js";
import Immutable from "immutable";

import {
  buildConnectMessage,
  buildCloseAtomMessage,
  buildOpenAtomMessage,
  buildDeleteAtomMessage,
  buildCreateMessage,
  buildEditMessage,
} from "../won-message-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as stateStore from "../redux/state-store.js";
import * as ownerApi from "../api/owner-api.js";
import { get, getIn } from "../utils.js";
import * as accountUtils from "../redux/utils/account-utils";
import { ensureLoggedIn } from "./account-actions.js";

export function fetchUnloadedAtom(atomUri) {
  return (dispatch, getState) =>
    stateStore.fetchAtomAndDispatch(atomUri, dispatch, getState);
}

export function connectSocketsServerSide(senderSocketUri, targetSocketUri) {
  return () => {
    if (!senderSocketUri) {
      throw new Error("SenderSocketUri not present");
    }

    if (!targetSocketUri) {
      throw new Error("TargetSocketUri not present");
    }

    return ownerApi
      .serverSideConnect(senderSocketUri, targetSocketUri)
      .then(async response => {
        if (!response.ok) {
          const errorMsg = await response.text();
          throw new Error(
            `Could not connect sockets(${senderSocketUri}<->${targetSocketUri}): ${errorMsg}`
          );
        }
      });
  };
}

export function connectSockets(
  senderSocketUri,
  targetSocketUri,
  connectMessage
) {
  return dispatch => {
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

    return won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent =>
      ownerApi.sendMessage(cnctMsg.message).then(jsonResp => {
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
      })
    );
  };
}

export function connectSocketTypes(
  senderAtomUri,
  targetAtomUri,
  senderSocketType,
  targetSocketType,
  connectMessage
) {
  return (dispatch, getState) => {
    const state = getState();
    const senderAtom = getIn(state, ["atoms", senderAtomUri]);
    const targetAtom = getIn(state, ["atoms", targetAtomUri]);

    const senderSocketUri = atomUtils.getSocketUri(
      senderAtom,
      senderSocketType
    );
    const targetSocketUri = atomUtils.getSocketUri(
      targetAtom,
      targetSocketType
    );

    if (!senderSocketUri) {
      throw new Error(
        `Atom ${get(senderAtom, "uri")} does not have a ${senderSocketType}`
      );
    }

    if (!targetSocketUri) {
      throw new Error(
        `Atom ${get(targetAtom, "uri")} does not have a ${targetSocketType}`
      );
    }

    const cnctMsg = buildConnectMessage({
      connectMessage: connectMessage,
      socketUri: senderSocketUri,
      targetSocketUri: targetSocketUri,
    });

    return won.wonMessageFromJsonLd(cnctMsg.message).then(optimisticEvent =>
      ownerApi.sendMessage(cnctMsg.message).then(jsonResp => {
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
      })
    );
  };
}

export function close(atomUri) {
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

export function open(atomUri) {
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

export function closedBySystem(event) {
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

export function deleteAtom(atomUri) {
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

export function edit(draft, oldAtom) {
  return (dispatch, getState) => {
    const state = getState();

    let prevParams = getIn(state, ["router", "prevParams"]);

    if (
      !accountUtils.isLoggedIn(get(state, "account")) &&
      prevParams.privateId
    ) {
      /*
       * `ensureLoggedIn` will generate a new privateId. should
       * there be a previous privateId, we don't want to change
       * back to that later.
       */
      prevParams = Object.assign({}, prevParams);
      delete prevParams.privateId;
    }

    return ensureLoggedIn(dispatch, getState).then(async () => {
      const { message, atomUri } = await buildEditMessage(draft, oldAtom);

      ownerApi.sendMessage(message).then(jsonResp => {
        dispatch({
          type: actionTypes.atoms.edit,
          payload: {
            eventUri: jsonResp.messageUri,
            message: jsonResp.message,
            atomUri: atomUri,
            atom: draft,
            oldAtom,
          },
        });

        dispatch(actionCreators.router__back());
      });
    });
  };
}

export function create(draft, personaUri, nodeUri) {
  return (dispatch, getState) => {
    const state = getState();

    if (!nodeUri) {
      nodeUri = getIn(state, ["config", "defaultNodeUri"]);
    }

    let prevParams = getIn(state, ["router", "prevParams"]);

    if (
      !accountUtils.isLoggedIn(get(state, "account")) &&
      prevParams.privateId
    ) {
      /*
             * `ensureLoggedIn` will generate a new privateId. should
             * there be a previous privateId, we don't want to change
             * back to that later.
             */
      prevParams = Object.assign({}, prevParams);
      delete prevParams.privateId;
    }

    return ensureLoggedIn(dispatch, getState).then(async () => {
      const { message, atomUri } = await buildCreateMessage(draft, nodeUri);

      ownerApi
        .sendMessage(message)
        .then(jsonResp => {
          dispatch({
            type: actionTypes.atoms.create,
            payload: {
              eventUri: jsonResp.messageUri,
              message: jsonResp.message,
              atomUri: atomUri,
              atom: draft,
            },
          });
        })
        .then(() => {
          const persona = getIn(state, ["atoms", personaUri]);
          if (persona) {
            const senderSocketUri = atomUtils.getSocketUri(
              persona,
              vocab.HOLD.HolderSocketCompacted
            );
            const targetSocketUri = `${atomUri}#holdableSocket`;

            return ownerApi
              .serverSideConnect(senderSocketUri, targetSocketUri, false, true)
              .then(async response => {
                if (!response.ok) {
                  const errorMsg = await response.text();
                  throw new Error(`Could not connect identity: ${errorMsg}`);
                }
              });
          }
        });
    });
  };
}
