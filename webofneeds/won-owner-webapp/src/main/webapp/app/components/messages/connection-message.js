import angular from "angular";
import inviewModule from "angular-inview";

import won from "../../won-es6.js";
import Immutable from "immutable";
import squareImageModule from "../square-image.js";
import connectionMessageStatusModule from "./connection-message-status.js";
import connectionMessageActionsModule from "./connection-message-actions.js";
import messageContentModule from "./message-content.js"; // due to our need of recursivley integrating the combinedMessageContentModule within referencedMessageModule, we need to import the components here otherwise we will not be able to generate the component
import referencedMessageContentModule from "./referenced-message-content.js";
import combinedMessageContentModule from "./combined-message-content.js";

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
  isMessageRejected,
  isMessageAccepted,
  isMessageRetracted,
  isMessageCancelled,
  isMessageCancellationPending,
  isMessageUnread,
  hasProposesReferences,
  hasClaimsReferences,
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
            ng-if="!self.isSent">
        </won-square-image>
        <won-square-image
            title="System"
            src=""
            uri="self.messageSenderUri"
            ng-if="self.isFromSystem">
        </won-square-image>
        <div class="won-cm__center"
                ng-class="{
                  'won-cm__center--nondisplayable': self.isConnectionMessage && !self.isParsable,
                  'won-cm__center--system': self.isFromSystem,
                  'won-cm__center--inject-into': self.isInjectIntoMessage
                }"
                in-view="$inview && self.markAsRead()">
            <div class="won-cm__center__bubble"
    			      ng-class="{
    			        'references' : 	self.hasReferences,
                  'pending': self.isPending,
                  'partiallyLoaded': self.isPartiallyLoaded,
                  'failure': self.isSent && self.isFailedToSend,
    			      }">
    			      <won-combined-message-content
    			        message-uri="self.messageUri"
                  connection-uri="self.connectionUri">
    			      </won-combined-message-content>
                <div class="won-cm__center__bubble__carret clickable"
                    ng-if="(self.isProposable || self.isClaimable) && !self.multiSelectType"
                    ng-click="self.showDetail = !self.showDetail">
                    <svg ng-show="!self.showDetail">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg ng-show="self.showDetail">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                </div>
                <won-connection-message-actions message-uri="self.messageUri" connection-uri="self.connectionUri" ng-if="self.showActionButtons()">
                </won-connection-message-actions>
            </div>
            <won-connection-message-status message-uri="self.messageUri" connection-uri="self.connectionUri">
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
        const isSent = message && message.get("outgoingMessage");
        const isReceived = message && !message.get("outgoingMessage");
        const isFailedToSend = message && message.get("failedToSend");
        const isReceivedByOwn = message && message.get("isReceivedByOwn");
        const isReceivedByRemote = message && message.get("isReceivedByRemote");

        // determines if the sent message is not received by any of the servers yet but not failed either
        const isPending =
          isSent && !isFailedToSend && !isReceivedByOwn && !isReceivedByRemote;

        // determines if the sent message is received by any of the servers yet but not failed either
        const isPartiallyLoaded =
          isSent &&
          !isFailedToSend &&
          (!(isReceivedByOwn && isReceivedByRemote) &&
            (isReceivedByOwn || isReceivedByRemote));

        const injectInto = message && message.get("injectInto");

        return {
          ownNeed,
          theirNeed,
          message,
          messageSenderUri: message && message.get("senderUri"),
          isConnectionMessage:
            message &&
            message.get("messageType") === won.WONMSG.connectionMessage,
          isSelected: message && message.get("isSelected"),
          multiSelectType: connection && connection.get("multiSelectType"),
          shouldShowRdf,
          rdfLinkURL,
          isParsable: message.get("isParsable"),
          isAccepted: isMessageAccepted(this.message),
          isRejected: isMessageRejected(this.message),
          isRetracted: isMessageRetracted(this.message),
          isCancellationPending: isMessageCancellationPending(this.message),
          isCancelled: isMessageCancelled(this.message),
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
          isInjectIntoMessage: injectInto && injectInto.size > 0,
          injectInto: injectInto,
          isReceived,
          isSent,
          isFailedToSend,
          isPending,
          isPartiallyLoaded,
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

      classOnComponentRoot("won-cm--left", () => this.isReceived, this);
      classOnComponentRoot("won-cm--right", () => this.isSent, this);
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
          case "claims":
            return this.isClaimable;
        }
      }
      return false;
    }

    showActionButtons() {
      return (
        this.showDetail ||
        hasProposesReferences(this.message) ||
        hasClaimsReferences(this.message) ||
        hasProposesToCancelReferences(this.message)
      );
    }

    markAsRead() {
      if (this.isUnread) {
        const payload = {
          messageUri: this.messageUri,
          connectionUri: this.connectionUri,
          needUri: this.ownNeed.get("uri"),
        };

        const tmp_messages__markAsRead = this.messages__markAsRead;

        setTimeout(function() {
          tmp_messages__markAsRead(payload);
        }, MESSAGE_READ_TIMEOUT);
      }
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
    connectionMessageActionsModule,
    messageContentModule,
    referencedMessageContentModule,
    combinedMessageContentModule,
    inviewModule.name,
  ])
  .directive("wonConnectionMessage", genComponentConf).name;
