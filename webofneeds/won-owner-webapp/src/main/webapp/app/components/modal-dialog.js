/**
 * Created by quasarchimaere on 24.05.2018.
 */

import angular from "angular";
import { actionCreators } from "../actions/actions.js";
import { attach } from "../cstm-ng-utils.js";
import { connect2Redux } from "../configRedux.js";
import { absHRef } from "../configRouting.js";

import "~/style/_modal-dialog.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element", "$state"];
function genComponentConf() {
  let template = `
      <div class="md__dialog">
          <div class="md__dialog__header">
             <span class="md__dialog__header__caption" ng-if="!self.showTerms">{{self.modalDialogCaption}}</span>
             <span class="md__dialog__header__caption" ng-if="self.showTerms">Important Note</span>
          </div>
          <div class="md__dialog__content">
             <span class="md__dialog__content__text" ng-if="!self.showTerms">{{self.modalDialogText}}</span>
             <span class="md__dialog__content__text" ng-if="self.showTerms">
                This action requires an account. If you want to proceed, we will create an anonymous account for you.
                <br/>
                <br/>
                By clicking 'Yes', you accept the <a target="_blank" rel="noopener noreferrer" href="{{ self.absHRef(self.$state, 'about', {'aboutSection': 'aboutTermsOfService'}) }}">Terms Of Service(ToS)</a> and anonymous account will be created. Clicking 'No' will just cancel the action.
              </span>
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
      this.absHRef = absHRef;

      const selectFromState = state => {
        const modalDialog = state.getIn(["view", "modalDialog"]);
        const modalDialogCaption = modalDialog && modalDialog.get("caption");
        const modalDialogText = modalDialog && modalDialog.get("text");
        const modalDialogButtons = modalDialog && modalDialog.get("buttons");

        const showTerms = modalDialog && modalDialog.get("showTerms");
        return {
          modalDialogCaption,
          modalDialogText,
          showTerms,
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
