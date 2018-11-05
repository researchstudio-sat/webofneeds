import angular from "angular";

import Immutable from "immutable";
import won from "../../won-es6.js";
import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { selectNeedByConnectionUri } from "../../selectors/selectors.js";
import { labels } from "../../won-label-utils.js";
import { fetchMessage } from "../../won-message-utils.js";
import { classOnComponentRoot } from "../../cstm-ng-utils.js";

import "style/_referenced-message-content.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];

function genComponentConf() {
  let template = `
      <div class="refmsgcontent__fragment" ng-if="self.hasClaimUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('claims')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('claims')}">
          <div class="refmsgcontent__fragment__header__label">Claiming {{ self.getCountString(self.claimUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret">
              <svg ng-show="!self.isReferenceExpanded('claims')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('claims')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('claims')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.claimUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasProposeUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('proposes')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('proposes')}">
          <div class="refmsgcontent__fragment__header__label">Proposing {{ self.getCountString(self.proposeUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret">
              <svg ng-show="!self.isReferenceExpanded('proposes')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('proposes')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('proposes')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.proposeUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasRetractUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('retracts')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('retracts')}">
          <div class="refmsgcontent__fragment__header__label">Retracting {{ self.getCountString(self.retractUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret clickable">
              <svg ng-show="!self.isReferenceExpanded('retracts')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('retracts')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('retracts')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.retractUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasAcceptUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('accepts')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('accepts')}">
          <div class="refmsgcontent__fragment__header__label">Accepting {{ self.getCountString(self.acceptUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret">
              <svg ng-show="!self.isReferenceExpanded('accepts')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('accepts')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('accepts')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.acceptUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasProposeToCancelUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('proposesToCancel')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('proposesToCancel')}">
          <div class="refmsgcontent__fragment__header__label">Proposing to cancel {{ self.getCountString(self.proposeToCancelUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret">
              <svg ng-show="!self.isReferenceExpanded('proposesToCancel')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('proposesToCancel')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('proposesToCancel')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.proposeToCancelUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasRejectUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('rejects')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('rejects')}">
          <div class="refmsgcontent__fragment__header__label">Rejecting {{ self.getCountString(self.rejectUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret">
              <svg ng-show="!self.isReferenceExpanded('rejects')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('rejects')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('rejects')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.rejectUrisArray"
            ng-class="{
              'won-cm--left' : self.getReferencedMessage(msgUri) && !self.getReferencedMessage(msgUri).get('outgoingMessage'),
              'won-cm--right' : self.getReferencedMessage(msgUri) && self.getReferencedMessage(msgUri).get('outgoingMessage'),
            }"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
      <div class="refmsgcontent__fragment" ng-if="self.hasForwardUris">
        <div class="refmsgcontent__fragment__header"
          ng-click="self.toggleReferenceExpansion('forwards')"
          ng-class="{'refmsgcontent__fragment__header--collapsed': !self.isReferenceExpanded('forwards')}">
          <div class="refmsgcontent__fragment__header__label">Forwarding {{ self.getCountString(self.forwardUrisSize)}}</div>
          <div class="refmsgcontent__fragment__header__carret">
              <svg ng-show="!self.isReferenceExpanded('forwards')">
                  <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
              </svg>
              <svg ng-show="self.isReferenceExpanded('forwards')">
                  <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
              </svg>
          </div>
        </div>
        <div class="refmsgcontent__fragment__body" ng-if="self.isReferenceExpanded('forwards')">
          <won-combined-message-content
            ng-click="self.loadMessage(msgUri)"
            ng-repeat="msgUri in self.forwardUrisArray"
            class="won-cm--forward"
            message-uri="self.getReferencedMessage(msgUri).get('uri')"
            connection-uri="self.connection.get('uri')">
          </won-combined-message-content>
        </div>
      </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      this.labels = labels;

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

        const expandedReferences =
          message && message.getIn(["viewState", "expandedReferences"]);

        const chatMessages = connection && connection.get("messages");

        const references = message && message.get("references");

        const rejectUris = references && references.get("rejects");
        const retractUris = references && references.get("retracts");
        const proposeUris = references && references.get("proposes");
        const proposeToCancelUris =
          references && references.get("proposesToCancel");
        const acceptUris = references && references.get("accepts");
        const forwardUris = references && references.get("forwards");
        const claimUris = references && references.get("claims");

        const acceptUrisSize = acceptUris ? acceptUris.size : 0;
        const proposeUrisSize = proposeUris ? proposeUris.size : 0;
        const proposeToCancelUrisSize = proposeToCancelUris
          ? proposeToCancelUris.size
          : 0;
        const rejectUrisSize = rejectUris ? rejectUris.size : 0;
        const retractUrisSize = retractUris ? retractUris.size : 0;
        const forwardUrisSize = forwardUris ? forwardUris.size : 0;
        const claimUrisSize = claimUris ? claimUris.size : 0;

        return {
          ownNeedUri: ownNeed && ownNeed.get("uri"),
          message,
          chatMessages: chatMessages,
          connection,
          acceptUrisSize,
          proposeUrisSize,
          proposeToCancelUrisSize,
          rejectUrisSize,
          retractUrisSize,
          forwardUrisSize,
          claimUrisSize,
          expandedReferences,
          hasProposeUris: proposeUrisSize > 0,
          hasAcceptUris: acceptUrisSize > 0,
          hasProposeToCancelUris: proposeToCancelUrisSize > 0,
          hasRetractUris: retractUrisSize > 0,
          hasRejectUris: rejectUrisSize > 0,
          hasForwardUris: forwardUrisSize > 0,
          hasClaimUris: claimUrisSize > 0,
          proposeUrisArray: proposeUris && Array.from(proposeUris.toSet()),
          retractUrisArray: retractUris && Array.from(retractUris.toSet()),
          rejectUrisArray: rejectUris && Array.from(rejectUris.toSet()),
          forwardUrisArray: forwardUris && Array.from(forwardUris.toSet()),
          proposeToCancelUrisArray:
            proposeToCancelUris && Array.from(proposeToCancelUris.toSet()),
          acceptUrisArray: acceptUris && Array.from(acceptUris.toSet()),
          claimUrisArray: claimUris && Array.from(claimUris.toSet()),
          hasContent: message && message.get("hasContent"),
          hasNotBeenLoaded: !message,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );

      classOnComponentRoot(
        "won-has-non-ref-content",
        () => this.hasContent || this.hasNotBeenLoaded,
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

    toggleReferenceExpansion(reference) {
      const currentExpansionState =
        this.expandedReferences && this.expandedReferences.get(reference);

      if (this.message && !this.multiSelectType) {
        this.messages__viewState__markExpandReference({
          messageUri: this.messageUri,
          connectionUri: this.connectionUri,
          needUri: this.ownNeedUri,
          isExpanded: !currentExpansionState,
          reference: reference,
        });
      }
    }

    isReferenceExpanded(reference) {
      return this.expandedReferences && this.expandedReferences.get(reference);
    }

    getCountString(elements) {
      if (elements > 1 || elements == 0) {
        return elements + " Messages";
      } else {
        return elements + " Message";
      }
    }

    loadMessage(messageUri) {
      if (!this.getReferencedMessage(messageUri)) {
        this.addMessageToState(messageUri);
      }
    }

    addMessageToState(eventUri) {
      const ownNeedUri = this.ownNeedUri;
      return fetchMessage(ownNeedUri, eventUri).then(response => {
        won.wonMessageFromJsonLd(response).then(msg => {
          if (msg.isFromOwner() && msg.getReceiverNeed() === ownNeedUri) {
            /*if we find out that the receiverneed of the crawled event is actually our
             need we will call the method again but this time with the correct eventUri
             */
            this.addMessageToState(msg.getRemoteMessageUri());
          } else {
            //If message isnt in the state we add it
            this.messages__processAgreementMessage(msg);
          }
        });
      });
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
  .module("won.owner.components.referencedMessageContent", [])
  .directive("wonReferencedMessageContent", genComponentConf).name;
