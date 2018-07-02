import angular from "angular";
import inviewModule from "angular-inview";

import won from "../won-es6.js";
import Immutable from "immutable";
import squareImageModule from "./square-image.js";
import labelledHrModule from "./labelled-hr.js";
import connectionMessageStatusModule from "./connection-message-status.js";
import messageContentModule from "./message-content.js";
import referencedMessageContentModule from "./referenced-message-content.js";
import trigModule from "./trig.js";
import { connect2Redux } from "../won-utils.js";
import { attach, get, getIn } from "../utils.js";
import {
  buildProposalMessage,
  buildModificationMessage,
} from "../won-message-utils.js";
import { actionCreators } from "../actions/actions.js";
import { selectNeedByConnectionUri } from "../selectors.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import { ownerBaseUrl } from "config";
import urljoin from "url-join";

const MESSAGE_READ_TIMEOUT = 1500;

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
        <won-square-image
            title="self.theirNeed.get('title')"
            src="self.theirNeed.get('TODOtitleImgSrc')"
            uri="self.theirNeed.get('uri')"
            ng-click="self.router__stateGoCurrent({postUri: self.theirNeed.get('uri')})"
            ng-if="!self.message.get('outgoingMessage')">
        </won-square-image>
        <div class="won-cm__center"
                ng-class="{'won-cm__center--nondisplayable': (self.message.get('messageType') === self.won.WONMSG.connectionMessage) && !self.message.get('isParsable')}"
                in-view="$inview && self.markAsRead()">
            <div 
                class="won-cm__center__bubble" 
                title="{{ self.shouldShowRdf ? self.rdfToString(self.message.get('contentGraphs')) : undefined }}"
    			      ng-class="{
    			        'references' : 	self.message.get('hasReferences'),
                  'pending': self.isPending(),
                  'partiallyLoaded': self.isPartiallyLoaded(),
                  'failure': self.message.get('outgoingMessage') && self.message.get('failedToSend'),
    			      }">
    			      <div class="won-cm__center__bubble__content">
                  <won-message-content
                      ng-if="!self.isConnectionMessage() || self.message.get('hasContent')"
                      message-uri="self.message.get('uri')"
                      connection-uri="self.connection.get('uri')">
                  </won-message-content>
                  <won-referenced-message-content
                      ng-if="self.message.get('hasReferences')"
                      message-uri="self.message.get('uri')"
                      connection-uri="self.connection.get('uri')">
                  </won-referenced-message-content>
                </div>
                <div class="won-cm__center__bubble__carret clickable"
                    ng-if="self.allowProposals"
                    ng-click="self.showDetail = !self.showDetail">
                    <svg ng-show="!self.showDetail">
                        <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
                    </svg>
                    <svg ng-show="self.showDetail">
                        <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                    </svg>
                </div>
                <won-trig
                    trig="self.contentGraphTrig"
                    ng-show="self.shouldShowRdf && self.contentGraphTrig">
                </won-trig>
                <div class="won-cm__center__bubble__button-area"
                    ng-if="self.showDetail">
                    <button class="won-button--filled thin black"
                        ng-click="self.sendProposal(); self.showDetail = !self.showDetail">
                        Propose <span ng-show="self.clicked">(again)</span>
                    </button>
                    <button class="won-button--filled thin black"
                        ng-click="self.retractMessage(); self.showDetail = !self.showDetail"
                        ng-if="self.message.get('outgoingMessage')">
                        Retract
                    </button>
                </div>
                <div class="won-cm__center__bubble__button-area" ng-if="(self.message.get('isProposeMessage') || self.message.get('isProposeToCancel'))">
                    <button class="won-button--filled thin red"
                        ng-if="!self.message.get('outgoingMessage')"
                        ng-disabled="self.clicked"
                        ng-click="self.sendAccept()">
                      Accept
                    </button>
                    <button class="won-button--filled thin black"
                        ng-show="!self.message.get('outgoingMessage')"
                        ng-disabled="self.clicked"
                        ng-click="self.rejectMessage()">
                      Reject
                    </button>
                    <button class="won-button--filled thin black"
                        ng-if="self.message.get('outgoingMessage') && !self.message.get('isAcceptMessage')"
                        ng-disabled="self.clicked"
                        ng-click="self.retractMessage()">
                      Retract
                    </button>
                </div>
            </div>
            <won-connection-message-status message-uri="self.message.get('uri')" connection-uri="self.connection.get('uri')">
            </won-connection-messages-status>
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
          contentGraphs: get(message, "contentGraphs") || Immutable.List(),
          contentGraphTrig: get(message, "contentGraphTrigRaw"),
          shouldShowRdf,
          rdfLinkURL,
          allowProposals:
            connection &&
            connection.get("state") === won.WON.Connected &&
            !message.get("hasReferences") &&
            message.get("hasContent"), //allow showing details only when the connection is already present
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
        "won-cm--system",
        () => this.isSystemMessage(),
        this
      );
      classOnComponentRoot("won-is-rejected", () => this.isRejected(), this);
      classOnComponentRoot("won-is-retracted", () => this.isRetracted(), this);
      classOnComponentRoot("won-is-accepted", () => this.isAccepted(), this);
      classOnComponentRoot("won-is-cancelled", () => this.isCancelled(), this);
      classOnComponentRoot(
        "won-is-cancellationPending",
        () => this.isCancellationPending(),
        this
      );
      classOnComponentRoot("won-unread", () => this.isUnread(), this);
    }

    isReceivedMessage() {
      return this.message && !this.message.get("outgoingMessage");
    }

    isSystemMessage() {
      //TODO: IMPLEMENT THIS METHOD
      return false;
    }

    isOutgoingMessage() {
      return this.message && this.message.get("outgoingMessage");
    }

    isUnread() {
      return this.message && this.message.get("unread");
    }

    isRejected() {
      const messageStatus = this.message && this.message.get("messageStatus");
      return messageStatus && messageStatus.get("isRejected");
    }
    isAccepted() {
      const messageStatus = this.message && this.message.get("messageStatus");
      return messageStatus && messageStatus.get("isAccepted");
    }
    isRetracted() {
      const messageStatus = this.message && this.message.get("messageStatus");
      return messageStatus && messageStatus.get("isRetracted");
    }
    isCancelled() {
      const messageStatus = this.message && this.message.get("messageStatus");
      return messageStatus && messageStatus.get("isCancelled");
    }
    isCancellationPending() {
      const messageStatus = this.message && this.message.get("messageStatus");
      return messageStatus && messageStatus.get("isCancellationPending");
    }

    markAsRead() {
      if (this.message && this.message.get("unread")) {
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
      const uri = this.message.get("remoteUri")
        ? this.message.get("remoteUri")
        : this.message.get("uri");
      const trimmedMsg = buildProposalMessage(
        uri,
        "proposes",
        "Ok, I am hereby making a proposal"
      );
      this.connections__sendChatMessage(trimmedMsg, this.connectionUri, true);

      this.onSendProposal({ proposalUri: uri });
    }

    proposeToCancel() {
      this.clicked = true;
      const uri = this.isOwn
        ? this.message.get("uri")
        : this.message.get("remoteUri");
      const msg = "Propose to cancel agreement : " + uri;
      const trimmedMsg = buildProposalMessage(uri, "proposesToCancel", msg);
      this.connections__sendChatMessage(trimmedMsg, this.connectionUri, true);

      this.onUpdate();
    }

    sendAccept() {
      this.clicked = true;
      const trimmedMsg = buildProposalMessage(
        this.message.get("remoteUri"),
        "accepts",
        "I accept the following proposition"
      );
      this.connections__sendChatMessage(trimmedMsg, this.connectionUri, true);

      this.markAsAccepted(true);
      this.onRemoveData({ proposalUri: this.messageUri });
    }

    retractMessage() {
      this.clicked = true;
      const uri = this.message.get("remoteUri")
        ? this.message.get("remoteUri")
        : this.message.get("uri");
      const trimmedMsg = buildModificationMessage(
        uri,
        "retracts",
        "Retracting the message"
      );
      this.connections__sendChatMessage(trimmedMsg, this.connectionUri, true);

      this.markAsRetracted(true);
      this.onUpdate();
    }

    rejectMessage() {
      this.clicked = true;
      const uri = this.message.get("remoteUri")
        ? this.message.get("remoteUri")
        : this.message.get("uri");
      const trimmedMsg = buildProposalMessage(
        uri,
        "rejects",
        "Rejecting the message"
      );
      this.connections__sendChatMessage(trimmedMsg, this.connectionUri, true);

      this.markAsRejected(true);
      this.onUpdate();
    }

    rdfToString(jsonld) {
      return JSON.stringify(jsonld);
    }

    /**
     * determines if the sent message is not received by any of the servers yet but not failed either
     */
    isPending() {
      return (
        this.message.get("outgoingMessage") &&
        !this.message.get("failedToSend") &&
        !this.message.get("isReceivedByOwn") &&
        !this.message.get("isReceivedByRemote")
      );
    }

    /**
     * determines if the sent message is received by any of the servers yet but not failed either
     */
    isPartiallyLoaded() {
      return (
        this.message.get("outgoingMessage") &&
        !this.message.get("failedToSend") &&
        (!(
          this.message.get("isReceivedByOwn") &&
          this.message.get("isReceivedByRemote")
        ) &&
          (this.message.get("isReceivedByOwn") ||
            this.message.get("isReceivedByRemote")))
      );
    }

    encodeParam(param) {
      return encodeURIComponent(param);
    }

    isConnectionMessage() {
      return this.message.get("messageType") === won.WONMSG.connectionMessage;
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
      // Usage: on-update="::myCallback(draft)"
      onUpdate: "&",
      onSendProposal: "&",
      onRemoveData: "&",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionMessage", [
    squareImageModule,
    labelledHrModule,
    connectionMessageStatusModule,
    messageContentModule,
    referencedMessageContentModule,
    inviewModule.name,
    trigModule,
  ])
  .directive("wonConnectionMessage", genComponentConf).name;
