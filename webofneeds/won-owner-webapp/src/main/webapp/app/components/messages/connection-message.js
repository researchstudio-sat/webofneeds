import angular from "angular";
import inviewModule from "angular-inview";

import won from "../../won-es6.js";
import Immutable from "immutable";
import squareImageModule from "../square-image.js";
import connectionMessageStatusModule from "./connection-message-status.js";
import messageContentModule from "./message-content.js"; // due to our need of recursivley integrating the combinedMessageContentModule within referencedMessageModule, we need to import the components here otherwise we will not be able to generate the component
import referencedMessageContentModule from "./referenced-message-content.js";
import combinedMessageContentModule from "./combined-message-content.js";

import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import {
  selectNeedByConnectionUri,
  isMessageProposable,
  isMessageCancelable,
  isMessageRejectable,
  isMessageRetractable,
  isMessageAcceptable,
  isMessageRejected,
  isMessageAccepted,
  isMessageRetracted,
  isMessageCancelled,
  isMessageCancellationPending,
  isMessageUnread,
  hasProposesReferences,
  hasProposesToCancelReferences,
} from "../../selectors.js";
import { classOnComponentRoot } from "../../cstm-ng-utils.js";

import { ownerBaseUrl } from "config";
import urljoin from "url-join";

import "style/_connection-message.scss";
import "style/_rdflink.scss";

