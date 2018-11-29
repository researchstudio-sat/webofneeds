/**
 * Created by ksinger on 18.02.2016.
 */

import Immutable from "immutable";
import won from "../won-es6.js";
import { actionTypes, actionCreators } from "./actions.js";
import { getPostUriFromRoute } from "../selectors/general-selectors.js";

import { checkAccessToCurrentRoute } from "../configRouting.js";

import { stateGoCurrent } from "./cstm-router-actions.js";

import {
  checkLoginStatus,
  privateId2Credentials,
  login,
} from "../won-utils.js";

import { getParameterByName } from "../utils.js";

import {
  fetchOwnedData,
  fetchDataForNonOwnedNeedOnly,
  wellFormedPayload,
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
        type: actionTypes.login,
        payload: Immutable.fromJS(data).merge({ loggedIn: true }),
      })
    )
    .then(data => {
      return loadingWhileSignedIn(dispatch, getState, data);
    })
    .catch(() => {
      // as this is one of the first action-creators to be executed, we need to get the param directly from the url-bar instead of `state.getIn(['router','currentParams','privateId'])`
      const privateId =
        getParameterByName("privateId") || localStorage.getItem("privateId");

      if (privateId) {
        return loadingWithAnonymousAccount(dispatch, getState, privateId).catch(
          () => loadingWhileSignedOut(dispatch, getState)
        );
      } else {
        /*
         * ok, we're really not logged in -- thus we need to fetch any publicly visible, required data
         */
        return loadingWhileSignedOut(dispatch, getState);
      }
    });
};

function loadingWhileSignedIn(dispatch, getState, data) {
  loginSuccess(data.username, true, dispatch, getState);
  fetchOwnedData(data.username, dispatchInitialPageLoad(dispatch)).then(() =>
    dispatch({
      type: actionTypes.initialPageLoad,
      payload: Immutable.fromJS({ initialLoadFinished: true }),
    })
  );
}

function loadingWithAnonymousAccount(dispatch, getState, privateId) {
  // using an anonymous account. need to log in.
  const { email } = privateId2Credentials(privateId);
  return (
    login({ privateId })
      /* quickly dispatch log-in status, even before loading data, to
     * allow making correct access-control decisions
     */
      .then(response => {
        loginSuccess(email, true, dispatch, getState);
        return response;
      })
      .then(() => fetchOwnedData(email, dispatchInitialPageLoad(dispatch)))
      .then(() => {
        return dispatch({
          type: actionTypes.initialPageLoad,
          payload: Immutable.fromJS({ initialLoadFinished: true }),
        });
      })
      .catch(e => {
        console.error(
          "failed to sign-in with privateId ",
          privateId,
          " because of: ",
          e
        );
        dispatch({
          type: actionTypes.user.loginFailed,
          payload: {
            loginError: Immutable.fromJS(won.PRIVATEID_NOT_FOUND_ERROR),
            credentials: { privateId },
          },
        });
        throw e;
      })
      .then(() => {
        dispatch(stateGoCurrent({ privateId }));
      })
  );
  //dispatch(actionCreators.login(email, password));
  //return; // the login action should fetch the required data, so we're done here.
}

function loginSuccess(username, loginStatus, dispatch, getState) {
  // reset websocket to make sure it's using the logged-in session
  dispatch(actionCreators.reconnect__start());

  /* quickly dispatch log-in status, even before loading data, to
     * allow making correct access-control decisions
     */
  dispatchInitialPageLoad(dispatch)({ email: username, loggedIn: loginStatus });

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
        type: actionTypes.initialPageLoad,
        payload: publicData.merge({ initialLoadFinished: true }),
      })
    )
    .then(() => checkAccessToCurrentRoute(dispatch, getState));
}

function dispatchInitialPageLoad(dispatch) {
  return payload =>
    dispatch({
      type: actionTypes.initialPageLoad,
      payload: wellFormedPayload(payload),
    });
}

/////////// THE ACTIONCREATORS BELOW SHOULD BE PART OF PAGELOAD
