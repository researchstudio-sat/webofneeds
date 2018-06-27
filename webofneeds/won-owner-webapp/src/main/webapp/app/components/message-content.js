import angular from "angular";

import won from "../won-es6.js";
import Immutable from "immutable";
import { connect2Redux } from "../won-utils.js";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { selectNeedByConnectionUri } from "../selectors.js";
import { labels } from "../won-label-utils.js";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="msgcontent__type" ng-if="self.isConnectMessage() || self.isOpenMessage()"> {{ self.labels.messageType[self.message.get('messageType')] }}</div>
      <div class="msgcontent__type" ng-if="self.isOtherMessage()"> {{ self.message.get('messageType') }}</div>
      <div class="msgcontent__text--prewrap" ng-if="self.message.get('text')">{{ self.message.get('text') }}</div> <!-- no spaces or newlines within the code-tag, because it is preformatted -->
      <div class="msgcontent__text" ng-if="!self.isConnectMessage() && !self.isOpenMessage() && !self.message.get('text')">{{ self.noParsableContentPlaceholder }}</div>
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
      return this.message.get("messageType") === won.WONMSG.connectionMessage;
    }

    isConnectMessage() {
      return this.message.get("messageType") === won.WONMSG.connectMessage;
    }

    isOpenMessage() {
      return this.message.get("messageType") === won.WONMSG.openMessage;
    }

    isOtherMessage() {
      return !(
        this.isOpenMessage() ||
        this.isConnectMessage() ||
        this.isConnectionMessage()
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
  .module("won.owner.components.messageContent", [])
  .directive("wonMessageContent", genComponentConf).name;
