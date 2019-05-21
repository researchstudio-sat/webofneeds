import angular from "angular";

import Immutable from "immutable";
import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../selectors/general-selectors.js";
import * as messageUtils from "../../message-utils.js";
import * as connectionUtils from "../../connection-utils.js";

import "~/style/_connection-message-actions.scss";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <button class="won-button--filled thin black"
          ng-if="self.isProposable"
          ng-disabled="self.multiSelectType || self.clicked"
          ng-click="self.sendActionMessage('proposes')">
          {{ self.getProposeLabel() }}
      </button>
      <button class="won-button--filled thin black"
          ng-if="self.isClaimable"
          ng-disabled="self.multiSelectType || self.clicked"
          ng-click="self.sendActionMessage('claims')">
          Claim
      </button>
      <button class="won-button--filled thin red"
          ng-if="self.isAcceptable"
          ng-disabled="self.multiSelectType || self.clicked"
          ng-click="self.sendActionMessage('accepts')">
        Accept
      </button>
      <button class="won-button--filled thin black"
          ng-if="self.isRejectable"
          ng-disabled="self.multiSelectType || self.clicked"
          ng-click="self.sendActionMessage('rejects')">
        Reject
      </button>
      <button class="won-button--filled thin black"
          ng-if="self.isRetractable"
          ng-disabled="self.multiSelectType || self.clicked"
          ng-click="self.sendActionMessage('retracts')">
        Retract
      </button>
      <button class="won-button--filled thin red"
          ng-if="self.isCancelable"
          ng-disabled="self.multiSelectType || self.clicked"
          ng-click="self.sendActionMessage('proposesToCancel')">
        Propose To Cancel
      </button>
      <button class="won-button--filled thin red"
          ng-if="self.isCancellationPending"
          ng-disabled="true">
        Cancellation Pending...
      </button>
      <button class="won-button--filled thin red"
          ng-if="self.isCancelled"
          ng-disabled="true">
        Cancelled
      </button>
      <button class="won-button--filled thin red"
          ng-if="self.isRejected"
          ng-disabled="true">
        Rejected
      </button>
      <button class="won-button--filled thin red"
          ng-if="self.isRetracted"
          ng-disabled="true">
        Retracted
      </button>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      this.clicked = false;

      const selectFromState = state => {
        const ownedAtom =
          this.connectionUri &&
          getOwnedAtomByConnectionUri(state, this.connectionUri);
        const connection =
          ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);
        const message =
          connection && this.messageUri
            ? getIn(connection, ["messages", this.messageUri])
            : Immutable.Map();

        return {
          ownedAtom,
          message,
          multiSelectType: connection && connection.get("multiSelectType"),
          isProposed: messageUtils.isMessageProposed(message),
          isClaimed: messageUtils.isMessageClaimed(message),
          isAccepted: messageUtils.isMessageAccepted(message),
          isRejected: messageUtils.isMessageRejected(message),
          isRetracted: messageUtils.isMessageRetracted(message),
          isCancellationPending: messageUtils.isMessageCancellationPending(
            message
          ),
          isCancelled: messageUtils.isMessageCancelled(message),
          isProposable:
            connectionUtils.isConnected(connection) &&
            messageUtils.isMessageProposable(message),
          isClaimable:
            connectionUtils.isConnected(connection) &&
            messageUtils.isMessageClaimable(message),
          isCancelable: messageUtils.isMessageCancelable(message),
          isRetractable: messageUtils.isMessageRetractable(message),
          isRejectable: messageUtils.isMessageRejectable(message),
          isAcceptable: messageUtils.isMessageAcceptable(message),
          isUnread: messageUtils.isMessageUnread(message),
          isFromSystem: message && message.get("systemMessage"),
          hasReferences: message && message.get("hasReferences"),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );
    }

    sendActionMessage(type) {
      this.clicked = true;
      this.connections__sendChatMessage(
        undefined,
        undefined,
        new Map().set(
          type,
          Immutable.Map().set(this.message.get("uri"), this.message)
        ),
        this.connectionUri,
        false
      );
    }

    getProposeLabel() {
      return this.isProposed ? "Propose (again)" : "Propose";
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      messageUri: "=",
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionMessageActions", [])
  .directive("wonConnectionMessageActions", genComponentConf).name;
