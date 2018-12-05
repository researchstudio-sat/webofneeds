/**
 * Created by ksinger on 18.02.2016.
 */

import Immutable from "immutable";
import { actionTypes, actionCreators } from "./actions.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";

import { checkAccessToCurrentRoute } from "../configRouting.js";

import { checkLoginStatus } from "../won-utils.js";

import {
  fetchOwnedData,
  fetchDataForNonOwnedNeedOnly,
} from "../won-message-utils.js";

export const pageLoadAction = () => (dispatch, getState) => {
  /* TODO the data fetched here should be baked into
    * the send html thus significantly improving the
    * initial page-load-speed.
    * TODO fetch config data here as well
    */

  checkLoginStatus()
    /* handle data, dispatch actions */
    .then(data =>
      dispatch({
        type: actionTypes.account.store,
        payload: Immutable.fromJS(data),
      })
    )
    .then(data => {
      return loadingWhileSignedIn(dispatch, getState, data);
    })
    .catch(() => {
      return loadingWhileSignedOut(dispatch, getState);
    });
};

function loadingWhileSignedIn(dispatch, getState, data) {
  loginSuccess(dispatch, getState);
  fetchOwnedData(data.username, dispatch).then(() =>
    dispatch({
      type: actionTypes.initialLoadFinished,
    })
  );
}

function loginSuccess(dispatch, getState) {
  // reset websocket to make sure it's using the logged-in session
  dispatch(actionCreators.reconnect__start());
  checkAccessToCurrentRoute(dispatch, getState);
}

function loadingWhileSignedOut(dispatch, getState) {
  let dataPromise;
  const state = getState();
  const postUri = getPostUriFromRoute(state);
  if (postUri && !state.getIn(["needs", postUri])) {
    //got an uri but no post loaded yet
    dataPromise = fetchDataForNonOwnedNeedOnly(postUri);
  } else {
    dataPromise = Promise.resolve(Immutable.Map());
  }
  return dataPromise
    .then(publicData =>
      dispatch({
        type: actionTypes.needs.storeTheirs,
        payload: publicData,
      })
    )
    .then(() =>
      dispatch({
        type: actionTypes.initialLoadFinished,
      })
    )
    .then(() => checkAccessToCurrentRoute(dispatch, getState));
}

/////////// THE ACTIONCREATORS BELOW SHOULD BE PART OF PAGELOAD
