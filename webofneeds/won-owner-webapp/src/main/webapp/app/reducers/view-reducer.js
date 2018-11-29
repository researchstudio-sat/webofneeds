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
});

export default function(viewState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.loginFailed:
    case actionTypes.view.showMainMenuDisplay:
      return viewState.set("showMainMenu", true);

    case actionTypes.account.login:
    case actionTypes.account.logout:
    case actionTypes.view.hideMainMenuDisplay:
      return viewState.set("showMainMenu", false);

    case actionTypes.view.toggleRdfDisplay:
      return viewState
        .set("showRdf", !viewState.get("showRdf"))
        .set("showMainMenu", false);

    case actionTypes.view.toggleClosedNeedsDisplay:
      return viewState.set(
        "showClosedNeeds",
        !viewState.get("showClosedNeeds")
      );

    case actionTypes.toggleAddMessageContentDisplay:
      return viewState
        .set("showAddMessageContent", !viewState.get("showAddMessageContent"))
        .set("selectedAddMessageContent", undefined);

    case actionTypes.selectAddMessageContent: {
      const selectedDetail = getIn(action, ["payload", "selectedDetail"]);
      return viewState
        .set("selectedAddMessageContent", selectedDetail)
        .set("showAddMessageContent", true);
    }

    case actionTypes.showAddMessageContentDisplay:
      return viewState.set("showAddMessageContent", true);

    case actionTypes.hideAddMessageContentDisplay:
      return viewState
        .set("showAddMessageContent", false)
        .set("selectedAddMessageContent", undefined);

    case actionTypes.removeAddMessageContent:
      return viewState.set("selectedAddMessageContent", undefined);

    case actionTypes.openModalDialog: {
      const modalDialog = Immutable.fromJS(action.payload);
      return viewState
        .set("showModalDialog", true)
        .set("modalDialog", modalDialog);
    }

    case actionTypes.closeModalDialog:
      return viewState
        .set("showModalDialog", false)
        .set("modalDialog", undefined);

    default:
      return viewState;
  }
}
