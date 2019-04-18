/**
 * Created by quasarchimaere on 29.11.2018.
 */
import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import { get, getIn } from "../utils.js";

const initialState = Immutable.fromJS({
  showRdf: false,
  showClosedAtoms: false,
  showMainMenu: false,
  showAddMessageContent: false,
  selectedAddMessageContent: undefined,
  showModalDialog: false,
  modalDialog: undefined,
  anonymousSlideIn: {
    visible: false,
    expanded: false,
    showEmailInput: false,

    linkSent: false,
    linkCopied: false,
  },
  showSlideIns: true,
  atoms: new Immutable.Map(),
});

const initialAtomState = Immutable.fromJS({
  showGeneralInfo: false,
  visibleTab: "DETAIL",
});

export default function(viewState = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.lostConnection:
      return viewState.set("showSlideIns", true);

    case actionTypes.view.toggleSlideIns:
      return viewState.set("showSlideIns", !viewState.get("showSlideIns"));

    case actionTypes.account.loginFailed:
    case actionTypes.view.showMainMenu:
      return viewState.set("showMainMenu", true);

    case actionTypes.account.reset:
      return initialState;

    case actionTypes.account.store:
    case actionTypes.view.hideMainMenu: {
      if (action.payload) {
        const emailVerified = action.payload.get("emailVerified");
        const acceptedTermsOfService = action.payload.get(
          "acceptedTermsOfService"
        );

        const isAnonymous = action.payload.get("isAnonymous");

        if (isAnonymous || !emailVerified || !acceptedTermsOfService) {
          viewState = viewState.set("showSlideIns", true);
        }
      }

      return viewState.set("showMainMenu", false);
    }

    case actionTypes.view.toggleRdf:
      return viewState
        .set("showRdf", !viewState.get("showRdf"))
        .set("showMainMenu", false);

    case actionTypes.view.toggleClosedAtoms:
      return viewState.set(
        "showClosedAtoms",
        !viewState.get("showClosedAtoms")
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

    case actionTypes.view.showTermsDialog: {
      const payload = Immutable.fromJS(action.payload);

      const acceptCallback = get(payload, "acceptCallback");
      const cancelCallback = get(payload, "cancelCallback");

      const termsDialog = Immutable.fromJS({
        showTerms: true,
        buttons: [
          {
            caption: "Yes, I accept ToS",
            callback: acceptCallback,
          },
          {
            caption: "No, cancel",
            callback: cancelCallback,
          },
        ],
      });
      return viewState
        .set("showModalDialog", true)
        .set("modalDialog", termsDialog);
    }

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

    case actionTypes.view.anonymousSlideIn.hide:
      return viewState
        .setIn(["anonymousSlideIn", "visible"], false)
        .setIn(["anonymousSlideIn", "expanded"], false)
        .setIn(["anonymousSlideIn", "linkSent"], false)
        .setIn(["anonymousSlideIn", "linkCopied"], false)
        .setIn(["anonymousSlideIn", "showEmailInput"], false)
        .set("showSlideIns", false);

    case actionTypes.view.anonymousSlideIn.expand:
      return viewState
        .setIn(["anonymousSlideIn", "visible"], true)
        .setIn(["anonymousSlideIn", "expanded"], true)
        .setIn(["anonymousSlideIn", "showEmailInput"], false);

    case actionTypes.view.anonymousSlideIn.collapse:
      return viewState
        .setIn(["anonymousSlideIn", "visible"], true)
        .setIn(["anonymousSlideIn", "expanded"], false)
        .setIn(["anonymousSlideIn", "showEmailInput"], false);

    case actionTypes.view.anonymousSlideIn.showEmailInput:
      return viewState.setIn(["anonymousSlideIn", "showEmailInput"], true);

    case actionTypes.account.sendAnonymousLinkEmailSuccess:
      return viewState
        .setIn(["anonymousSlideIn", "linkSent"], true)
        .setIn(["anonymousSlideIn", "linkCopied"], false);

    case actionTypes.account.copiedAnonymousLinkSuccess:
      return viewState
        .setIn(["anonymousSlideIn", "linkCopied"], true)
        .setIn(["anonymousSlideIn", "linkSent"], false);

    case actionTypes.atoms.selectTab: {
      const atomUri = action.payload.get("atomUri");
      const selectTab = action.payload.get("selectTab");

      return viewState.updateIn(
        ["atoms", atomUri],
        initialAtomState,
        atomState => atomState.update("visibleTab", () => selectTab)
      );
    }

    default:
      return viewState;
  }
}
