import angular from "angular";

import Immutable from "immutable";
import { relativeTime } from "../../won-label-utils.js";
import { connect2Redux } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../selectors/general-selectors.js";

import "~/style/_connection-message-status.scss";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="msgstatus__icons"
          ng-if="self.message.get('outgoingMessage')">
          <svg class="msgstatus__icons__icon" ng-if="!self.message.get('failedToSend')" ng-class="{'received' : self.message.get('isReceivedByOwn')}">
              <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
          </svg>
          <svg class="msgstatus__icons__icon" ng-if="!self.message.get('failedToSend')" ng-class="{'received' : self.message.get('isReceivedByRemote')}">
              <use xlink:href="#ico36_added_circle" href="#ico36_added_circle"></use>
          </svg>
          <svg class="msgstatus__icons__icon" ng-if="self.message.get('failedToSend')" style="--local-primary: red;">
              <use xlink:href="#ico16_indicator_warning" href="#ico16_indicator_warning"></use>
          </svg>
      </div>
      <div class="msgstatus__time" ng-show="!self.message.get('outgoingMessage') || (!self.message.get('failedToSend') && (self.message.get('isReceivedByRemote') && self.message.get('isReceivedByOwn')))">
          {{ self.relativeTime(self.lastUpdateTime, self.message.get('date')) }}
      </div>
      <div class="msgstatus__time--pending" ng-show="self.message.get('outgoingMessage') && !self.message.get('failedToSend') && (!self.message.get('isReceivedByRemote') || !self.message.get('isReceivedByOwn'))">
          Sending&nbsp;&hellip;
      </div>
      <div class="msgstatus__time--failure" ng-show="self.message.get('outgoingMessage') && self.message.get('failedToSend')">
          Sending failed
      </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);
      this.relativeTime = relativeTime;

      const selectFromState = state => {
        const ownedAtom =
          this.connectionUri &&
          getOwnedAtomByConnectionUri(state, this.connectionUri);
        const connection =
          ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);
        const message =
          connection && this.messageUri
            ? getIn(connection, ["messages", this.messageUri])
            : Immutable.Map();

        return {
          connection,
          message,
          lastUpdateTime: state.get("lastUpdateTime"),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
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
  .module("won.owner.components.connectionMessageStatus", [])
  .directive("wonConnectionMessageStatus", genComponentConf).name;
