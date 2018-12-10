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
  acceptTermsOfService,
  confirmRegistration,
  resendEmailVerification,
  sendAnonymousLinkEmail,
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
  if (state.getIn(["account", "loggedIn"])) {
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
    dispatch(actionCreators.account__registerFailed({ privateId }));
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
    const prevEmail = state.getIn(["account", "email"]);

    const wasLoggedIn =
      !state.getIn(["process", "processingInitialLoad"]) &&
      (prevPrivateId || prevEmail);

    if (
      state.getIn(["process", "processingLoginForEmail"]) === email ||
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
      !state.getIn(["process", "processingInitialLoad"]) &&
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

    return Promise.resolve()
      .then(() => {
        _loginInProcessFor = email;
        return dispatch({
          type: actionTypes.account.loginStarted,
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
          type: actionTypes.account.store,
          payload: Immutable.fromJS(data),
        })
      )
      .then(() => dispatch({ type: actionTypes.upgradeHttpSession }))
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
      .then(() => {
        if (options_.fetchData) {
          return fetchOwnedData(email, dispatch);
        } else {
          return Immutable.Map(); // only need to fetch data for non-new accounts
        }
      })
      .then(() => dispatch({ type: actionTypes.account.loginFinished }))
      .catch(error =>
        error.response.json().then(loginError => {
          return Promise.resolve()
            .then(() => {
              if (wasLoggedIn) {
                return dispatch({ type: actionTypes.account.reset });
              }
            })
            .then(() => {
              if (credentials.privateId) {
                loginError = won.PRIVATEID_NOT_FOUND_ERROR;
              }

              dispatch(
                actionCreators.account__loginFailed({
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
 * Processes logout
 * @returns {Function}
 */
export function accountLogout() {
  clearPrivateId();

  return (dispatch, getState) => {
    const state = getState();

    if (state.getIn(["process", "processingLogout"]) || _logoutInProcess) {
      console.debug(
        "There's already a logout in process. Aborting redundant attempt."
      );
      return;
    }
    _logoutInProcess = true;

    return Promise.resolve()
      .then(() => dispatch({ type: actionTypes.account.logoutStarted }))
      .then(() => logout())
      .catch(error => {
        //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
        console.error("Error while trying to log out: ", error);
      })
      .then(() => {
        // for the case that we've been logged in to an anonymous account, we need to remove the privateId here.
        if (getIn(state, ["router", "currentParams", "privateId"])) {
          return stateGoCurrent({ privateId: null })(dispatch, getState);
        }
      })
      .then(() => dispatch({ type: actionTypes.downgradeHttpSession }))
      .then(() => dispatch({ type: actionTypes.account.reset }))
      .then(() => won.clearStore())
      .then(() => checkAccessToCurrentRoute(dispatch, getState))
      .then(() => {
        _logoutInProcess = false;
      })
      .then(() => dispatch({ type: actionTypes.account.logoutFinished }));
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
        accountLogin(credentials, {
          fetchData: false,
          redirectToFeed: true,
        })(dispatch, getState)
      )
      .catch(error => {
        //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
        const registerError =
          "Registration failed (E-Mail might already be used)";
        console.error(registerError, error);
        dispatch(
          actionCreators.account__registerFailed({ registerError, error })
        );
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
        return accountLogin(credentials, {
          fetchData: true,
          redirectToFeed: true,
        })(dispatch, getState);
      })
      .catch(error => {
        //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
        const registerError = "Account Transfer failed";
        console.error(registerError, error);
        dispatch(
          actionCreators.account__registerFailed({ registerError, error })
        );
      });
}

export function accountAcceptDisclaimer() {
  return dispatch => {
    setDisclaimerAccepted();
    dispatch({ type: actionTypes.account.acceptDisclaimerSuccess });
  };
}

export function accountAcceptTermsOfService() {
  return dispatch => {
    dispatch({ type: actionTypes.account.acceptTermsOfServiceStarted });
    acceptTermsOfService()
      .then(() => {
        dispatch({ type: actionTypes.account.acceptTermsOfServiceSuccess });
      })
      .catch(() => {
        dispatch({ type: actionTypes.account.acceptTermsOfServiceFailed });
      });
  };
}

export function accountVerifyEmailAddress(verificationToken) {
  return dispatch => {
    dispatch({ type: actionTypes.account.verifyEmailAddressStarted });
    confirmRegistration(verificationToken)
      .then(() => {
        dispatch({ type: actionTypes.account.verifyEmailAddressSuccess });
      })
      .catch(error =>
        dispatch({
          type: actionTypes.account.verifyEmailAddressFailed,
          payload: {
            emailVerificationError: Immutable.fromJS(error.jsonResponse),
          },
        })
      );
  };
}

export function accountResendVerificationEmail(email) {
  return dispatch => {
    dispatch({ type: actionTypes.account.resendVerificationEmailStarted });
    resendEmailVerification(email)
      .then(() => {
        dispatch({ type: actionTypes.account.resendVerificationEmailSuccess });
      })
      .catch(error =>
        dispatch({
          type: actionTypes.account.resendVerificationEmailFailed,
          payload: {
            emailVerificationError: Immutable.fromJS(error.jsonResponse),
          },
        })
      );
  };
}

export function accountSendAnonymousLinkEmail(email, privateId) {
  return dispatch => {
    dispatch({ type: actionTypes.account.sendAnonymousLinkEmailStarted });
    sendAnonymousLinkEmail(email, privateId)
      .then(() => {
        dispatch({ type: actionTypes.account.sendAnonymousLinkEmailSuccess });
      })
      .catch(error =>
        dispatch({
          type: actionTypes.account.sendAnonymousLinkEmailFailed,
          payload: {
            anonymousEmailError: Immutable.fromJS(error.jsonResponse),
          },
        })
      );
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
        //FIXME: this seems weird and unintentional to me, the actionTypes.account.reset closes the main menu (see view-reducer.js) and the dispatch after opens it again, is this wanted that way?
        dispatch({ type: actionTypes.account.reset });
        dispatch({ type: actionTypes.view.showMainMenu });
      } else {
        dispatch(actionCreators.lostConnection());
      }
      console.warn(e);
    }
  };
}
