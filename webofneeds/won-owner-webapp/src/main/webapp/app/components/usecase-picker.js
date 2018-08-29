/**
 * Created by quasarchimaere on 03.07.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import { attach, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";
import usecasePickerContentModule from "./usecase-picker-content.js";
import usecaseGroupModule from "./usecase-group.js";

import "style/_usecase-picker.scss";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <div class="ucp__header">
            <a class="ucp__header__back clickable"
                ng-click="self.router__stateGoCurrent({showUseCases: undefined, useCase: undefined, useCaseGroup: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ucp__header__back__icon">
                    <use xlink:href="#ico36_close" href="#ico36_close"></use>
                </svg>
            </a>
            <span class="ucp__header__title">What do you have or want?</span>
        </div>
        <won-usecase-picker-content ng-if="!self.useCaseGroup">
        </won-usecase-picker-content>
        <won-usecase-group ng-if="!!self.useCaseGroup">
        </won-usecase-group>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);
      window.ucp4dbg = this;

      const selectFromState = state => {
        const useCaseGroup = getIn(state, [
          "router",
          "currentParams",
          "useCaseGroup",
        ]);

        return {
          useCaseGroup,
          connectionHasBeenLost: !selectIsConnected(state),
        };
      };

      // Using actionCreators like this means that every action defined there is available in the template.
      connect2Redux(selectFromState, actionCreators, [], this);
    }
  }

  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      /*scope-isolation*/
    },
    template: template,
  };
}

export default //.controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
angular
  .module("won.owner.components.usecasePicker", [
    ngAnimate,
    usecasePickerContentModule,
    usecaseGroupModule,
  ])
  .directive("wonUsecasePicker", genComponentConf).name;
