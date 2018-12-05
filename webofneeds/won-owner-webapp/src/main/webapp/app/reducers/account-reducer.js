/**
 * Created by ksinger on 10.05.2016.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";

import { isDisclaimerAccepted } from "../won-localstorage.js";

const initialState = Immutable.fromJS({
  loggedIn: false,
  email: undefined,
  emailVerified: false,
  acceptedTermsOfService: false,
  acceptedDisclaimer: isDisclaimerAccepted(),
});

export default function(userData = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.store: {
      const username = action.payload.get("username");
      const emailVerified = action.payload.get("emailVerified");
      const acceptedTermsOfService = action.payload.get(
        "acceptedTermsOfService"
      );

      return userData
        .set("loggedIn", true)
        .set("username", username)
        .set("emailVerified", emailVerified)
        .set("acceptedTermsOfService", acceptedTermsOfService);
    }

    case actionTypes.account.verifyEmailAddressFailed:
      return userData.set(
        "emailVerificationError",
        action.payload.emailVerificationError
      );

    case actionTypes.account.verifyEmailAddressSuccess:
      return userData
        .set("emailVerificationError", undefined)
        .set("emailVerified", true);
    case actionTypes.account.verifyEmailAddressStarted:
      return userData.set("emailVerificationError", undefined);

    case actionTypes.account.acceptTermsOfServiceSuccess:
      return userData.set("acceptedTermsOfService", true);
    case actionTypes.account.acceptTermsOfServiceFailed:
      return userData.set("acceptedTermsOfService", false);

    case actionTypes.account.reset:
      return initialState.set(
        "acceptedDisclaimer",
        userData.get("acceptedDisclaimer")
      );

    case actionTypes.account.loginFailed:
      return userData
        .set("loginError", action.payload.loginError)
        .set("loggedIn", false);

    case actionTypes.view.clearLoginError:
      if (!userData.get("loggedIn")) {
        return userData.set("loginError", undefined);
      } else {
        return userData;
      }

    case actionTypes.view.clearRegisterError:
      return userData.set("registerError", undefined);

    case actionTypes.account.registerFailed:
      return userData
        .set("registerError", action.payload.registerError)
        .set("loggedIn", false);

    case actionTypes.account.acceptDisclaimerSuccess:
      return userData.set("acceptedDisclaimer", true);

    default:
      return userData;
  }
}
