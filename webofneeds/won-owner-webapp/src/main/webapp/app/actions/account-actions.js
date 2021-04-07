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
import { getPathname, getQueryParams, parseWorkerError } from "../utils";

export const requireLogin = callback => (dispatch, getState) =>
  checkLoginState(dispatch, getState, callback);

export const checkLoginState = (dispatch, getState, actionWhenLoggedIn) => {
  //TODO: DISTINGUISH BETWEEN ACCEPT ANON, AND LOGIN SUCCESS
  const state = getState();

  return ownerApi
    .checkLoginStatus()
    .then(() => {
      return actionWhenLoggedIn(state);
    })
    .catch(() => {
      dispatch(
        actionCreators.view__showLoggedOutDialog(
          Immutable.fromJS({
            afterLoginCallback: () => {
              dispatch(actionCreators.view__hideModalDialog());
              //FIXME: we used to have a delay in between (anon)login and atomCreation/actionWhenLoggedIn, i am not sure if we still need it but if so, we should add it here
              return actionWhenLoggedIn(state);
            },
          })
        )
      );
    });
};

let _loginInProcessFor;
/**
 *
 * @param credentials either {email, password} or {privateId}
 * @param callback function that gets executed after successful registration/login
 * @returns {Function}
 */
export const login = (credentials, callback) => (dispatch, getState) => {
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
    .then(() => callback && callback())
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
export const logout = history => (dispatch, getState) => {
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
 * @param callback function that gets executed after successful registration/login
 * @returns {Function}
 */
export const register = (credentials, callback) => (dispatch, getState) =>
  ownerApi
    .registerAccount(credentials)
    .then(() => login(credentials)(dispatch, getState))
    .then(() => callback && callback())
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
//FIXME: transfer only works if we have the full privateId which we might not have anymore after the refactoring
export const transfer = credentials => (dispatch, getState) =>
  ownerApi
    .transferPrivateAccount(credentials)
    .then(() => {
      credentials.privateId = undefined;
      return login(credentials)(dispatch, getState);
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
export const resetPassword = credentials => (dispatch, getState) =>
  ownerApi
    .resetPassword(credentials)
    .then(() => {
      credentials.privateId = undefined;
      credentials.password = credentials.newPassword;
      return login(credentials)(dispatch, getState);
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
export const changePassword = credentials => dispatch =>
  ownerApi
    .changePassword(credentials)
    .then(() => {
      dispatch({ type: actionTypes.account.changePasswordSuccess });
    })
    .catch(() => {
      dispatch({ type: actionTypes.account.changePasswordFailed });
    });

export const acceptDisclaimer = () => dispatch => {
  setDisclaimerAccepted();
  dispatch({ type: actionTypes.account.acceptDisclaimerSuccess });
};

export const acceptTermsOfService = () => dispatch => {
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

export const verifyEmailAddress = verificationToken => dispatch => {
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

export const resendVerificationEmail = email => dispatch => {
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

export const sendAnonymousLinkEmail = (email, privateId) => dispatch => {
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

export const fetchUserSettings = () => dispatch => {
  dispatch({ type: actionTypes.account.fetchUserSettingsStarted });
  ownerApi
    .fetchUserSettings()
    .then(userSettings => {
      dispatch({
        type: actionTypes.account.fetchUserSettingsSuccess,
        payload: { userSettings: Immutable.fromJS(userSettings) },
      });
    })
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      dispatch({
        type: actionTypes.account.fetchUserSettingsFailed,
        payload: {
          fetchUserSettingsError: Immutable.fromJS(errorParsed.response),
        },
      });
    });
};

export const updateAtomUserSettings = atomUserSetting => dispatch => {
  dispatch({ type: actionTypes.account.updateAtomUserSettingsStarted });
  ownerApi
    .updateAtomUserSettings(atomUserSetting)
    .then(() => {
      dispatch({
        type: actionTypes.account.updateAtomUserSettingsSuccess,
        payload: { atomUserSetting: atomUserSetting },
      });
    })
    .catch(error => {
      let errorParsed = parseWorkerError(error);

      dispatch({
        type: actionTypes.account.updateAtomUserSettingsFailed,
        payload: {
          updateAtomUserSettings: Immutable.fromJS(errorParsed.response),
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
    .catch(err => {
      const error = parseWorkerError(err);
      if (error.status >= 400 && error.status < 500) {
        //FIXME: this seems weird and unintentional to me, the actionTypes.account.reset closes the main menu (see view-reducer.js) and the dispatch after opens it again, is this wanted that way?
        dispatch({ type: actionTypes.account.reset });
        dispatch({ type: actionTypes.view.showMainMenu });
      } else {
        dispatch(actionCreators.lostConnection());
      }
      console.warn(error);
    });
};
