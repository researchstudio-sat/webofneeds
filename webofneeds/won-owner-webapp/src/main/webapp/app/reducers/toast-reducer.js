import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";
import won from "../won-es6.js";
import { getIn, get, generateIdString } from "../utils.js";
import { parseRestErrorMessage } from "../won-utils.js";

const initialState = Immutable.fromJS({});

export default function(allToasts = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.toasts.test:
      allToasts = pushNewToast(allToasts, "Error Toast", won.TOASTS.ERROR);
      allToasts = pushNewToast(allToasts, "Warning Toast", won.TOASTS.WARN);
      allToasts = pushNewToast(allToasts, "Info Toast", won.TOASTS.INFO);
      return allToasts;

    case actionTypes.account.reset:
      return initialState;

    case actionTypes.messages.connect.failure:
    case actionTypes.connections.sendChatMessageFailed: {
      const msg = getIn(action, ["payload", "message"]);
      return pushNewToast(
        allToasts,
        "Error while processing chat message: \n\n" + msg,
        won.TOASTS.ERROR
      );
    }

    case actionTypes.atoms.createFailure: {
      return pushNewToast(
        allToasts,
        "Could not create Atom, further information in console log",
        won.TOASTS.ERROR
      );
    }

    case actionTypes.atoms.editFailure: {
      return pushNewToast(
        allToasts,
        "Could not edit Atom, further information in console log",
        won.TOASTS.ERROR
      );
    }

    case actionTypes.account.registerFailed: {
      const privateId = getIn(action, ["payload", "privateId"]);
      if (privateId) {
        return pushNewToast(
          allToasts,
          "Sorry, something failed when posting/generating a new private-ID (the one in " +
            "your url-bar). Copy the text you've written somewhere safe, then log out / remove " +
            "the ID, then refresh the page and try posting again.",
          won.TOASTS.ERROR
        );
      } else {
        return allToasts;
      }
    }

    case actionTypes.account.acceptTermsOfServiceFailed:
      return pushNewToast(
        allToasts,
        "Failed to accept Terms Of Service",
        won.TOASTS.ERROR
      );

    case actionTypes.account.loginFailed: {
      const loginError = getIn(action, ["payload", "loginError"]);
      const errorCode = loginError && loginError.get("code");
      if (errorCode == won.RESPONSECODE.PRIVATEID_NOT_FOUND) {
        //If there is a privateId Problem we push a toast
        return pushNewToast(
          allToasts,
          parseRestErrorMessage(loginError),
          won.TOASTS.ERROR
        );
      } else {
        return allToasts;
      }
    }

    case actionTypes.view.toggleDebugMode: {
      const debugmode = get(action, "payload");
      const text = debugmode ? "Debugmode On" : "Debugmode Off";
      return pushNewToast(allToasts, text, won.TOASTS.INFO);
    }

    case actionTypes.toasts.push: {
      const text = getIn(action, ["payload", "text"]);
      if (text) {
        const type = getIn(action, ["payload", "type"]);
        return pushNewToast(allToasts, text, type ? type : won.TOASTS.INFO);
      } else {
        return allToasts;
      }
    }

    case actionTypes.geoLocationDenied:
      return pushNewToast(
        allToasts,
        'Sorry, we were unable to create your "What\'s Around"-Post, ' +
          "because you have denied us accesss to your current location. " +
          'To enable it, reload the page and click on "allow access". If' +
          "you've disabled it permanently you can find instructions here for  " +
          "[Chrome](https://support.google.com/chrome/answer/142065), " +
          "[Firefox](https://www.mozilla.org/en-US/firefox/geolocation/), " +
          "[Safari](https://support.apple.com/en-us/HT204690), " +
          "[Internet Explorer Edge](https://privacy.microsoft.com/en-us/windows-10-location-and-privacy).",
        won.TOASTS.WARN
      );

    //INFO TOASTS: vocab.WON.infoToast

    //WARN TOASTS: vocab.WON.warnToast
    //ERROR TOASTS: vocab.WON.errorToast
    case actionTypes.messages.closeAtom.failed:
      return pushNewToast(allToasts, "Failed to close atom", won.TOASTS.ERROR);

    case actionTypes.messages.reopenAtom.failed:
      return pushNewToast(allToasts, "Failed to reopen atom", won.TOASTS.ERROR);

    case actionTypes.messages.chatMessage.failure:
      return pushNewToast(
        allToasts,
        "Failed to send chat message",
        won.TOASTS.ERROR
      );

    case actionTypes.messages.atomMessageReceived: {
      const humanReadable = action.payload.humanReadable;
      const message = action.payload.message;
      return pushNewToast(
        allToasts,
        "Notification for your posting '" + humanReadable + "': " + message,
        won.TOASTS.INFO
      );
    }

    case actionTypes.atoms.closedBySystem: {
      const humanReadable = action.payload.humanReadable;
      const message = action.payload.message;
      return pushNewToast(
        allToasts,
        "Closed your posting '" + humanReadable + "'. Cause: " + message,
        won.TOASTS.INFO
      );
    }

    case actionTypes.failedToGetLocation: {
      return pushNewToast(
        allToasts,
        "Could not get location. You may need to allow this in your browser",
        won.TOASTS.ERROR
      );
    }

    //SPECIFIC TOAST ACTIONS
    case actionTypes.toasts.delete:
      return allToasts.deleteIn([get(action.payload, "id")]);

    default:
      return allToasts;
  }
}

/**
 *
 * @param allToasts
 * @param msg
 * @param type
 * @returns {*}
 */
function pushNewToast(allToasts, msg, type) {
  let toastType = type;
  if (!toastType) {
    toastType = won.TOASTS.INFO;
  }

  const id = generateIdString(6);
  return allToasts.setIn(
    [id],
    Immutable.fromJS({
      id: id,
      type: toastType,
      msg: msg,
    })
  );
}
