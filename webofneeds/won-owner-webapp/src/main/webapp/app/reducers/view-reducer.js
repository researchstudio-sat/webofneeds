/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { getIn } from "../utils.js";

const initialState = Immutable.fromJS({
  showRdf: false,
  showClosedNeeds: false,
  showMainMenu: false,
  showAddMessageContent: false,
  selectedAddMessageContent: undefined,
  showModalDialog: false,
  modalDialog: undefined,
  showAnonymousSlideIn: false,
});

export default function(viewState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.loginFailed:
    case actionTypes.view.showMainMenu:
      return viewState.set("showMainMenu", true);

    case actionTypes.account.store:
    case actionTypes.account.reset:
    case actionTypes.view.hideMainMenu:
      return viewState.set("showMainMenu", false);

    case actionTypes.view.toggleRdf:
      return viewState
        .set("showRdf", !viewState.get("showRdf"))
        .set("showMainMenu", false);

    case actionTypes.view.toggleClosedNeeds:
      return viewState.set(
        "showClosedNeeds",
        !viewState.get("showClosedNeeds")
      );

    case actionTypes.view.toggleAddMessageContent:
      return viewState
        .set("showAddMessageContent", !viewState.get("showAddMessageContent"))
        .set("selectedAddMessageContent", undefined);

    case actionTypes.view.selectAddMessageContent: {
      const selectedDetail = getIn(action, ["payload", "selectedDetail"]);
      return viewState
        .set("selectedAddMessageContent", selectedDetail)
        .set("showAddMessageContent", true);
    }

    case actionTypes.view.hideAddMessageContent:
      return viewState
        .set("showAddMessageContent", false)
        .set("selectedAddMessageContent", undefined);

    case actionTypes.view.removeAddMessageContent:
      return viewState.set("selectedAddMessageContent", undefined);

    case actionTypes.view.showModalDialog: {
      const modalDialog = Immutable.fromJS(action.payload);
      return viewState
        .set("showModalDialog", true)
        .set("modalDialog", modalDialog);
    }

    case actionTypes.view.hideModalDialog:
      return viewState
        .set("showModalDialog", false)
        .set("modalDialog", undefined);

    default:
      return viewState;
  }
}