const MESSAGE_READ_TIMEOUT = 1500;

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-square-image
            class="clickable"
            title="self.theirNeed.get('humanReadable')"
            src="self.theirNeed.get('TODOtitleImgSrc')"
            uri="self.theirNeed.get('uri')"
            ng-click="!self.multiSelectType && self.router__stateGoCurrent({postUri: self.theirNeed.get('uri')})"
            ng-if="!self.message.get('outgoingMessage')">
        </won-square-image>
        <won-square-image
            title="System"
            src=""
            uri="self.message.get('senderUri')"
            ng-if="self.message.get('systemMessage')">
        </won-square-image>
        <div class="won-cm__center"
                ng-class="{
                  'won-cm__center--nondisplayable': (self.message.get('messageType') === self.won.WONMSG.connectionMessage) && !self.message.get('isParsable'),
                  'won-cm__center--system': self.message.get('systemMessage')
                }"
                in-view="$inview && self.markAsRead()">
            <div 
                class="won-cm__center__bubble"
    			      ng-class="{
    			        'references' : 	self.message.get('hasReferences'),
                  'pending': self.isPending(),
                  'partiallyLoaded': self.isPartiallyLoaded(),
                  'failure': self.message.get('outgoingMessage') && self.message.get('failedToSend'),
    			      }">
    			      <won-combined-message-content
    			        message-uri="self.message.get('uri')"
                  connection-uri="self.connection.get('uri')">
    			      </won-combined-message-content>
                <div class="won-cm__center__bubble__carret clickable"
                    ng-if="self.isProposable && !self.multiSelectType"
                    ng-click="self.showDetail = !self.showDetail">
                    <svg ng-show="!self.showDetail">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg ng-show="self.showDetail">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                </div>
                <div class="won-cm__center__bubble__button-area"
                    ng-if="self.showDetail && !self.multiSelectType">
                    <button class="won-button--filled thin black"
                        ng-click="self.sendProposal(); self.showDetail = !self.showDetail">
                        Propose <span ng-show="self.clicked">(again)</span>
                    </button>
                    <button class="won-button--filled thin black"
                        ng-click="self.retractMessage(); self.showDetail = !self.showDetail"
                        ng-if="self.isRetractable">
                        Retract
                    </button>
                </div>
                <div class="won-cm__center__bubble__button-area" ng-if="self.showActionButtons()">
                    <button class="won-button--filled thin red"
                        ng-if="self.isAcceptable"
                        ng-disabled="self.multiSelectType || self.clicked"
                        ng-click="self.sendAccept()">
                      Accept
                    </button>
                    <button class="won-button--filled thin black"
                        ng-show="self.isRejectable"
                        ng-disabled="self.multiSelectType || self.clicked"
                        ng-click="self.rejectMessage()">
                      Reject
                    </button>
                    <button class="won-button--filled thin black"
                        ng-if="self.isRetractable"
                        ng-disabled="self.multiSelectType || self.clicked"
                        ng-click="self.retractMessage()">
                      Retract
                    </button>
                    <button class="won-button--filled thin red"
                        ng-if="self.isCancelable"
                        ng-disabled="self.multiSelectType || self.clicked"
                        ng-click="self.proposeToCancel()">
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
                </div>
            </div>
            <won-connection-message-status message-uri="self.message.get('uri')" connection-uri="self.connection.get('uri')">
            </won-connection-message-status>
            <a ng-if="self.rdfLinkURL" target="_blank" href="{{self.rdfLinkURL}}">
                <svg class="rdflink__small clickable">
                    <use xlink:href="#rdf_logo_2" href="#rdf_logo_2"></use>
                </svg>
            </a>
        </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      this.clicked = false;
      this.showDetail = false;
      this.won = won;

      const selectFromState = state => {
        const ownNeed =
          this.connectionUri &&
          selectNeedByConnectionUri(state, this.connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
        const theirNeed =
          connection && state.getIn(["needs", connection.get("remoteNeedUri")]);
        const message =
          connection && this.messageUri
            ? getIn(connection, ["messages", this.messageUri])
            : Immutable.Map();

        const shouldShowRdf = state.get("showRdf");

        let rdfLinkURL;
        if (shouldShowRdf && ownerBaseUrl && ownNeed && message) {
          rdfLinkURL = urljoin(
            ownerBaseUrl,
            "/rest/linked-data/",
            `?requester=${this.encodeParam(ownNeed.get("uri"))}`,
            `&uri=${this.encodeParam(message.get("uri"))}`,
            message.get("outgoingMessage") ? "&deep=true" : ""
          );
        }

        return {
          ownNeed,
          theirNeed,
          connection,
          message,
          isSelected: message && message.get("isSelected"),
          multiSelectType: connection && connection.get("multiSelectType"),
          shouldShowRdf,
          rdfLinkURL,
          isAccepted: isMessageAccepted(this.message),
          isRejected: isMessageRejected(this.message),
          isRetracted: isMessageRetracted(this.message),
          isCancellationPending: isMessageCancellationPending(this.message),
          isCancelled: isMessageCancelled(this.message),
          isProposable:
            connection &&
            connection.get("state") === won.WON.Connected &&
            isMessageProposable(message),
          isCancelable: isMessageCancelable(message),
          isRetractable: isMessageRetractable(message),
          isRejectable: isMessageRejectable(message),
          isAcceptable: isMessageAcceptable(message),
          isUnread: isMessageUnread(message),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );

      classOnComponentRoot(
        "won-cm--left",
        () => this.isReceivedMessage(),
        this
      );
      classOnComponentRoot(
        "won-cm--right",
        () => this.isOutgoingMessage(),
        this
      );
      classOnComponentRoot(
        "won-is-multiSelect",
        () => !!this.multiSelectType,
        this
      );
      classOnComponentRoot(
        "won-not-selectable",
        () => !this.isSelectable(),
        this
      );
      classOnComponentRoot("won-is-selected", () => this.isSelected, this);
      classOnComponentRoot("won-is-rejected", () => this.isRejected, this);
      classOnComponentRoot("won-is-retracted", () => this.isRetracted, this);
      classOnComponentRoot("won-is-accepted", () => this.isAccepted, this);
      classOnComponentRoot("won-is-cancelled", () => this.isCancelled, this);
      classOnComponentRoot(
        "won-is-cancellationPending",
        () => this.isCancellationPending,
        this
      );
      classOnComponentRoot("won-unread", () => this.isUnread, this);
    }

    isSelectable() {
      if (this.message && this.multiSelectType) {
        switch (this.multiSelectType) {
          case "rejects":
            return this.isRejectable;
          case "retracts":
            return this.isRetractable;
          case "proposesToCancel":
            return this.isCancelable;
          case "accepts":
            return this.isAcceptable;
          case "proposes":
            return this.isProposable;
        }
      }
      return false;
    }

    isReceivedMessage() {
      return this.message && !this.message.get("outgoingMessage");
    }

    isOutgoingMessage() {
      return this.message && this.message.get("outgoingMessage");
    }

    showActionButtons() {
      return (
        hasProposesReferences(this.message) ||
        hasProposesToCancelReferences(this.message)
      );
    }

    markAsRead() {
      if (this.isUnread) {
        const payload = {
          messageUri: this.message.get("uri"),
          connectionUri: this.connectionUri,
          needUri: this.ownNeed.get("uri"),
        };

        const tmp_messages__markAsRead = this.messages__markAsRead;

        setTimeout(function() {
          tmp_messages__markAsRead(payload);
        }, MESSAGE_READ_TIMEOUT);
      }
    }

    markAsAccepted(accepted) {
      const payload = {
        messageUri: this.message.get("uri"),
        connectionUri: this.connectionUri,
        needUri: this.ownNeed.get("uri"),
        accepted: accepted,
      };

      this.messages__messageStatus__markAsAccepted(payload);
    }

    markAsRejected(rejected) {
      const payload = {
        messageUri: this.message.get("uri"),
        connectionUri: this.connectionUri,
        needUri: this.ownNeed.get("uri"),
        rejected: rejected,
      };

      this.messages__messageStatus__markAsRejected(payload);
    }

    markAsRetracted(retracted) {
      const payload = {
        messageUri: this.message.get("uri"),
        connectionUri: this.connectionUri,
        needUri: this.ownNeed.get("uri"),
        retracted: retracted,
      };

      this.messages__messageStatus__markAsRetracted(payload);
    }

    markAsCancelled(cancelled) {
      const payload = {
        messageUri: this.message.get("uri"),
        connectionUri: this.connectionUri,
        needUri: this.ownNeed.get("uri"),
        cancelled: cancelled,
      };

      this.messages__messageStatus__markAsCancelled(payload);
    }

    markAsCancellationPending(cancellationPending) {
      const payload = {
        messageUri: this.message.get("uri"),
        connectionUri: this.connectionUri,
        needUri: this.ownNeed.get("uri"),
        cancellationPending: cancellationPending,
      };

      this.messages__messageStatus__markAsCancellationPending(payload);
    }

    sendProposal() {
      this.clicked = true;
      this.sendActionMessage("proposes");
    }

    proposeToCancel() {
      this.clicked = true;
      this.sendActionMessage("proposesToCancel");

      this.markAsCancellationPending(true);
    }

    sendAccept() {
      this.clicked = true;
      this.sendActionMessage("accepts");

      this.markAsAccepted(true);
    }

    retractMessage() {
      this.clicked = true;
      this.sendActionMessage("retracts");

      this.markAsRetracted(true);
    }

    rejectMessage() {
      this.clicked = true;
      this.sendActionMessage("rejects");

      this.markAsRejected(true);
    }

    sendActionMessage(type) {
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

    /**
     * determines if the sent message is not received by any of the servers yet but not failed either
     */
    isPending() {
      const pending =
        this.message &&
        this.message.get("outgoingMessage") &&
        !this.message.get("failedToSend") &&
        !this.message.get("isReceivedByOwn") &&
        !this.message.get("isReceivedByRemote");

      return pending;
    }

    /**
     * determines if the sent message is received by any of the servers yet but not failed either
     */
    isPartiallyLoaded() {
      const partiallyLoaded =
        this.message &&
        this.message.get("outgoingMessage") &&
        !this.message.get("failedToSend") &&
        (!(
          this.message.get("isReceivedByOwn") &&
          this.message.get("isReceivedByRemote")
        ) &&
          (this.message.get("isReceivedByOwn") ||
            this.message.get("isReceivedByRemote")));

      return partiallyLoaded;
    }

    encodeParam(param) {
      return encodeURIComponent(param);
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
  .module("won.owner.components.connectionMessage", [
    squareImageModule,
    connectionMessageStatusModule,
    messageContentModule,
    referencedMessageContentModule,
    combinedMessageContentModule,
    inviewModule.name,
  ])
  .directive("wonConnectionMessage", genComponentConf).name;
