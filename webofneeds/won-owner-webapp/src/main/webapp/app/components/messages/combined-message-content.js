import angular from "angular";

import won from "../../won-es6.js";
import { connect2Redux } from "../../configRedux.js";
import { attach, get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import {
  getOwnedAtomByConnectionUri,
  getAtoms,
} from "../../redux/selectors/general-selectors.js";
import { getOwnedConnections } from "../../redux/selectors/connection-selectors.js";
import trigModule from "../trig.js";
import { labels } from "../../won-label-utils.js";
import { classOnComponentRoot } from "../../cstm-ng-utils.js";
import squareImageModule from "../square-image.js";

import "~/style/_combined-message-content.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
      <div class="msg__header" ng-if="!self.isConnectionMessage && !self.hasNotBeenLoaded">
          <div class="msg__header__type">{{ self.getTypeHeaderLabel() }}</div>
      </div>
      <div class="msg__header" ng-if="self.personaName && self.isConnectionMessage && !self.hasNotBeenLoaded && !(self.hasClaims || self.hasProposes)">
          <div class="msg__header__type">{{ self.personaName }}</div>
      </div>
      <div class="msg__header msg__header--agreement" ng-if="self.isConnectionMessage && (self.hasClaims || self.hasProposes) && !self.hasNotBeenLoaded">
          <div class="msg__header__type">{{ self.getAgreementHeaderLabel() }}</div>
      </div>
      <div class="msg__header msg__header--forwarded-from" ng-if="self.isConnectionMessage && self.originatorUri && !self.hasNotBeenLoaded && !self.isGroupChatMessage">
          <div class="msg__header__type">Forwarded from:</div>
          <won-square-image
            class="msg__header__originator"
            uri="::self.originatorUri">
          </won-square-image>
      </div>
      <div class="msg__header msg__header--inject-into" ng-if="self.isConnectionMessage && self.isInjectIntoMessage && !self.hasNotBeenLoaded && !self.isGroupChatMessage">
          <div class="msg__header__type">Forward to:</div>
          <won-square-image
            class="msg__header__inject"
            ng-class="{'clickable': self.isInjectIntoConnectionPresent(connUri)}"
            ng-repeat="connUri in self.injectIntoArray"
            uri="self.getInjectIntoAtomUri(connUri)"
            ng-click="!self.multiSelectType && self.isInjectIntoConnectionPresent(connUri) && self.router__stateGoCurrent({connectionUri: connUri})">
          </won-square-image>
      </div>
      <won-message-content
          ng-if="self.hasContent || self.hasNotBeenLoaded"
          message-uri="::self.messageUri"
          connection-uri="::self.connectionUri">
      </won-message-content>
      <won-referenced-message-content
          ng-if="self.hasReferences"
          message-uri="::self.messageUri"
          connection-uri="::self.connectionUri">
      </won-referenced-message-content>
      <won-trig
          trig="self.contentGraphTrig"
          ng-if="self.shouldShowRdf && self.contentGraphTrig">
      </won-trig>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const ownedAtom =
          this.connectionUri &&
          getOwnedAtomByConnectionUri(state, this.connectionUri);
        const connection =
          ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);

        const message =
          connection &&
          this.messageUri &&
          getIn(connection, ["messages", this.messageUri]);

        const messageType = message && message.get("messageType");
        const injectInto = message && message.get("injectInto");

        const hasReferences = message && message.get("hasReferences");
        const references = message && message.get("references");
        const referencesProposes = references && references.get("proposes");
        const referencesClaims = references && references.get("claims");

        const allConnections = getOwnedConnections(state);
        const allAtoms = getAtoms(state);

        /*Extract persona name from message:

         either within the atom of the originatorUri-atom (in group-chat-messages)
         or
         within the targetAtomUri-atom of the connection (for 1:1 chats)
         */
        const relevantAtomUri =
          !get(message, "outgoingMessage") &&
          (this.groupChatMessage
            ? get(message, "originatorUri")
            : get(connection, "targetAtomUri"));
        const relevantPersonaUri =
          relevantAtomUri && getIn(allAtoms, [relevantAtomUri, "heldBy"]);
        const personaName =
          relevantPersonaUri &&
          getIn(allAtoms, [relevantPersonaUri, "content", "personaName"]);

        return {
          allAtoms,
          allConnections,
          personaName,
          multiSelectType: connection && connection.get("multiSelectType"),
          contentGraphTrig: get(message, "contentGraphTrigRaw"),
          shouldShowRdf: state.getIn(["view", "showRdf"]),
          hasContent: message && message.get("hasContent"),
          hasNotBeenLoaded: !message,
          hasReferences,
          hasClaims: referencesClaims && referencesClaims.size > 0,
          hasProposes: referencesProposes && referencesProposes.size > 0,
          messageStatus: message && message.get("messageStatus"),
          isGroupChatMessage: this.groupChatMessage,
          isInjectIntoMessage: injectInto && injectInto.size > 0, //contains the targetConnectionUris
          originatorUri: message && message.get("originatorUri"),
          injectIntoArray: injectInto && Array.from(injectInto.toSet()),
          messageType,
          isConnectionMessage: messageType === won.WONMSG.connectionMessage,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri", "self.groupChatMessage"],
        this
      );

      classOnComponentRoot(
        "won-has-non-ref-content",
        () =>
          !this.isConnectionMessage ||
          this.hasContent ||
          this.hasNotBeenLoaded ||
          this.hasClaims ||
          this.hasProposes ||
          this.originatorUri,
        this
      );
      classOnComponentRoot(
        "won-has-ref-content",
        () => this.hasReferences,
        this
      );
    }

    getTypeHeaderLabel() {
      const headerLabel = labels.messageType[this.messageType];
      return headerLabel || this.messageType;
    }

    getAgreementHeaderLabel() {
      if (this.hasClaims && this.hasProposes) {
        if (this.messageStatus) {
          if (this.messageStatus.get("isCancelled"))
            return "Agreement/Claim - Cancelled";
          if (this.messageStatus.get("isCancellationPending"))
            return "Agreement/Claim - Accepted(Pending Cancellation)";
          if (this.messageStatus.get("isAccepted"))
            return "Agreement/Claim - Accepted";
          if (this.messageStatus.get("isRetracted"))
            return "Proposal/Claim - Retracted";
          if (this.messageStatus.get("isRejected"))
            return "Proposal/Claim - Rejected";
        }
        return "Proposal/Claim";
      } else if (this.hasClaims) {
        if (this.messageStatus) {
          if (this.messageStatus.get("isCancelled")) return "Claim - Cancelled";
          if (this.messageStatus.get("isCancellationPending"))
            return "Claim - Accepted(Pending Cancellation)";
          if (this.messageStatus.get("isAccepted")) return "Claim - Accepted";
          if (this.messageStatus.get("isRetracted")) return "Claim - Retracted";
          if (this.messageStatus.get("isRejected")) return "Claim - Rejected";
        }
        return "Claim";
      } else if (this.hasProposes) {
        if (this.messageStatus) {
          if (this.messageStatus.get("isCancelled"))
            return "Agreement - Cancelled";
          if (this.messageStatus.get("isCancellationPending"))
            return "Agreement - Pending Cancellation";
          if (this.messageStatus.get("isAccepted")) return "Agreement";
          if (this.messageStatus.get("isRetracted"))
            return "Proposal - Retracted";
          if (this.messageStatus.get("isRejected"))
            return "Proposal - Rejected";
        }
        return "Proposal";
      }
      //should never happen
      return undefined;
    }

    getInjectIntoAtomUri(connectionUri) {
      //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
      let connection =
        this.allConnections && this.allConnections.get(connectionUri);

      if (connection) {
        return connection.get("targetAtomUri");
      } else {
        connection =
          this.allConnections &&
          this.allConnections.find(
            conn => conn.get("targetConnectionUri") === connectionUri
          );

        if (connection) {
          return connection.get("targetAtomUri");
        }
      }
      return undefined;
    }

    getInjectIntoAtomTitle(connectionUri) {
      //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
      const injectIntoAtomUri = this.getInjectIntoAtomUri(connectionUri);
      const injectIntoAtom =
        injectIntoAtomUri && this.allAtoms.get(injectIntoAtomUri);

      return injectIntoAtom && injectIntoAtom.get("humanReadable");
    }

    isInjectIntoConnectionPresent(connectionUri) {
      //TODO: THIS MIGHT BE A CONNECTION THAT WE DONT EVEN OWN, SO WE NEED TO BE MORE SMART ABOUT IT
      let connection =
        this.allConnections && this.allConnections.get(connectionUri);

      if (connection) {
        return true;
      } else {
        return (
          this.allConnections &&
          !!this.allConnections.find(
            conn => conn.get("targetConnectionUri") === connectionUri
          )
        );
      }
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
  .module("won.owner.components.combinedMessageContent", [
    trigModule,
    squareImageModule,
  ])
  .directive("wonCombinedMessageContent", genComponentConf).name;
