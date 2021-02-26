/**
 * Created by ksinger on 19.02.2016.
 */
import { actionTypes, actionCreators } from "./actions.js";
import Immutable from "immutable";

import {
  buildCloseAtomMessage,
  buildOpenAtomMessage,
  buildDeleteAtomMessage,
  buildCreateMessage,
  buildEditMessage,
} from "../won-message-utils.js";
import {
  connectHolderToCreatedAtomUri,
  connectAtomSockets,
} from "~/app/actions/connections-actions";
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
) => (dispatch, getState) =>
  connectAtomSockets(
    senderSocketUri,
    targetSocketUri,
    connectMessage,
    accountUtils.isAtomOwned(
      generalSelectors.getAccountState(getState()),
      extractAtomUriBySocketUri(targetSocketUri)
    ),
    dispatch
  );

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

export const closedBySystem = wonMessage => (dispatch, getState) => {
  //first check if we really have the 'own' atom in the state - otherwise we'll ignore the message
  const atom = getState().getIn(["atoms", wonMessage.getAtom()]);
  if (!atom) {
    console.debug(
      "ignoring deactivateMessage for an atom that is not ours:",
      wonMessage.getAtom()
    );
  }
  dispatch({
    type: actionTypes.atoms.closedBySystem,
    payload: {
      atomUri: wonMessage.getAtom(),
      humanReadable: atomUtils.getTitle(
        atom,
        generalSelectors.getExternalDataState(getState())
      ),
      message: wonMessage.getTextMessage(),
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

export const createAtomFromDraftAndDispatch = (
  atomDraft,
  nodeUri,
  dispatch
) => {
  const { message, atomUri } = buildCreateMessage(atomDraft, nodeUri);

  return ownerApi
    .sendMessage(message)
    .then(jsonResp => {
      dispatch({
        type: actionTypes.atoms.create,
        payload: {
          eventUri: jsonResp.messageUri,
          message: jsonResp.message,
          atomUri: atomUri,
          atom: atomDraft,
        },
      });
    })
    .then(() => atomUri);
};

export const create = (draft, personaUri, nodeUri) => (dispatch, getState) => {
  const state = getState();

  if (!nodeUri) {
    nodeUri = generalSelectors.getDefaultNodeUri(state);
  }

  const holder = generalSelectors.getAtom(personaUri)(getState());

  if (personaUri && !holder) {
    console.warn(
      "Could not find holder with Uri: ",
      personaUri,
      ", holder not be stored in the state"
    );
  }

  return ensureLoggedIn(dispatch, getState).then(() => {
    return createAtomFromDraftAndDispatch(draft, nodeUri, dispatch).then(
      atomUri => connectHolderToCreatedAtomUri(holder, atomUri)
    );
  });
};
