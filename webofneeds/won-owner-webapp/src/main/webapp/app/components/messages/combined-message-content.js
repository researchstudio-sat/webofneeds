import angular from "angular";

import won from "../../won-es6.js";
import { connect2Redux } from "../../won-utils.js";
import { attach, get, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { selectNeedByConnectionUri } from "../../selectors.js";
import trigModule from "../trig.js";
import { labels } from "../../won-label-utils.js";

import "style/_combined-message-content.scss";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="msg__header" ng-if="self.message && !self.isConnectionMessage()">
          <div class="msg__header__type" ng-if="!self.isOtherMessage()">{{ self.labels.messageType[self.messageType] }}</div>
          <div class="msg__header__type" ng-if="self.isOtherMessage()">{{ self.messageType }}</div>
      </div>
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
      <won-trig
          trig="self.contentGraphTrig"
          ng-if="self.shouldShowRdf && self.contentGraphTrig">
      </won-trig>
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
          connection &&
          this.messageUri &&
          getIn(connection, ["messages", this.messageUri]);

        return {
          contentGraphTrig: get(message, "contentGraphTrigRaw"),
          shouldShowRdf: state.get("showRdf"),
          connection,
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

    isConnectionMessage() {
      return (
        this.message &&
        this.message.get("messageType") === won.WONMSG.connectionMessage
      );
    }

    isOtherMessage() {
      return !(
        this.isHintMessage() ||
        this.isHintFeedbackMessage() ||
        this.isOpenMessage() ||
        this.isConnectMessage() ||
        this.isConnectionMessage()
      );
    }

    isHintMessage() {
      return this.messageType === won.WONMSG.hintMessage;
    }

    isHintFeedbackMessage() {
      return this.messageType === won.WONMSG.hintFeedbackMessage;
    }

    isConnectMessage() {
      return this.messageType === won.WONMSG.connectMessage;
    }

    isOpenMessage() {
      return this.messageType === won.WONMSG.openMessage;
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
  .module("won.owner.components.combinedMessageContent", [trigModule])
  .directive("wonCombinedMessageContent", genComponentConf).name;
