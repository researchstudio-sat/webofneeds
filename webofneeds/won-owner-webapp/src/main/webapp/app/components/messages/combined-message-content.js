import angular from "angular";

import won from "../../won-es6.js";
import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { selectNeedByConnectionUri } from "../../selectors.js";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
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

        return {
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
  .module("won.owner.components.combinedMessageContent", [])
  .directive("wonCombinedMessageContent", genComponentConf).name;
