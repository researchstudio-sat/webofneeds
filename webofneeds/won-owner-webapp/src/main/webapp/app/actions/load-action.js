/**
 * Created by ksinger on 18.02.2016.
 */

import Immutable from "immutable";
import { actionCreators, actionTypes } from "./actions.js";

import { checkAccessToCurrentRoute } from "../configRouting.js";

import * as ownerApi from "../api/owner-api.js";
import * as stateStore from "../redux/state-store.js";

export const pageLoadAction = () => (dispatch, getState) => {
  ownerApi
    .checkLoginStatus()
    /* handle data, dispatch actions */
    .then(data =>
      dispatch({
        type: actionTypes.account.store,
        payload: Immutable.fromJS(data),
      })
    )
    .then(() => {
      return loadingWhileSignedIn(dispatch, getState);
    })
    .catch(() => handleNotLoggedIn()) //do not remove this line
    .then(() => checkAccessToCurrentRoute(dispatch, getState))
    .then(() => dispatch({ type: actionTypes.initialLoadFinished }));
};

function loadingWhileSignedIn(dispatch, getState) {
  // reset websocket to make sure it's using the logged-in session
  dispatch(actionCreators.reconnect__start());
  return stateStore.fetchOwnedData(dispatch, getState);
}

export const fetchWhatsNew = modifiedAfterDate => (dispatch, getState) => {
  dispatch({
    type: actionTypes.atoms.fetchWhatsNew,
  });
  return stateStore.fetchWhatsNew(dispatch, getState, modifiedAfterDate);
};

export const fetchPersonas = () => (dispatch, getState) => {
  dispatch({
    type: actionTypes.atoms.fetchMetaAtoms,
  });
  return stateStore.fetchPersonas(dispatch, getState);
};

export const fetchWhatsAround = (modifiedAfterDate, location, maxDistance) => (
  dispatch,
  getState
) => {
  dispatch({
    type: actionTypes.atoms.fetchWhatsAround,
  });
  return stateStore.fetchWhatsAround(
    dispatch,
    getState,
    modifiedAfterDate,
    location,
    maxDistance
  );
};
/*
 Simply prints a logline and resolves the promise so we can go on in the chain
*/
function handleNotLoggedIn() {
  console.debug("No User Logged in yet, continuing with the initialLoad");
  return Promise.resolve();
}
