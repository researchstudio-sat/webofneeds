/**
 * Created by ksinger on 18.02.2016.
 */

import Immutable from "immutable";
import { actionCreators, actionTypes } from "./actions.js";
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
    .then(() => loadingWhileSignedIn(dispatch, getState))
    .catch(() => handleNotLoggedIn())
    .then(() => dispatch({ type: actionTypes.initialLoadFinished }));
};

function loadingWhileSignedIn(dispatch, getState) {
  // reset websocket to make sure it's using the logged-in session
  dispatch(actionCreators.reconnect__start());
  return stateStore.fetchOwnedMetaData(dispatch, getState);
}

export const fetchWhatsNew = createdAfterDate => (dispatch, getState) => {
  dispatch({
    type: actionTypes.atoms.fetchWhatsNew,
  });
  return stateStore.fetchWhatsNew(dispatch, getState, createdAfterDate);
};

export const fetchPersonas = () => (dispatch, getState) => {
  dispatch({
    type: actionTypes.atoms.fetchMetaAtoms,
  });
  return stateStore.fetchPersonas(dispatch, getState);
};

export const fetchWhatsAround = (createdAfterDate, location, maxDistance) => (
  dispatch,
  getState
) => {
  dispatch({
    type: actionTypes.atoms.fetchWhatsAround,
  });
  return stateStore.fetchWhatsAround(
    dispatch,
    getState,
    createdAfterDate,
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
