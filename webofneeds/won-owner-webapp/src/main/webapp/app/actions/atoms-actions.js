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
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as accountUtils from "../redux/utils/account-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as stateStore from "../redux/state-store.js";
import * as ownerApi from "../api/owner-api.js";
import { getUri, extractAtomUriBySocketUri } from "../utils.js";
import { ensureLoggedIn } from "./account-actions.js";

export const fetchUnloadedAtom = atomUri => (dispatch, getState) =>
  stateStore.fetchAtomAndDispatch(atomUri, dispatch, getState);

export const fetchUnloadedConnectionsContainer = atomUri => (
  dispatch,
  getState
) =>
  stateStore.fetchConnectionsContainerAndDispatch(atomUri, dispatch, getState);

export const connectSockets = (
  senderSocketUri,
  targetSocketUri,
  connectMessage
) => (dispatch, getState) => {
  if (!senderSocketUri) {
    throw new Error("SenderSocketUri not present");
  }

  if (!targetSocketUri) {
    throw new Error("TargetSocketUri not present");
  }

  const accountState = generalSelectors.getAccountState(getState());
  if (
    accountUtils.isAtomOwned(
      accountState,
      extractAtomUriBySocketUri(senderSocketUri)
    ) &&
    accountUtils.isAtomOwned(
      accountState,
      extractAtomUriBySocketUri(targetSocketUri)
    )
  ) {
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
  } else {
    const cnctMsg = buildConnectMessage({
      connectMessage: connectMessage,
      socketUri: senderSocketUri,
      targetSocketUri: targetSocketUri,
    });

    return ownerApi.sendMessage(cnctMsg).then(jsonResp =>
      won
        .wonMessageFromJsonLd(
          jsonResp.message,
          vocab.WONMSG.uriPlaceholder.event
        )
        .then(wonMessage =>
          dispatch({
            type: actionTypes.atoms.connectSockets,
            payload: {
              eventUri: jsonResp.messageUri,
              message: jsonResp.message,
              optimisticEvent: wonMessage,
              senderSocketUri: senderSocketUri,
              targetSocketUri: targetSocketUri,
            },
          })
        )
    );
  }
};

export const close = atomUri => (dispatch, getState) => {
  buildCloseAtomMessage(atomUri)
    .then(message => ownerApi.sendMessage(message))
    .then(jsonResp => {
      dispatch(
        actionCreators.messages__send({
          eventUri: jsonResp.messageUri,
          message: jsonResp.message,
        })
      );

      const atom = generalSelectors.getAtom(atomUri)(getState());

      //Close all the open connections of the atom
      atomUtils.getConnectedConnections(atom).map(conn => {
        dispatch(actionCreators.connections__close(getUri(conn)));
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

export const open = atomUri => dispatch => {
  buildOpenAtomMessage(atomUri)
    .then(message => ownerApi.sendMessage(message))
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

export const closedBySystem = event => (dispatch, getState) => {
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
      humanReadable: atomUtils.getTitle(
        atom,
        generalSelectors.getExternalDataState(getState())
      ),
      message: event.getTextMessage(),
    },
  });
};

export const deleteAtom = atomUri => dispatch => {
  buildDeleteAtomMessage(atomUri)
    .then(message => ownerApi.sendMessage(message))
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

export const edit = (draft, oldAtom, callback) => (dispatch, getState) =>
  ensureLoggedIn(dispatch, getState).then(() => {
    const { message, atomUri } = buildEditMessage(draft, oldAtom);
    return ownerApi.sendMessage(message).then(jsonResp => {
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
      callback();
    });
  });

export const create = (draft, personaUri, nodeUri) => (dispatch, getState) => {
  const state = getState();

  if (!nodeUri) {
    nodeUri = generalSelectors.getDefaultNodeUri(state);
  }

  return ensureLoggedIn(dispatch, getState).then(() => {
    const { message, atomUri } = buildCreateMessage(draft, nodeUri);

    return ownerApi
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
        const persona = generalSelectors.getAtom(personaUri)(state);
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
