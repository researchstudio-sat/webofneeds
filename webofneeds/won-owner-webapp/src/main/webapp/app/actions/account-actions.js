/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import Immutable from "immutable";
import { actionTypes, actionCreators } from "./actions.js";
import { fetchOwnedData } from "../won-message-utils.js";
import {
  registerAccount,
  transferPrivateAccount,
  login,
  logout,
  parseCredentials,
  generatePrivateId,
  checkLoginStatus,
} from "../won-utils.js";
import {
  clearPrivateId,
  savePrivateId,
  setDisclaimerAccepted,
} from "../won-localstorage.js";
import { stateGoCurrent } from "./cstm-router-actions.js";
import { checkAccessToCurrentRoute } from "../configRouting.js";

import { getIn } from "../utils.js";
import { getOwnedConnectionUris } from "../selectors/connection-selectors.js";
import { loadLatestMessagesOfConnection } from "./connections-actions.js";

/**
 * Makes sure user is either logged in
 * or creates a private-ID account as fallback.
 */
export async function ensureLoggedIn(dispatch, getState) {
  const state = getState();
  if (state.getIn(["user", "loggedIn"])) {
    return;
  }

  const privateId = generatePrivateId();
  try {
    await accountRegister({ privateId })(dispatch, getState);
  } catch (err) {
    console.error(
      `Creating temporary account (${privateId}) has failed due to `,
      err
    );
    dispatch(actionCreators.registerFailed({ privateId }));
  }
}

let _loginInProcessFor;
/**
 *
 * @param username
 * @param password
 * @param options
 *    * fetchData(true): whether or not to fetch a users owned needs. If the account
 *    signing in is new, there's no need to fetch this and `false` can be passed here
 *    * doRedirects(true): whether or not to do any redirects at all (e.g. if an invalid route was accessed)
 *    * redirectToFeed(false): whether or not to redirect to the feed after signing in (needs `redirects` to be true)
 *    * relogIfNecessary(true):  if there's a valid session or privateId, log out from that first.
 *
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountLogin(credentials, options) {
  const options_ = Object.assign(
    {
      // defaults
      fetchData: true,
      doRedirects: true,
      redirectToFeed: false,
      relogIfNecessary: true, // if there's a valid session or privateId, log out from that first.
    },
    options
  );
  return (dispatch, getState) => {
    const state = getState();

    const { email } = parseCredentials(credentials);

    const prevPrivateId = getIn(state, [
      "router",
      "currentParams",
      "privateId",
    ]);
    const prevEmail = state.getIn(["user", "email"]);

    const wasLoggedIn =
      state.get("initialLoadFinished") && (prevPrivateId || prevEmail);

    if (
      state.get("loginInProcessFor") === email ||
      _loginInProcessFor === email
    ) {
      console.debug(
        "Already logging in as ",
        email,
        ". Canceling redundant attempt."
      );
      return;
    }

    if (
      state.get("initialLoadFinished") &&
      ((credentials.privateId && credentials.privateId === prevPrivateId) ||
        (credentials.email && credentials.email === prevEmail))
    ) {
      console.debug(
        "Already logged into this account (" +
          (credentials.privateId || credentials.email) +
          "). Aborting second login attempt."
      );
      return;
    }

    const curriedDispatch = data =>
      dispatch({
        type: actionTypes.login,
        payload: Immutable.fromJS(data).merge({ email: email, loggedIn: true }),
      });

    return Promise.resolve()
      .then(() => {
        _loginInProcessFor = email;
        return dispatch({
          type: actionTypes.loginStarted,
          payload: { email },
        });
      })
      .then(() => {
        if (wasLoggedIn) {
          return logout().then(() => {
            if (
              options_.doRedirects &&
              getIn(state, ["router", "currentParams", "privateId"])
            ) {
              return stateGoCurrent({ privateId: "" })(dispatch, getState);
            }
          });
        }
      })
      .then(() => {
        if (options_.doRedirects && credentials.privateId) {
          return stateGoCurrent({ privateId: credentials.privateId })(
            dispatch,
            getState
          );
        }
      })
      .then(() => login(credentials))
      .then(data =>
        dispatch({
          type: actionTypes.login,
          payload: Immutable.fromJS(data).merge({
            email: email,
            loggedIn: true,
          }),
        })
      )
      .then(() => curriedDispatch({ httpSessionUpgraded: true }))
      .then(() => {
        if (!options_.doRedirects) {
          return;
        } else if (options_.redirectToFeed) {
          return dispatch(
            actionCreators.router__stateGoResetParams("connections")
          );
        } else {
          return checkAccessToCurrentRoute(dispatch, getState);
        }
      })
      .then((/*response*/) => {
        if (options_.fetchData) {
          return fetchOwnedData(email, curriedDispatch);
        } else {
          return Immutable.Map(); // only need to fetch data for non-new accounts
        }
      })
      .then(() => curriedDispatch({ loginFinished: true }))
      .catch(error =>
        error.response.json().then(loginError => {
          return Promise.resolve()
            .then(() => {
              if (wasLoggedIn) {
                return dispatch({
                  type: actionTypes.logout,
                  payload: Immutable.fromJS({ loggedIn: false }),
                });
              }
            })
            .then(() => {
              if (credentials.privateId) {
                loginError = won.PRIVATEID_NOT_FOUND_ERROR;
              }

              dispatch(
                actionCreators.loginFailed({
                  loginError: Immutable.fromJS(loginError),
                  error,
                  credentials,
                })
              );
            })
            .then(
              () =>
                options_.doRedirects &&
                checkAccessToCurrentRoute(dispatch, getState)
            );
        })
      )
      .then(() => {
        _loginInProcessFor = undefined;
      })
      .then(() => {
        if (credentials.privateId) {
          savePrivateId(credentials.privateId);
        }
      });
  };
}

