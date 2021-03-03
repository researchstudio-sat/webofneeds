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

import * as generalSelectors from "../redux/selectors/general-selectors.js";

import * as accountUtils from "../redux/utils/account-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import { getPathname, getQueryParams, delay, parseWorkerError } from "../utils";

/**
 * Makes sure user is either logged in
 * or creates a private-ID account as fallback.
 */
export const ensureLoggedIn = (dispatch, getState) => {
  const state = getState();
  if (accountUtils.isLoggedIn(generalSelectors.getAccountState(state))) {
    return Promise.resolve();
  } else {
    const privateId = wonUtils.generatePrivateId();

    return accountRegister({ privateId })(dispatch, getState)
      .then(() => delay(2000))
      .catch(err => {
        console.error(
          `Creating temporary account (${privateId}) has failed due to `,
          err
        );
        dispatch(actionCreators.account__registerFailed({ privateId }));
      });
  }
};

let _loginInProcessFor;
/**
 *
 * @param credentials either {email, password} or {privateId}
 * @param redirectToFeed def. false, whether or not to redirect to the feed after signing in (needs `redirects` to be true)
 * @returns {Function}
 */
export const accountLogin = credentials => (dispatch, getState) => {
  const state = getState();

  const { email } = wonUtils.parseCredentials(credentials);

  const accountState = generalSelectors.getAccountState(state);
  const processState = generalSelectors.getProcessState(state);
  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const processingLoginForEmail =
    processUtils.isProcessingLoginForEmail(processState) || _loginInProcessFor;

  if (processingLoginForEmail) {
    console.debug(
      "Already logging in as ",
      processingLoginForEmail,
      ". Canceling redundant attempt."
    );
    return;
  }

  if (isLoggedIn && !processUtils.isProcessingInitialLoad(processState)) {
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
    .then(() => stateStore.fetchOwnedMetaData(dispatch, getState))
    .then(() => dispatch({ type: actionTypes.account.loginFinished }))
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      let loginError = errorParsed.response;

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
            })
          );
        });
    })
    .then(() => {
      _loginInProcessFor = undefined;
    });
};

let _logoutInProcess;

/**
 * Processes logout
 * @returns {Function}
 */
export const accountLogout = history => (dispatch, getState) => {
  const state = getState();

  if (
    processUtils.isProcessingLogout(generalSelectors.getProcessState(state)) ||
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
      const { privateId } = getQueryParams(history.location);
      if (privateId) {
        history.replace(getPathname(history.location));
      }
    })
    .then(() => dispatch({ type: actionTypes.downgradeHttpSession }))
    .then(() => dispatch({ type: actionTypes.account.reset }))
    .then(() => {
      _logoutInProcess = false;
    })
    .then(() => dispatch({ type: actionTypes.account.logoutFinished }));
};

/**
 * @param credentials either {email, password} or {privateId}
 * @returns {Function}
 */
export const accountRegister = credentials => (dispatch, getState) =>
  ownerApi
    .registerAccount(credentials)
    .then(() => accountLogin(credentials)(dispatch, getState))
    .catch(error => {
      //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
      const registerError =
        "Registration failed (E-Mail might already be used)";
      console.error(registerError, error);
      dispatch(
        actionCreators.account__registerFailed({ registerError, error })
      );
    });

/**
 * @param credentials {email, password, privateId}
 * @returns {Function}
 */
//FIXME: accountTransfer only works if we have the full privateId which we might not have anymore after the refactoring
export const accountTransfer = credentials => (dispatch, getState) =>
  ownerApi
    .transferPrivateAccount(credentials)
    .then(() => {
      credentials.privateId = undefined;
      return accountLogin(credentials)(dispatch, getState);
    })
    .catch(error => {
      //TODO: PRINT MORE SPECIFIC ERROR MESSAGE, already registered/password to short etc.
      const registerError = "Account Transfer failed";
      console.error(registerError, error);
      dispatch(
        actionCreators.account__registerFailed({ registerError, error })
      );
    });

