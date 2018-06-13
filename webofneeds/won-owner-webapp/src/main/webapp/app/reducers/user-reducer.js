/**
 * Created by ksinger on 10.05.2016.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";

import { isDisclaimerAccepted } from "../won-localstorage.js";

const initialState = Immutable.fromJS({
  loggedIn: false,
  acceptedDisclaimer: isDisclaimerAccepted(),
});

export default function(userData = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.initialPageLoad:
    case actionTypes.login: {
      //because we get payload as immutablejs-map sometimes but not always
      const immutablePayload = Immutable.fromJS(action.payload);

      const loggedIn = immutablePayload.get("loggedIn");
      const email = immutablePayload.get("email");

      if (loggedIn) {
        return Immutable.fromJS({
          loggedIn: true,
          email: email,
          acceptedDisclaimer: userData.get("acceptedDisclaimer"),
        });
      } else {
        return userData;
      }
    }

    case actionTypes.logout:
      return Immutable.fromJS({
        loggedIn: false,
        acceptedDisclaimer: userData.get("acceptedDisclaimer"),
      });

    case actionTypes.loginFailed:
      return Immutable.fromJS({
        loginError: action.payload.loginError,
        loggedIn: false,
        acceptedDisclaimer: userData.get("acceptedDisclaimer"),
      });

    case actionTypes.typedAtLoginCredentials:
      if (!userData.get("loggedIn")) {
        return userData.set("loginError", undefined);
      } else {
        return userData;
      }

    case actionTypes.registerReset:
      return Immutable.fromJS({ registerError: undefined });

    case actionTypes.registerFailed:
      return Immutable.fromJS({
        registerError: action.payload.registerError,
        acceptedDisclaimer: userData.get("acceptedDisclaimer"),
      });

    case actionTypes.acceptDisclaimerSuccess:
      return userData.set("acceptedDisclaimer", true);

    default:
      return userData;
  }
}
