/**
 * Created by ksinger on 19.02.2016.
 */

import won from "../won-es6.js";
import Immutable from "immutable";
import { actionTypes, actionCreators } from "./actions.js";
import * as stateStore from "../redux/state-store.js";
import * as wonUtils from "../won-utils.js";
import * as ownerApi from "../api/owner-api.js";
import { setDisclaimerAccepted } from "../won-localstorage.js";
import { stateGoCurrent } from "./cstm-router-actions.js";
import { checkAccessToCurrentRoute } from "../configRouting.js";

import { get } from "../utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import { loadLatestMessagesOfConnection } from "./connections-actions.js";
import { getPrivateIdFromRoute } from "../redux/selectors/general-selectors.js";

import * as accountUtils from "../redux/utils/account-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";

/**
 * Makes sure user is either logged in
 * or creates a private-ID account as fallback.
 */
export async function ensureLoggedIn(dispatch, getState) {
  const state = getState();
  if (accountUtils.isLoggedIn(get(state, "account"))) {
    return;
  }

  const privateId = wonUtils.generatePrivateId();
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
 * @param credentials either {email, password} or {privateId}
 * @param redirectToFeed def. false, whether or not to redirect to the feed after signing in (needs `redirects` to be true)
 * @returns {Function}
 */
export function accountLogin(credentials, redirectToFeed = false) {
  return (dispatch, getState) => {
    const state = getState();

    const { email } = wonUtils.parseCredentials(credentials);

    const accountState = get(state, "account");
    const isLoggedIn = accountUtils.isLoggedIn(accountState);
    const processingLoginForEmail =
      processUtils.isProcessingLoginForEmail(get(state, "process")) ||
      _loginInProcessFor;

    if (processingLoginForEmail) {
      console.debug(
        "Already logging in as ",
        processingLoginForEmail,
        ". Canceling redundant attempt."
      );
      return;
    }

    if (
      isLoggedIn &&
      !processUtils.isProcessingInitialLoad(get(state, "process"))
    ) {
      const loggedInEmail = accountUtils.getEmail(accountState);

      if (credentials.email === loggedInEmail) {
        console.debug(
          "Already loggedIn with (" +
            credentials.email +
            "). Aborting login attempt."
        );
        return;
      }
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
        if (isLoggedIn) {
          return ownerApi.logout();
        }
      })
      .then(() => ownerApi.login(credentials))
      .then(data =>
        dispatch({
          type: actionTypes.account.store,
          payload: Immutable.fromJS(data),
        })
      )
      .then(() => dispatch({ type: actionTypes.upgradeHttpSession }))
      .then(() => stateStore.fetchOwnedData(dispatch, getState))
      .then(() => dispatch({ type: actionTypes.account.loginFinished }))
      .catch(error =>
        error.response.json().then(loginError => {
          return Promise.resolve()
            .then(() => {
              if (isLoggedIn) {
                return dispatch({ type: actionTypes.account.reset });
              }
            })
            .then(() => {
              if (credentials.privateId) {
                loginError = won.PRIVATEID_NOT_FOUND_ERROR;
              }

              return dispatch(
                actionCreators.account__loginFailed({
                  loginError: Immutable.fromJS(loginError),
                  error,
                  credentials,
                })
              );
            });
        })
      )
      .then(() => {
        if (redirectToFeed) {
          return dispatch(
            actionCreators.router__stateGoResetParams("inventory")
          );
        }
        return Promise.resolve();
      })
      .then(() => checkAccessToCurrentRoute(dispatch, getState))
      .then(() => {
        _loginInProcessFor = undefined;
      });
  };
}

let _logoutInProcess;

/**
 * Processes logout
 * @returns {Function}
 */
export function accountLogout() {
  return (dispatch, getState) => {
    const state = getState();

    if (
      processUtils.isProcessingLogout(get(state, "process")) ||
      _logoutInProcess
    ) {
      console.debug("Logout in process. Aborting redundant attempt.");
      return;
    }
    _logoutInProcess = true;

    return Promise.resolve()
      .then(() => dispatch({ type: actionTypes.account.logoutStarted }))
      .then(() => ownerApi.logout())
      .catch(error => {
        //TODO: PRINT ERROR MESSAGE AND CHANGE STATE ACCORDINGLY
        console.error("Error while trying to log out: ", error);
      })
      .then(() => {
        // for the case that we've been logged in to an anonymous account, we need to remove the privateId here.
        if (getPrivateIdFromRoute(state)) {
          return stateGoCurrent({ privateId: null })(dispatch, getState);
        }
      })
      .then(() => dispatch({ type: actionTypes.downgradeHttpSession }))
      .then(() => dispatch({ type: actionTypes.account.reset }))
      .then(() => won.clearStore())
      .then(() => {
        _logoutInProcess = false;
      })
      .then(() => dispatch({ type: actionTypes.account.logoutFinished }))
      .then(() => checkAccessToCurrentRoute(dispatch, getState));
  };
}

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export function accountRegister(credentials) {
  return (dispatch, getState) =>
    ownerApi
      .registerAccount(credentials)
      .then(() => accountLogin(credentials, true)(dispatch, getState))
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
  //FIXME: accountTransfer only works if we have the full privateId which we might not have anymore after the refactoring
  return (dispatch, getState) =>
    ownerApi
      .transferPrivateAccount(credentials)
      .then(() => {
        credentials.privateId = undefined;
        return accountLogin(credentials, true)(dispatch, getState);
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

/**
 * @param credentials {email, oldPassword, newPassword}
 * @returns {Function}
 */
export function accountChangePassword(credentials) {
  return dispatch =>
    ownerApi
      .changePassword(credentials)
      .then(() => {
        dispatch({ type: actionTypes.account.changePasswordSuccess });
      })
      .catch(() => {
        dispatch({ type: actionTypes.account.changePasswordFailed });
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
    ownerApi
      .acceptTermsOfService()
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
    ownerApi
      .confirmRegistration(verificationToken)
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
    ownerApi
      .resendEmailVerification(email)
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
    ownerApi
      .sendAnonymousLinkEmail(email, privateId)
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
      await ownerApi.checkLoginStatus();
      dispatch({ type: actionTypes.reconnect.success });

      const state = getState();

      /* 
       * -- loading latest messages for all connections (we might have missed some during the dc) --
       */
      const connectionUris = connectionSelectors.getOwnedConnectionUris(state);
      await Promise.all(
        connectionUris.map(connectionUri =>
          loadLatestMessagesOfConnection({
            connectionUri,
            numberOfEvents: 10, //TODO magic number :|
            state,
            dispatch,
          })
        )
      );
    } catch (e) {
      if (e.status >= 400 && e.status < 500) {
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
