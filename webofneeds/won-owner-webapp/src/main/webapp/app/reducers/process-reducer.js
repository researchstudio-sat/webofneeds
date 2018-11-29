/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
//import { getIn } from "../utils.js";

const initialState = Immutable.fromJS({
  creatingWhatsX: false,
  logoutInProcess: false,
});

export default function(processState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.needs.whatsNew:
    case actionTypes.needs.whatsAround:
      return processState.set("creatingWhatsX", true);

    case actionTypes.failedToGetLocation:
    case actionTypes.needs.createSuccessful:
      return processState.set("creatingWhatsX", false);

    case actionTypes.account.logoutStarted:
      return processState.set("logoutInProcess", true);

    case actionTypes.account.logout:
      return processState.set("logoutInProcess", false);

    default:
      return processState;
  }
}