let _logoutInProcess;
/**
 *
 * @param options
 *    * doRedirects(true): whether or not to do any redirects at all (e.g. if an invalid route was accessed)
 *
 * @returns {Function}
 */
export function accountLogout(options) {
  const options_ = {
    doRedirects: true,
    ...options,
  };

  clearPrivateId();

  return (dispatch, getState) => {
    const state = getState();

    if (state.get("logoutInProcess") || _logoutInProcess) {
      console.debug(
        "There's already a logout in process. Aborting redundant attempt."
      );
      return;
    }
    _logoutInProcess = true;

    return Promise.resolve()
      .then(() =>
        dispatch({
          type: actionTypes.logoutStarted,
          payload: {},
        })
      )
      .then(() => logout())
      .catch(error => {
        //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
        console.error("Error while trying to log out: ", error);
      })
      .then(() => {
        // for the case that we've been logged in to an anonymous account, we need to remove the privateId here.
        if (
          options_.doRedirects &&
          getIn(state, ["router", "currentParams", "privateId"])
        ) {
          return stateGoCurrent({ privateId: null })(dispatch, getState);
        }
      })
      .then(() =>
        dispatch({
          type: actionTypes.logout,
          payload: Immutable.fromJS({
            loggedIn: false,
            httpSessionDowngraded: true,
          }),
        })
      )
      .then(() => {
        won.clearStore();
      })
      .then(
        () =>
          options_.doRedirects && checkAccessToCurrentRoute(dispatch, getState)
      )
      .then(() => {
        _logoutInProcess = false;
      })
      .then(() =>
        dispatch({
          type: actionTypes.logout,
          payload: Immutable.fromJS({ loggedIn: false, logoutFinished: true }),
        })
      );
  };
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountRegister(credentials) {
  return (dispatch, getState) =>
    registerAccount(credentials)
      .then(() =>
        /*response*/ accountLogin(credentials, {
          fetchData: false,
          redirectToFeed: true,
        })(dispatch, getState)
      )
      .catch(error => {
        //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
        const registerError =
          "Registration failed (E-Mail might already be used)";
        console.error(registerError, error);
        dispatch(actionCreators.registerFailed({ registerError, error }));
      });
}

/**
 * @param credentials {email, password, privateId}
 * @returns {Function}
 */
export function accountTransfer(credentials) {
  return (dispatch, getState) =>
    transferPrivateAccount(credentials)
      .then(() => {
        credentials.privateId = undefined;
        /*response*/
        accountLogin(credentials, {
          fetchData: true,
          redirectToFeed: true,
        })(dispatch, getState);
      })
      .catch(error => {
        //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
        const registerError = "Account Transfer failed";
        console.error(registerError, error);
        dispatch(actionCreators.registerFailed({ registerError, error }));
      });
}

export function accountAcceptDisclaimer() {
  return dispatch => {
    setDisclaimerAccepted();
    dispatch({ type: actionTypes.acceptDisclaimerSuccess });
  };
}

export function reconnect() {
  return async (dispatch, getState) => {
    dispatch({ type: actionTypes.reconnect.start });
    try {
      await checkLoginStatus();
      dispatch({ type: actionTypes.reconnect.success });

      const state = getState();

      /*
      * -- check for new connections (i.e. matches and incoming requests) --
      */
      // await pageLoadAction()(dispatch, getState);
      // const email = getIn(state, ["user", "email"]);
      // await fetchOwnedData(email, payload => {
      //   dispatch({
      //     type: actionTypes.initialPageLoad, // TODO make this it's own type
      //     payload: wellFormedPayload(payload),
      //   });
      // });
      //TODO hard reload

      /* 
       * -- loading latest messages for all connections (we might have missed some during the dc) --
       */
      const connectionUris = getOwnedConnectionUris(state);
      await Promise.all(
        connectionUris.map(async connectionUri => {
          await loadLatestMessagesOfConnection({
            connectionUri,
            numberOfEvents: 10, //TODO magic number :|
            state,
            dispatch,
            actionTypesToDispatch: {
              start: actionTypes.reconnect.startingToLoadConnectionData,
              success: actionTypes.reconnect.receivedConnectionData,
              failure: actionTypes.reconnect.connectionFailedToLoad,
            },
          });
        })
      );
    } catch (e) {
      if (e.message == "Unauthorized") {
        dispatch({ type: actionTypes.logout });
        dispatch({ type: actionTypes.showMainMenuDisplay });
      } else {
        dispatch(actionCreators.lostConnection());
      }
      console.warn(e);
    }
  };
}
