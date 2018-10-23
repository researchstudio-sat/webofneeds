import angular from "angular";

import won from "../../won-es6.js";
import Immutable from "immutable";
import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import {
  selectNeedByConnectionUri,
  isMessageProposable,
  isMessageClaimable,
  isMessageCancelable,
  isMessageRejectable,
  isMessageRetractable,
  isMessageAcceptable,
  isMessageProposed,
  isMessageClaimed,
  isMessageRejected,
  isMessageAccepted,
  isMessageRetracted,
  isMessageCancelled,
  isMessageCancellationPending,
  isMessageUnread,
} from "../../selectors.js";

import "style/_connection-message-actions.scss";

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
        const ownNeed =
          this.connectionUri &&
          selectNeedByConnectionUri(state, this.connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
        const message =
          connection && this.messageUri
            ? getIn(connection, ["messages", this.messageUri])
            : Immutable.Map();

        return {
          ownNeed,
          message,
          multiSelectType: connection && connection.get("multiSelectType"),
          isProposed: isMessageProposed(message),
          isClaimed: isMessageClaimed(message),
          isAccepted: isMessageAccepted(message),
          isRejected: isMessageRejected(message),
          isRetracted: isMessageRetracted(message),
          isCancellationPending: isMessageCancellationPending(message),
          isCancelled: isMessageCancelled(message),
          isProposable:
            connection &&
            connection.get("state") === won.WON.Connected &&
            isMessageProposable(message),
          isClaimable:
            connection &&
            connection.get("state") === won.WON.Connected &&
            isMessageClaimable(message),
          isCancelable: isMessageCancelable(message),
          isRetractable: isMessageRetractable(message),
          isRejectable: isMessageRejectable(message),
          isAcceptable: isMessageAcceptable(message),
          isUnread: isMessageUnread(message),
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
