import angular from "angular";

import Immutable from "immutable";
import { connect2Redux } from "../won-utils.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { selectNeedByConnectionUri } from "../selectors.js";
import { labels } from "../won-label-utils.js";
import messageContentModule from "./message-content.js";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="refmsgcontent__fragment" ng-if="self.hasProposeUris">
        <div class="refmsgcontent__fragment__header">Proposes</div>
        <div class="refmsgcontent__fragment__body">
          <won-message-content
            ng-repeat="msgUri in self.proposeUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasRetractUris">
        <div class="refmsgcontent__fragment__header">Retracts</div>
        <div class="refmsgcontent__fragment__body">
          <won-message-content
            ng-repeat="msgUri in self.retractUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasAcceptUris">
        <div class="refmsgcontent__fragment__header">Accepts</div>
        <div class="refmsgcontent__fragment__body">
          <won-message-content
            ng-repeat="msgUri in self.acceptUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasProposeToCancelUris">
        <div class="refmsgcontent__fragment__header">Propose to cancel</div>
        <div class="refmsgcontent__fragment__body">
          <won-message-content
            ng-repeat="msgUri in self.proposeToCancelUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasRejectUris">
        <div class="refmsgcontent__fragment__header">Rejects</div>
        <div class="refmsgcontent__fragment__body">
          <won-message-content
            ng-repeat="msgUri in self.rejectUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-message-content>
        </div>
      </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      this.labels = labels;

      this.noParsableContentPlaceholder =
        "«This message couldn't be displayed as it didn't contain," +
        "any parsable content! " +
        'Click on the "Show raw RDF data"-button in ' +
        'the main-menu on the right side of the navigationbar to see the "raw" message-data.»';

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

        const chatMessages = connection && connection.get("messages");

        const references = message && message.get("references");

        const rejectUris = references && references.get("rejects");
        const retractUris = references && references.get("retracts");
        const proposeUris = references && references.get("proposes");
        const proposeToCancelUris =
          references && references.get("proposesToCancel");
        const acceptUris = references && references.get("accepts");

        const hasAcceptUris = acceptUris && acceptUris.size > 0;
        const hasProposeUris = proposeUris && proposeUris.size > 0;
        const hasProposeToCancelUris =
          proposeToCancelUris && proposeToCancelUris.size > 0;
        const hasRetractUris = retractUris && retractUris.size > 0;
        const hasRejectUris = rejectUris && rejectUris.size > 0;

        return {
          headerText: "Special Message",
          chatMessages: chatMessages,
          connection,
          hasProposeUris,
          hasAcceptUris,
          hasProposeToCancelUris,
          hasRetractUris,
          hasRejectUris,
          proposeUrisArray: hasProposeUris && Array.from(proposeUris.toSet()),
          retractUrisArray: hasRetractUris && Array.from(retractUris.toSet()),
          rejectUrisArray: hasRejectUris && Array.from(rejectUris.toSet()),
          proposeToCancelUrisArray:
            hasProposeToCancelUris && Array.from(proposeToCancelUris.toSet()),
          acceptUrisArray: hasAcceptUris && Array.from(acceptUris.toSet()),
          message,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );
    }

    getReferencedMessage(messageUri) {
      let referencedMessage =
        this.chatMessages && this.chatMessages.get(messageUri);
      if (referencedMessage) {
        return referencedMessage;
      } else {
        const foundMessages =
          this.chatMessages &&
          this.chatMessages.filter(msg => msg.get("remoteUri") === messageUri);
        if (foundMessages && foundMessages.size > 0) {
          return foundMessages.first();
        } else {
          return undefined;
        }
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
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.referencedMessageContent", [
    messageContentModule,
  ])
  .directive("wonReferencedMessageContent", genComponentConf).name;
