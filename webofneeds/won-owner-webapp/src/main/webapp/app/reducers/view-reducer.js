/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";

const initialState = Immutable.fromJS({
  showRdf: false,
  showClosedNeeds: false,
  showMainMenu: false,
  showAddMessageContent: false,
  showModalDialog: false,
});

export default function(viewState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.loginFailed:
    case actionTypes.showMainMenuDisplay:
      return viewState.set("showMainMenu", true);

    case actionTypes.account.login:
    case actionTypes.account.logout:
    case actionTypes.hideMainMenuDisplay:
      return viewState.set("showMainMenu", false);

    case actionTypes.toggleRdfDisplay:
      return viewState
        .set("showRdf", !viewState.get("showRdf"))
        .set("showMainMenu", false);

    case actionTypes.toggleClosedNeedsDisplay:
      return viewState.set(
        "showClosedNeeds",
        !viewState.get("showClosedNeeds")
      );

    case actionTypes.hideClosedNeedsDisplay:
      return viewState.set("showClosedNeeds", false);

    case actionTypes.showClosedNeedsDisplay:
      return viewState.set("showClosedNeeds", true);

    default:
      return viewState;
  }
}
