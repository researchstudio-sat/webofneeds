/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getIn } from "../utils.js";

const initialState = Immutable.fromJS({
  initialLoadFinished: false,
  processingPublish: false,
  processingLogout: false,
  loginInProcess: false,
  processingLoginForEmail: undefined,
});

export default function(processState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.persona.create:
    case actionTypes.needs.create:
    case actionTypes.needs.whatsNew:
    case actionTypes.needs.whatsAround:
      return processState.set("processingPublish", true);

    case actionTypes.failedToGetLocation:
    case actionTypes.persona.createSuccessful:
    case actionTypes.needs.createSuccessful:
      return processState.set("processingPublish", false);

    case actionTypes.account.logoutStarted:
      return processState.set("processingLogout", true);

    case actionTypes.account.logout:
      return processState.set("processingLogout", false);

    case actionTypes.account.loginStarted:
      return processState
        .set("loginInProcess", true)
        .set("processingLoginForEmail", getIn(action, ["payload", "email"]));

    case actionTypes.account.login: {
      if (getIn(action, ["payload", "loginFinished"])) {
        return processState
          .set("loginInProcess", false)
          .set("processingLoginForEmail", undefined);
      }
      return processState;
    }

    case actionTypes.account.loginFailed:
      return processState
        .set("loginInProcess", false)
        .set("processingLoginForEmail", undefined);

    case actionTypes.initialPageLoad: {
      if (processState.get("initialLoadFinished")) {
        return processState;
      } else {
        const initialLoadFinished = getIn(action, [
          "payload",
          "initialLoadFinished",
        ]);
        return processState.set("initialLoadFinished", initialLoadFinished);
      }
    }

    default:
      return processState;
  }
}
