/**
 * Created by quasarchimaere on 24.05.2018.
 */

import angular from "angular";
import { actionCreators } from "../actions/actions.js";
import { attach } from "../utils.js";
import { connect2Redux } from "../won-utils.js";

import "style/_modal-dialog.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
      <div class="md__dialog">
          <div class="md__dialog__header">
             <span class="md__dialog__header__caption">{{self.modalDialogCaption}}</span>
          </div>
          <div class="md__dialog__content">
             <span class="md__dialog__content__text">{{self.modalDialogText}}</span>
          </div>
          <div class="md__dialog__footer">
             <button
                ng-repeat="button in self.modalDialogButtons"
                class="won-button--filled lighterblue"
                ng-click="button.get('callback')()">
                    <span>{{button.get("caption")}}</span>
             </button>
          </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const modalDialog = state.getIn(["view", "modalDialog"]);
        const modalDialogCaption = modalDialog && modalDialog.get("caption");
        const modalDialogText = modalDialog && modalDialog.get("text");
        const modalDialogButtons = modalDialog && modalDialog.get("buttons");

        return {
          modalDialogCaption,
          modalDialogText,
          modalDialogButtons:
            modalDialogButtons && modalDialogButtons.toArray(),
        };
      };

      connect2Redux(selectFromState, actionCreators, [], this);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    // //scope: { }, // not isolated on purpose to allow using parent's scope
    scope: {},
    template: template,
  };
}

export default angular
  .module("won.owner.components.modalDialog", [])
  .directive("wonModalDialog", genComponentConf).name;
