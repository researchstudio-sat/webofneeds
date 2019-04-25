/**
 * Created by ksinger on 18.02.2016.
 */

import Immutable from "immutable";
import { actionTypes, actionCreators } from "./actions.js";

import { checkAccessToCurrentRoute } from "../configRouting.js";

import * as wonUtils from "../won-utils.js";
import * as wonMessageUtils from "../won-message-utils.js";

export const pageLoadAction = () => (dispatch, getState) => {
  wonUtils
    .checkLoginStatus()
    /* handle data, dispatch actions */
    .then(data =>
      dispatch({
        type: actionTypes.account.store,
        payload: Immutable.fromJS(data),
      })
    )
    .then(() => {
      return loadingWhileSignedIn(dispatch);
    })
    .catch(() => handleNotLoggedIn()) //do not remove this line
    .then(() => checkAccessToCurrentRoute(dispatch, getState))
    .then(() => dispatch({ type: actionTypes.initialLoadFinished }));
};

function loadingWhileSignedIn(dispatch) {
  // reset websocket to make sure it's using the logged-in session
  dispatch(actionCreators.reconnect__start());
  return wonMessageUtils.fetchOwnedData(dispatch);
}

export const fetchWhatsNew = () => (dispatch, getState) => {
  dispatch({
    type: actionTypes.atoms.fetchWhatsNew,
  });
  return wonMessageUtils.fetchWhatsNew(dispatch, getState);
};

export const fetchWhatsAround = () => (dispatch, getState) => {
  dispatch({
    type: actionTypes.atoms.fetchWhatsAround,
  });
  return wonMessageUtils.fetchWhatsAround(dispatch, getState);
};
/*
 Simply prints a logline and resolves the promise so we can go on in the chain
*/
function handleNotLoggedIn() {
  console.debug("No User Logged in yet, continuing with the initialLoad");
  return Promise.resolve();
}
