import angular from "angular";
import inviewModule from "angular-inview";

import won from "../../won-es6.js";
import Immutable from "immutable";
import squareImageModule from "../square-image.js";
import connectionMessageStatusModule from "./connection-message-status.js";
import connectionMessageActionsModule from "./connection-message-actions.js";
import messageContentModule from "./message-content.js"; // due to our atom of recursivley integrating the combinedMessageContentModule within referencedMessageModule, we need to import the components here otherwise we will not be able to generate the component
import referencedMessageContentModule from "./referenced-message-content.js";
import combinedMessageContentModule from "./combined-message-content.js";
import labelledHrModule from "../labelled-hr.js";

import { connect2Redux } from "../../won-utils.js";
import { attach, getIn, get } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../selectors/general-selectors.js";
import * as messageUtils from "../../message-utils.js";
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
            uri="::self.theirAtom.get('uri')"
            ng-click="!self.multiSelectType && self.router__stateGoCurrent({viewAtomUri: self.theirAtom.get('uri'), viewConnUri: undefined})"
            ng-if="!self.isChangeNotificationMessage && !self.isSent && !(self.isGroupChatMessage && self.originatorUri)">
        </won-square-image>
        <won-square-image
            class="clickable"
            uri="::self.originatorUri"
            ng-click="!self.multiSelectType && self.router__stateGoCurrent({viewAtomUri: self.originatorUri, viewConnUri: undefined})"
            ng-if="!self.isChangeNotificationMessage && self.isReceived && self.isGroupChatMessage && self.originatorUri">
        </won-square-image>
        <won-square-image
            uri="::self.messageSenderUri"
            ng-if="!self.isChangeNotificationMessage && self.isFromSystem">
        </won-square-image>
        <div class="won-cm__center"
                ng-class="{
                  'won-cm__center--nondisplayable': self.isConnectionMessage && !self.isParsable,
                  'won-cm__center--system': self.isFromSystem,
                  'won-cm__center--inject-into': self.isInjectIntoMessage
                }"
                in-view="self.isUnread && $inview && self.markAsRead()"
                ng-if="!self.isChangeNotificationMessage">
            <div class="won-cm__center__bubble"
                ng-class="{
    			        'references' : 	self.hasReferences,
                  'pending': self.isPending,
                  'partiallyLoaded': self.isPartiallyLoaded,
                  'failure': self.isSent && self.isFailedToSend,
    			      }">
    			      <won-combined-message-content
    			        ng-if="!self.isCollapsed"
    			        message-uri="::self.messageUri"
                  connection-uri="self.connectionUri"
                  group-chat-message="self.isGroupChatMessage">
    			      </won-combined-message-content>
    			      <div class="won-cm__center__bubble__collapsed clickable"
    			        ng-if="self.isCollapsed"
    			        ng-click="self.expandMessage()">
                  {{ self.generateCollapsedLabel() }}
    			      </div>
                <div class="won-cm__center__bubble__carret clickable"
                    ng-if="!self.isGroupChatMessage && !self.isCollapsed && (self.isProposable || self.isClaimable) && !self.multiSelectType"
                    ng-click="self.toggleActions()">
                    <svg ng-show="!self.showActions">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg ng-show="self.showActions">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                </div>
                <won-connection-message-actions message-uri="::self.messageUri" connection-uri="self.connectionUri" ng-if="!self.isCollapsed && self.showActionButtons()">
                </won-connection-message-actions>
            </div>
            <won-connection-message-status message-uri="::self.messageUri" connection-uri="self.connectionUri">
            </won-connection-message-status>
            <a ng-if="self.rdfLinkURL" target="_blank" href="{{self.rdfLinkURL}}">
                <svg class="rdflink__small clickable">
                    <use xlink:href="#rdf_logo_2" href="#rdf_logo_2"></use>
                </svg>
            </a>
        </div>
        <won-labelled-hr
            ng-if="self.isChangeNotificationMessage"
            in-view="self.isUnread && $inview && self.markAsRead()"
            label="::'Post has been modified'" class="won-cm__modified">
        </won-labelled-hr>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      this.won = won;

      const selectFromState = state => {
        const ownedAtom =
          this.connectionUri &&
          getOwnedAtomByConnectionUri(state, this.connectionUri);
        const connection = getIn(ownedAtom, [
          "connections",
          this.connectionUri,
        ]);
        const theirAtom = getIn(state, [
          "atoms",
          get(connection, "targetAtomUri"),
        ]);
        const message =
          connection && this.messageUri
            ? getIn(connection, ["messages", this.messageUri])
            : Immutable.Map();

        const shouldShowRdf = getIn(state, ["view", "showRdf"]);

        let rdfLinkURL;
        if (shouldShowRdf && ownerBaseUrl && ownedAtom && message) {
          rdfLinkURL = urljoin(
            ownerBaseUrl,
            "/rest/linked-data/",
            `?requester=${this.encodeParam(get(ownedAtom, "uri"))}`,
            `&uri=${this.encodeParam(get(message, "uri"))}`,
            get(message, "outgoingMessage") ? "&deep=true" : ""
          );
        }
        const isSent = get(message, "outgoingMessage");
        const isReceived = !get(message, "outgoingMessage");
        const isFailedToSend = get(message, "failedToSend");
        const isReceivedByOwn = get(message, "isReceivedByOwn");
        const isReceivedByRemote = get(message, "isReceivedByRemote");

        // determines if the sent message is not received by any of the servers yet but not failed either
        const isPending =
          isSent && !isFailedToSend && !isReceivedByOwn && !isReceivedByRemote;

        // determines if the sent message is received by any of the servers yet but not failed either
        const isPartiallyLoaded =
          isSent &&
          !isFailedToSend &&
          (!(isReceivedByOwn && isReceivedByRemote) &&
            (isReceivedByOwn || isReceivedByRemote));

        const injectInto = get(message, "injectInto");

        return {
          ownedAtom,
          theirAtom,
          message,
          messageSenderUri: get(message, "senderUri"),
          isGroupChatMessage: this.groupChatMessage,
          originatorUri: get(message, "originatorUri"),
          isConnectionMessage: messageUtils.isConnectionMessage(message),
          isChangeNotificationMessage: messageUtils.isChangeNotificationMessage(
            message
          ),
          isSelected: getIn(message, ["viewState", "isSelected"]),
          isCollapsed: getIn(message, ["viewState", "isCollapsed"]),
          showActions: getIn(message, ["viewState", "showActions"]),
          multiSelectType: get(connection, "multiSelectType"),
          shouldShowRdf,
          rdfLinkURL,
          isParsable: messageUtils.isParsable(message),
          isClaimed: messageUtils.isMessageClaimed(message),
          isProposed: messageUtils.isMessageProposed(message),
          isAccepted: messageUtils.isMessageAccepted(message),
          isRejected: messageUtils.isMessageRejected(message),
          isRetracted: messageUtils.isMessageRetracted(message),
          isCancellationPending: messageUtils.isMessageCancellationPending(
            message
          ),
          isCancelled: messageUtils.isMessageCancelled(message),
          isProposable:
            get(connection, "state") === won.WON.Connected &&
            messageUtils.isMessageProposable(message),
          isClaimable:
            get(connection, "state") === won.WON.Connected &&
            messageUtils.isMessageClaimable(message),
          isCancelable: messageUtils.isMessageCancelable(message),
          isRetractable: messageUtils.isMessageRetractable(message),
          isRejectable: messageUtils.isMessageRejectable(message),
          isAcceptable: messageUtils.isMessageAcceptable(message),
          isUnread: messageUtils.isMessageUnread(message),
          isInjectIntoMessage: injectInto && injectInto.size > 0,
          injectInto: injectInto,
          isReceived,
          isSent,
          isFailedToSend,
          isPending,
          isPartiallyLoaded,
          isFromSystem: get(message, "systemMessage"),
          hasReferences: get(message, "hasReferences"),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri", "self.groupChatMessage"],
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
      classOnComponentRoot("won-is-proposed", () => this.isProposed, this);
      classOnComponentRoot("won-is-claimed", () => this.isClaimed, this);
      classOnComponentRoot("won-is-rejected", () => this.isRejected, this);
      classOnComponentRoot("won-is-retracted", () => this.isRetracted, this);
      classOnComponentRoot("won-is-accepted", () => this.isAccepted, this);
      classOnComponentRoot("won-is-cancelled", () => this.isCancelled, this);
      classOnComponentRoot("won-is-collapsed", () => this.isCollapsed, this);
      classOnComponentRoot(
        "won-change-notification",
        () => this.isChangeNotificationMessage,
        this
      );

      classOnComponentRoot(
        "won-is-cancellationPending",
        () => this.isCancellationPending,
        this
      );
      classOnComponentRoot("won-unread", () => this.isUnread, this);
    }

    expandMessage() {
      if (this.message && !this.multiSelectType) {
        this.messages__viewState__markAsCollapsed({
          messageUri: get(this.message, "uri"),
          connectionUri: this.connectionUri,
          atomUri: get(this.ownedAtom, "uri"),
          isCollapsed: false,
        });
      }
    }

    toggleActions() {
      this.messages__viewState__markShowActions({
        messageUri: get(this.message, "uri"),
        connectionUri: this.connectionUri,
        atomUri: get(this.ownedAtom, "uri"),
        showActions: !this.showActions,
      });
    }

    generateCollapsedLabel() {
      if (this.message) {
        let label;

        if (this.isClaimed) label = "Message was claimed.";
        else if (this.isProposed) label = "Message was proposed.";
        else if (this.isAccepted) label = "Message was accepted.";
        else if (this.isRejected) label = "Message was rejected.";
        else if (this.isRetracted) label = "Message was retracted.";
        else if (this.isCancellationPending) label = "Cancellation pending.";
        else if (this.isCancelled) label = "Cancelled.";
        else label = "Message collapsed.";

        return label + " Click to expand.";
      }
      return undefined;
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
        !this.isGroupChatMessage &&
        (this.showActions ||
          messageUtils.hasProposesReferences(this.message) ||
          messageUtils.hasClaimsReferences(this.message) ||
          messageUtils.hasProposesToCancelReferences(this.message))
      );
    }

    markAsRead() {
      if (this.isUnread) {
        const payload = {
          messageUri: this.messageUri,
          connectionUri: this.connectionUri,
          atomUri: get(this.ownedAtom, "uri"),
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
      groupChatMessage: "=",
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
    labelledHrModule,
    inviewModule.name,
  ])
  .directive("wonConnectionMessage", genComponentConf).name;
