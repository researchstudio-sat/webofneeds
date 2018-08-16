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
      <div class="msg__header" ng-if="!self.isConnectionMessage">
          <div class="msg__header__type">{{ self.getHeaderLabel() }}</div>
      </div>
      <won-message-content
          ng-if="self.hasContent || self.hasNotBeenLoadedYet"
          message-uri="self.messageUri"
          connection-uri="self.connectionUri">
      </won-message-content>
      <won-referenced-message-content
          ng-if="self.hasReferences"
          message-uri="self.messageUri"
          connection-uri="self.connectionUri">
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
        const ownNeed =
          this.connectionUri &&
          selectNeedByConnectionUri(state, this.connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
        const message =
          connection &&
          this.messageUri &&
          getIn(connection, ["messages", this.messageUri]);

        const messageType = message && message.get("messageType");

        return {
          contentGraphTrig: get(message, "contentGraphTrigRaw"),
          shouldShowRdf: state.get("showRdf"),
          hasContent: message && message.get("hasContent"),
          hasNotBeenLoadedYet: !message,
          hasReferences: message && message.get("hasReferences"),
          messageType,
          isConnectionMessage: messageType === won.WONMSG.connectionMessage,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );
    }

    getHeaderLabel() {
      const headerLabel = labels.messageType[this.messageType];
      return headerLabel || this.messageType;
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
