/**
 * Created by ksinger on 26.11.2015.
 */

import { actionTypes } from "../actions/actions.js";
import Immutable from "immutable";

const initialState = Immutable.fromJS({
  waitingForAnswer: {},
  claimOnSuccess: {},
  refreshDataOnSuccess: {},
  lostConnection: false,
  reconnecting: false,
});
export function messagesReducer(messages = initialState, action = {}) {
  switch (action.type) {
    case actionTypes.account.reset:
      return initialState;

    case actionTypes.connections.sendChatMessage:
    case actionTypes.connections.close:
    case actionTypes.messages.send:
    case actionTypes.atoms.edit:
    case actionTypes.atoms.connectSockets:
    case actionTypes.atoms.create: {
      return messages.setIn(
        ["waitingForAnswer", action.payload.messageUri],
        action.payload.message
      );
    }

    case actionTypes.atoms.editSuccessful:
    case actionTypes.atoms.editFailure:
    case actionTypes.atoms.createSuccessful:
    case actionTypes.atoms.createFailure:
    case actionTypes.messages.chatMessage.failure:
    case actionTypes.messages.chatMessage.success:
      return messages.removeIn(["waitingForAnswer", action.payload.messageUri]);

    case actionTypes.connections.sendChatMessageRefreshDataOnSuccess:
      return messages
        .setIn(
          ["waitingForAnswer", action.payload.messageUri],
          action.payload.message
        )
        .setIn(
          ["refreshDataOnSuccess", action.payload.messageUri],
          action.payload.message
        );

    case actionTypes.connections.sendChatMessageClaimOnSuccess:
      return messages
        .setIn(
          ["waitingForAnswer", action.payload.messageUri],
          action.payload.message
        )
        .setIn(
          ["claimOnSuccess", action.payload.messageUri],
          action.payload.message
        );

    case actionTypes.lostConnection:
      return messages.set("lostConnection", true).set("reconnecting", false);

    case actionTypes.initialLoadFinished: {
      return messages.set("lostConnection", false).set("reconnecting", false);
    }

    case actionTypes.account.loginFinished: {
      return messages.set("lostConnection", false).set("reconnecting", false);
    }

    case actionTypes.upgradeHttpSession: {
      return messages.set("reconnecting", true);
    }

    case actionTypes.downgradeHttpSession: {
      return messages.set("reconnecting", true);
    }

    case actionTypes.account.logoutFinished: {
      return initialState;
    }

    case actionTypes.reconnect.start:
      return messages.set("reconnecting", true);

    case actionTypes.reconnect.success:
      return messages.set("lostConnection", false).set("reconnecting", false);

    default:
      return messages;
  }
}
