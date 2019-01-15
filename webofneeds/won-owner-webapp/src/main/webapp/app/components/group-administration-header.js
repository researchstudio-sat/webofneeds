/**
 * Created by quasarchimaere on 08.01.2019.
 */
import angular from "angular";
import "ng-redux";
import groupImageModule from "./group-image.js";
import { actionCreators } from "../actions/actions.js";
import { attach, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";

import "style/_group-administration-header.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
function genComponentConf() {
  let template = `
      <won-group-image
        class="ch__icon"
        need-uri="self.needUri">
      </won-group-image>
      <div class="ch__right">
        <div class="ch__right__topline">
          <div class="ch__right__topline__title">
            Group Chat Administration
          </div>
        </div>
        <div class="ch__right__subtitle">
          <span class="ch__right__subtitle__type">
            TODO: Add number of (current) participants
          </span>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const groupChatNeed = getIn(state, ["needs", this.needUri]);

        return {
          groupChatNeed,
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      needUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupAdministrationHeader", [groupImageModule])
  .directive("wonGroupAdministrationHeader", genComponentConf).name;