/**
 * @param credentials {email, newPassword, recoveryKey}
 */
export const accountResetPassword = credentials => (dispatch, getState) =>
  ownerApi
    .resetPassword(credentials)
    .then(() => {
      credentials.privateId = undefined;
      credentials.password = credentials.newPassword;
      return accountLogin(credentials)(dispatch, getState);
    })
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      let resetPasswordError = "Password Reset failed. Error not found";
      switch (errorParsed.response.code) {
        case 1400:
        case 8400:
          resetPasswordError = "No User found with this email";
          break;
        case 8401:
          resetPasswordError = "Password not valid";
          break;
        case 8402:
          resetPasswordError = "Wrong Recovery Key";
          break;
      }

      dispatch({
        type: actionTypes.account.resetPasswordFailed,
        payload: {
          resetPasswordError: Immutable.fromJS({
            code: errorParsed.response.code,
            msg: resetPasswordError,
          }),
        },
      });
    });

/**
 * @param credentials {email, oldPassword, newPassword}
 * @returns {Function}
 */
export const accountChangePassword = credentials => dispatch =>
  ownerApi
    .changePassword(credentials)
    .then(() => {
      dispatch({ type: actionTypes.account.changePasswordSuccess });
    })
    .catch(() => {
      dispatch({ type: actionTypes.account.changePasswordFailed });
    });

export const accountAcceptDisclaimer = () => dispatch => {
  setDisclaimerAccepted();
  dispatch({ type: actionTypes.account.acceptDisclaimerSuccess });
};

export const accountAcceptTermsOfService = () => dispatch => {
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

export const accountVerifyEmailAddress = verificationToken => dispatch => {
  dispatch({ type: actionTypes.account.verifyEmailAddressStarted });
  ownerApi
    .confirmRegistration(verificationToken)
    .then(() => {
      dispatch({ type: actionTypes.account.verifyEmailAddressSuccess });
    })
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      dispatch({
        type: actionTypes.account.verifyEmailAddressFailed,
        payload: {
          emailVerificationError: Immutable.fromJS(errorParsed.response),
        },
      });
    });
};

export const accountResendVerificationEmail = email => dispatch => {
  dispatch({ type: actionTypes.account.resendVerificationEmailStarted });
  ownerApi
    .resendEmailVerification(email)
    .then(() => {
      dispatch({ type: actionTypes.account.resendVerificationEmailSuccess });
    })
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      dispatch({
        type: actionTypes.account.resendVerificationEmailFailed,
        payload: {
          emailVerificationError: Immutable.fromJS(errorParsed.response),
        },
      });
    });
};

export const accountSendAnonymousLinkEmail = (email, privateId) => dispatch => {
  dispatch({ type: actionTypes.account.sendAnonymousLinkEmailStarted });
  ownerApi
    .sendAnonymousLinkEmail(email, privateId)
    .then(() => {
      dispatch({ type: actionTypes.account.sendAnonymousLinkEmailSuccess });
    })
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      dispatch({
        type: actionTypes.account.sendAnonymousLinkEmailFailed,
        payload: {
          anonymousEmailError: Immutable.fromJS(errorParsed.response),
        },
      });
    });
};

export const reconnect = () => (dispatch, getState) => {
  dispatch({ type: actionTypes.reconnect.start });
  return ownerApi
    .checkLoginStatus()
    .then(() => {
      dispatch({ type: actionTypes.reconnect.success });

      return stateStore.fetchOwnedMetaData(dispatch, getState);
    })
    .catch(e => {
      if (e.status >= 400 && e.status < 500) {
        //FIXME: this seems weird and unintentional to me, the actionTypes.account.reset closes the main menu (see view-reducer.js) and the dispatch after opens it again, is this wanted that way?
        dispatch({ type: actionTypes.account.reset });
        dispatch({ type: actionTypes.view.showMainMenu });
      } else {
        dispatch(actionCreators.lostConnection());
      }
      console.warn(e);
    });
};
