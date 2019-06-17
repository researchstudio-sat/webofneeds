/**
 * Component for rendering the connection state as an svg
 * Created by fsuda on 10.04.2017.
 */
import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import { labels } from "../won-label-utils.js";
import { actionCreators } from "../actions/actions.js";

import { attach } from "../utils.js";
import { getOwnedAtomByConnectionUri } from "../redux/selectors/general-selectors.js";
import { connect2Redux } from "../won-utils.js";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
        <div class="cs__state" title="{{self.labels.connectionState[self.state]}}">
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color);" ng-if="self.unread && self.state === self.WON.Suggested">
                 <use xlink:href="#ico36_match" href="#ico36_match"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color);" ng-if="self.unread && self.state === self.WON.RequestSent">
                 <use xlink:href="#ico36_outgoing" href="#ico36_outgoing"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color);" ng-if="self.unread && self.state === self.WON.RequestReceived">
                 <use xlink:href="#ico36_incoming" href="#ico36_incoming"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color);" ng-if="self.unread && self.state === self.WON.Connected">
                 <use xlink:href="#ico36_message" href="#ico36_message"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color);" ng-if="self.unread && self.state === self.WON.Closed">
                 <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color-light);" ng-if="!self.unread && self.state === self.WON.Suggested">
                 <use xlink:href="#ico36_match" href="#ico36_match"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color-light);" ng-if="!self.unread && self.state === self.WON.RequestSent">
                 <use xlink:href="#ico36_outgoing" href="#ico36_outgoing"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color-light);" ng-if="!self.unread && self.state === self.WON.RequestReceived">
                 <use xlink:href="#ico36_incoming" href="#ico36_incoming"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color-light);" ng-if="!self.unread && self.state === self.WON.Connected">
                 <use xlink:href="#ico36_message" href="#ico36_message"></use>
            </svg>
            <svg class="cs__state__icon" style="--local-primary:var(--won-primary-color-light);" ng-if="!self.unread && self.state === self.WON.Closed">
                 <use xlink:href="#ico36_close_circle" href="#ico36_close_circle"></use>
            </svg>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;

      const selectFromState = state => {
        const atom = getOwnedAtomByConnectionUri(state, this.connectionUri);
        const connection =
          atom && atom.getIn(["connections", this.connectionUri]);

        return {
          state: connection && connection.get("state"),
          unread: connection && connection.get("unread"),
          WON: won.WON,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
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
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionState", [])
  .directive("wonConnectionState", genComponentConf).name;
