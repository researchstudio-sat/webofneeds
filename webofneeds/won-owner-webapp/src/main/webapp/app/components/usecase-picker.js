/**
 * Created by quasarchimaere on 03.07.2018.
 */
import angular from "angular";
import ngAnimate from "angular-animate";

import "ng-redux";
import { attach } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect2Redux } from "../won-utils.js";
import { selectIsConnected } from "../selectors.js";

const serviceDependencies = [
  "$ngRedux",
  "$scope",
  "$element" /*'$routeParams' /*injections as strings here*/,
];

function genComponentConf() {
  const template = `
        <div class="ucp__header">
            <a class="ucp__header__back clickable show-in-responsive"
                ng-click="self.router__stateGoCurrent({showCreateView: undefined})">
                <svg style="--local-primary:var(--won-primary-color);"
                    class="ucp__header__back__icon">
                    <use xlink:href="#ico36_backarrow" href="#ico36_backarrow"></use>
                </svg>
            </a>
            <span class="ucp__header__title">Pick Usecase</span>
        </div>
        <div class="ucp__content">
          PICK YOUR USECASE
        </div>
    `;

  class Controller {
    constructor(/* arguments <- serviceDependencies */) {
      attach(this, serviceDependencies, arguments);

      window.ucp4dbg = this;

      const selectFromState = state => {
        return {
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
  .module("won.owner.components.usecasePicker", [ngAnimate])
  .directive("wonUsecasePicker", genComponentConf).name;
